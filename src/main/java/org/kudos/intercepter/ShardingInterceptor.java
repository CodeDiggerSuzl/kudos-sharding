package org.kudos.intercepter;

import com.github.benmanes.caffeine.cache.Cache;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.kudos.annotation.KudosSharding;
import org.kudos.annotation.NoSharding;
import org.kudos.config.RemoteConfigFetcher;
import org.kudos.config.TableShardingConfig;
import org.kudos.sharding.ShardingResult;
import org.kudos.sharding.ShardingStrategy;
import org.kudos.context.ShardingContextHolder;
import org.kudos.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
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
@Component
@Intercepts({
        // SQL replacement
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
        // sharding
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}), @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class ShardingInterceptor implements Interceptor {
    @Autowired
    private RemoteConfigFetcher remoteConfigFetcher;
    // table sharding config map
    // key: datasource group, value: table sharding config(with sharding strategy, datasource total, table total)
    private Map<String, Map<String, TableShardingConfig>> tableShardingConfigMap;
    private final ThreadLocal<Boolean> shardingFlagCtx = new ThreadLocal<>();
    private final ThreadLocal<Map<String, String>> tableNameMapCtx = new ThreadLocal<>();

    public ShardingInterceptor() {
    }

    public ShardingInterceptor(Map<String, Map<String, TableShardingConfig>> tableShardingConfigMap) {
        this.tableShardingConfigMap = tableShardingConfigMap;
    }


    private static final ReflectorFactory REFLECTOR_FACTORY = new DefaultReflectorFactory();

    // local cache: key as the dao shardingStrategyName, and annotation config
    @Autowired
    Cache<String, Object> caffeineCache;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        final Object target = invocation.getTarget();
        final Class<?> targetClz = target.getClass();
        final Object[] args = invocation.getArgs();
        final Method method = invocation.getMethod();

        // get annotation info.
        if (Executor.class.isAssignableFrom(targetClz)) {
            final MappedStatement mappedStatement = (MappedStatement) args[0];
            final String fullMethodName = mappedStatement.getId(); // org.kudos.mapper.UserInfoDao.selectAll
            final int idx = fullMethodName.lastIndexOf(".");
            String interfaceName = fullMethodName.substring(0, idx);
            // local cache: key:interface shardingStrategyName value: sharding annotation config
            final Class<?> daoClazz = ClassUtils.forName(interfaceName, this.getClass().getClassLoader());
            // TODO add cache to improve the performance
            final NoSharding nosharding = AnnotationUtils.findAnnotation(daoClazz, NoSharding.class);
            // once marked as no sharding, this mapper will not be sharded, even if it has sharding annotation
            if (nosharding != null) {
                shardingFlagCtx.set(false);
                // don't shard
                return invocation.proceed();
            }
            final KudosSharding shardingCfg = AnnotationUtils.findAnnotation(daoClazz, KudosSharding.class);
            // // not marked with annotation
            if (shardingCfg == null) {
                shardingFlagCtx.set(false);
                return invocation.proceed();
            }
            // here is core sharding logic
            final String shardingKey = shardingCfg.shardingKey();
            //
            final Class<? extends ShardingStrategy> strategy = shardingCfg.shardingStrategy(); // add cache TODO
            final String[] tableNameArr = shardingCfg.tableName();
            final String tableName = tableNameArr[0];
            final ShardingStrategy shardingStrategy = strategy.newInstance();
            final Object arg = args[1];
            if (arg instanceof MapperMethod.ParamMap) {
                // TODO here
                final Object shardingKeyValue = ((MapperMethod.ParamMap<?>) arg).get(shardingKey);
                final ShardingResult shardingResult = shardingStrategy.sharding(shardingKeyValue, 3, 3);
                System.out.println("shardingKey:" + shardingKey + "value = " + shardingKeyValue + " shardingResult = " + JsonUtils.toStr(shardingResult));
                ShardingContextHolder.DB_CTX.set(shardingResult);

                Map<String, String> tableNameMapping = new HashMap<>();
                tableNameMapping.put(tableName, tableName + "_" + shardingResult.getTableNo());
                tableNameMapCtx.set(tableNameMapping);
            }


            // get sharding config
            // 1. get sharding strategy
            // 2. get sharding key
            // 3. get sharding result by sharding strategy and sharding key
            // 4. set into the thread local context
            // 5. determine which datasource to use
            // 6. sql replace
            // 7. free the thread local


        }

        // sql replacement
        if (StatementHandler.class.isAssignableFrom(targetClz)) {
            try {
                final Thread thread = Thread.currentThread();
                if (Boolean.FALSE.equals(shardingFlagCtx.get())) {
                    // do nothing
                    return invocation.proceed();
                }
                final Map<String, String> tableMapping = tableNameMapCtx.get();
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
                System.out.println("replaced sql: " + replacedSql);

            } finally {
                shardingFlagCtx.remove();
                ShardingContextHolder.DB_CTX.remove();
                tableNameMapCtx.remove();
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
