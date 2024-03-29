package org.kudos.intercepter;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.kudos.annotation.KudosSharding;
import org.kudos.annotation.NoSharding;
import org.kudos.annotation.ShardingConfig;
import org.kudos.config.RemoteConfigFetcher;
import org.kudos.config.TableShardingConfig;
import org.kudos.context.ShardingContextHolder;
import org.kudos.sharding.ShardingResult;
import org.kudos.sharding.strategy.ShardingStrategy;
import org.kudos.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * sharding interceptor.
 *
 * @author suzl
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Intercepts({
        // SQL replacement
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
        // sharding
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class ShardingInterceptor implements Interceptor {

    @Autowired
    @Qualifier("kudosRemoteConfigFetcher")
    private RemoteConfigFetcher remoteConfigFetcher;

    private static final ReflectorFactory REFLECTOR_FACTORY = new DefaultReflectorFactory();
    /**
     * table sharding config map key: datasource group, value: table sharding config(with sharding strategy, datasource
     * total, table total)
     */
    private Map<String, Map<String, TableShardingConfig>> tableShardingConfigMap;
    /**
     * db number and name mapping, cause the sharing result is a number, need the association of no and name mapping
     */
    private Map<String, Map<String, String>> dbNoMapping;

    public ShardingInterceptor() {
    }

    public ShardingInterceptor(final Map<String, Map<String, TableShardingConfig>> tableShardingConfigMap) {
        this.tableShardingConfigMap = tableShardingConfigMap;
    }

    // local cache: key as the dao getStrategyName, and annotation config
    @Autowired
    @Qualifier("kudosCaffeineCache")
    private Cache<String, Object> caffeineCache;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        //        Object s = caffeineCache.getIfPresent("s");
        //        log.info("get from local cache: {}", s);
        final Object target = invocation.getTarget();
        final Class<?> targetClz = target.getClass();
        final Object[] args = invocation.getArgs();
        final Method method = invocation.getMethod();

        // get annotation info.
        if (Executor.class.isAssignableFrom(targetClz)) {
            final MappedStatement mappedStatement = (MappedStatement) args[0];
            final String fullMethodName = mappedStatement.getId();
            final int idx = fullMethodName.lastIndexOf(".");
            String interfaceName = fullMethodName.substring(0, idx);
            // local cache: key:interface getStrategyName value: sharding annotation config
            final Class<?> daoClazz = ClassUtils.forName(interfaceName, this.getClass().getClassLoader());
            // TODO add cache to improve the performance
            final NoSharding nosharding = AnnotationUtils.findAnnotation(daoClazz, NoSharding.class);
            // once marked as no sharding, this mapper will not be sharded, even if it has sharding annotation
            if (nosharding != null) {
                ShardingContextHolder.setShardingFlag(false);
                // don't shard
                return invocation.proceed();
            }

            final KudosSharding shardingConf = AnnotationUtils.findAnnotation(daoClazz, KudosSharding.class);
            // // not marked with annotation
            if (shardingConf == null) {
                ShardingContextHolder.setShardingFlag(false);
                return invocation.proceed();
            }
            log.info("[kudos-sharding]:sharding annotation config: {}", JsonUtils.toStr(shardingConf.toString()));

            // here is core sharding logic
            final String shardingKey = shardingConf.shardingKey();
            log.debug("[kudos-sharding]:[kudos-sharding]: got sharding key sharding key: {}", shardingKey);
            // get value of sharding key from args
            final Object arg = args[1];
            final Object shardingValue = KudosShardingHelper.getShardingKeyValue(shardingKey, arg, mappedStatement);
            log.info("[kudos-sharding]:sharding key: {}, sharding value: {}", shardingKey, shardingValue);
            // sharding strategy for main table
            final Class<? extends ShardingStrategy> strategy = shardingConf.shardingStrategy(); // add cache TODO

            final String mainTableName = shardingConf.mainTableName();
            final ShardingConfig[] otherTableArr = shardingConf.otherTableShardingConfig();

            final String dbGroupKey = remoteConfigFetcher.fetchCurrDatasourceGroupKey();
            // final String dbGroupKey = "default";
            final Map<String, TableShardingConfig> tableShardingConfigMap = this.tableShardingConfigMap.get(dbGroupKey);
            log.info("[kudos-sharding]:table sharding config map: {}", JsonUtils.toStr(tableShardingConfigMap));
            final String connector = shardingConf.tableConnector();

            // multiple table sharding
            if (otherTableArr.length > 0) {
                // pass
            } else {
                // only one table sharding
                TableShardingConfig mainTableConfig = tableShardingConfigMap.get(mainTableName);
                if (mainTableConfig == null) {
                    ShardingContextHolder.clearAllContexts();
                    throw new RuntimeException("could not find table sharding config for table: [" + mainTableName + "]in data source group: [" + dbGroupKey + "].please check your config");
                }
                final ShardingStrategy shardingStrategy = strategy.newInstance();
                ShardingResult shardingResult = shardingStrategy.sharding(shardingValue, mainTableConfig.getDbCnt(), mainTableConfig.getTableCnt());
                log.info("[kudos-sharding]:sharding value : {} sharding result: {}", shardingValue, JsonUtils.toStr(shardingResult));
                // set sharding result to context holder
                ShardingContextHolder.setShardingResult(shardingResult);
                final String tableNo = shardingResult.getTableNo();
                final String tableNameWithNo = String.format("%s%s%s", mainTableName, connector, tableNo);
                Map<String, String> tableNameMap = new HashMap<>(1);
                tableNameMap.put(mainTableName, tableNameWithNo);
                ShardingContextHolder.setTableNameMap(tableNameMap);
            }
        }

        // sql replacement
        if (StatementHandler.class.isAssignableFrom(targetClz)) {
            try {
                final Boolean shardingFlag = ShardingContextHolder.getShardingFlag();
                log.info("[kudos-sharding]:sharding flag: {}", shardingFlag);
                if (Boolean.FALSE.equals(shardingFlag)) {
                    log.info("[kudos-sharding]:not sharding, skip sql replacement");
                    // do nothing
                    return invocation.proceed();
                }
                final Map<String, String> tableMapping = ShardingContextHolder.getTableNameMap();
                log.debug("[kudos-sharding]:table mapping: {}", JsonUtils.toStr(tableMapping));
                // get  sql
                final MetaObject metaObject = MetaObject.forObject(target, SystemMetaObject.DEFAULT_OBJECT_FACTORY, SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY, REFLECTOR_FACTORY);
                final String sqlPropName = "delegate.boundSql.sql";
                final String originalSql = (String) metaObject.getValue(sqlPropName);
                String replacedSql = originalSql;
                for (Map.Entry<String, String> entry : tableMapping.entrySet()) {
                    final String originalTableName = entry.getKey();
                    final String afterTableName = entry.getValue();
                    // really tricky here
                    String regex = "\\b" + originalTableName + "\\b";
                    replacedSql = originalSql.replaceAll(regex, afterTableName);
                }
                metaObject.setValue(sqlPropName, replacedSql);
            } finally {
                ShardingContextHolder.clearAllContexts();
            }
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}

