package org.kudos;

import com.alibaba.druid.pool.DruidDataSource;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.plugin.Interceptor;
import org.kudos.annotation.KudosSharding;
import org.kudos.config.*;
import org.kudos.intercepter.ShardingInterceptor;
import org.kudos.utils.JsonUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * kudos sharding auto configuration.
 *
 * @author suzl
 */
@Configuration
@Slf4j
@EnableConfigurationProperties
@ConditionalOnClass({DruidDataSource.class, KudosSharding.class})
public class KudosShardingAutoConfiguration {

    private static final int DATASOURCE_GROUP_NAME_SIZE = 4;

    @Bean(name = "kudosRemoteConfigFetcher")
    public RemoteConfigFetcher remoteConfigFetcher() {
        return new RemoteConfigFetcher();
    }

    @Bean(name = "kudosCaffeineCache")
    public Cache<String, Object> caffeineCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(3, TimeUnit.HOURS)
                .initialCapacity(64)
                .maximumSize(512)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.sharding")
    public KudosShardingConfigProperty kudosShardingConfig() {
        return new KudosShardingConfigProperty();
    }

    @Bean
    @Primary // mark as the primary datasource, so that spring will use this datasource.
    public DataSource kudosDynamicDataSource(KudosShardingConfigProperty property) {
        log.debug("initializing kudosDynamicDataSource through kudos sharding config = {}", JsonUtils.toStr(property));
        // check the config
        if (property == null) {
            throw new IllegalArgumentException("kudos sharding config is null, please check your config");
        }
        // default datasource
        final String defaultDsGroup = property.getDefaultDataSourceGroup();
        if (StringUtils.isEmpty(defaultDsGroup)) {
            throw new IllegalArgumentException("default data source group is empty, please check your config");
        }

        // check the datasource group map
        final Map<String, DataSourceGroupConfig> dataSourceGroupMap = property.getDataSourceGroupMap();
        if (dataSourceGroupMap == null || dataSourceGroupMap.isEmpty()) {
            throw new IllegalArgumentException("data source group map is empty, please check your config");
        }

        final DataSourceGroupConfig defaultDataSourceGroup = dataSourceGroupMap.get(defaultDsGroup);
        if (defaultDataSourceGroup == null) {
            throw new IllegalArgumentException("default data source group is not found, please check your config");
        }

        Map<String, DataSourceMapping> dsgMappingMap = new HashMap<>(16);

        for (Map.Entry<String, DataSourceGroupConfig> entry : dataSourceGroupMap.entrySet()) {
            final String groupKey = entry.getKey();
            final DataSourceGroupConfig datasourceGroup = entry.getValue();

            if (datasourceGroup == null) {
                throw new IllegalArgumentException("data source group config of [" + groupKey + "] is null, please check your config");
            }

            final String defaultDsName = datasourceGroup.getDefaultDataSourceName();
            if (StringUtils.isEmpty(defaultDsName)) {
                throw new IllegalArgumentException("default data source getStrategyName of [" + groupKey + "] is empty, please check your config");
            }

            final Map<String, DataBaseConfig> dataBaseConfigMap = datasourceGroup.getDataBaseConfigMap();
            if (dataBaseConfigMap == null || dataBaseConfigMap.isEmpty()) {
                throw new IllegalArgumentException("data base config map of [" + groupKey + "] is empty, please check your config");
            }

            final DataBaseConfig defaultDatasource = dataBaseConfigMap.get(defaultDsName);
            if (defaultDatasource == null) {
                throw new IllegalArgumentException("default data source of [" + groupKey + "] is not found, please check your config");
            }

            final Map<String, TableShardingConfig> tableShardingConfigMap = datasourceGroup.getTableShardingConfigMap();
            if (tableShardingConfigMap == null || tableShardingConfigMap.isEmpty()) {
                throw new IllegalArgumentException("table sharding config map of [" + groupKey + "] is empty, please check your config");
            }

            final DataSourceMapping dsMapping = new DataSourceMapping();
            dsMapping.setDefaultDataSourceName(defaultDsName);
            dsMapping.setTableShardingConfigMap(tableShardingConfigMap);
            Map<String, DataSource> dataSourceMap = new HashMap<>(dataBaseConfigMap.size());
            dataBaseConfigMap.forEach((k, v) -> {
                // TODO: add druid pool config
                DruidDataSource ds = new DruidDataSource();
                ds.setUsername(v.getUsername());
                ds.setPassword(v.getPassword());
                ds.setUrl(v.getUrl());
                ds.setDriverClassName(v.getDriverClassName());
                dataSourceMap.put(k, ds);
            });
            dsMapping.setDataSourceMap(dataSourceMap);

            final Map<String, String> dbMapping = datasourceGroup.getDbMapping();
            if (dbMapping == null || dbMapping.isEmpty()) {
                throw new IllegalArgumentException("db mapping of [" + groupKey + "] is empty, please check your config");
            }
            Map<String, String> mapCpy = new HashMap<>(dbMapping.size());
            dbMapping.forEach((dbNo, dbName) -> {
                if (StringUtils.isEmpty(dbNo) || StringUtils.isEmpty(dbName)) {
                    throw new IllegalArgumentException("db mapping of [" + groupKey + "] is empty, please check your config");
                }
                // if dbNo is not an umber, then throw exception
                if (!org.apache.commons.lang3.StringUtils.isNumeric(dbNo)) {
                    throw new IllegalArgumentException("db mapping of [" + groupKey + "], [" + dbNo + "] is not a number, please check your config");
                }
                if (dbNo.length() > DATASOURCE_GROUP_NAME_SIZE) {
                    throw new IllegalArgumentException("db mapping of [" + groupKey + "], [" + dbNo + "] is too long, you might don't need " + dbNo + " databases. check your config");
                }
                // TODO better solution handle this
                mapCpy.put(new DecimalFormat("0000").format(new BigDecimal(dbNo)), dbName);
            });
            dsMapping.setDbNumberMapping(mapCpy);
            dsgMappingMap.put(groupKey, dsMapping);
        }
        KudosDynamicDataSource dynamicDataSource = new KudosDynamicDataSource(dsgMappingMap, property.getDefaultDataSourceGroup());
        LazyConnectionDataSourceProxy lazyDsProxy = new LazyConnectionDataSourceProxy();
        lazyDsProxy.setTargetDataSource(dynamicDataSource);
        lazyDsProxy.setDefaultAutoCommit(false);
        lazyDsProxy.setDefaultTransactionIsolation(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        System.out.println("kudosDynamicDataSource bean method ends..");
        return dynamicDataSource;
    }


    @Bean
    @ConditionalOnMissingBean(name = "kudosShardingInterceptor")
    public Interceptor kudosShardingInterceptor(KudosShardingConfigProperty property) {
        log.info("initializing kudosShardingInterceptor through kudos sharding config = {}", JsonUtils.toStr(property));
        final Map<String, DataSourceGroupConfig> dataSourceGroupMap = property.getDataSourceGroupMap();

        Map<String, Map<String, TableShardingConfig>> tableShardingConfigMap = new HashMap<>(dataSourceGroupMap.size());

        for (Map.Entry<String, DataSourceGroupConfig> entry : dataSourceGroupMap.entrySet()) {
            final String groupKey = entry.getKey();
            final DataSourceGroupConfig group = entry.getValue();

            final Map<String, TableShardingConfig> shardingCfgMap = group.getTableShardingConfigMap();
            final Set<String> tableNameSet = new HashSet<>();
            // table config map check
            shardingCfgMap.forEach((tableName, shardingCfg) -> {
                if (StringUtils.isEmpty(tableName) || shardingCfg == null) {
                    throw new IllegalArgumentException("table sharding config of [" + groupKey + "] is empty, please check your config");
                }
                final String shardingType = shardingCfg.getShardingType();
                if (StringUtils.isEmpty(shardingType)) {
                    throw new IllegalArgumentException("sharding type of [" + tableName + "] in group [" + groupKey + " ] is empty, please check your config");
                }
                final Integer dbCnt = shardingCfg.getDbCnt();
                final Integer tableCnt = shardingCfg.getTableCnt();

                if (dbCnt == null || tableCnt == null || dbCnt < 1 || tableCnt < 1) {
                    throw new IllegalArgumentException("dbCnt or tableCnt of [" + tableName + "] in group [" + groupKey + " ] is empty, please check your config");
                }
                // one table only can be configured once
                if (Boolean.FALSE.equals(tableNameSet.add(tableName))) {
                    throw new IllegalArgumentException("table [" + tableName + "] in group [" + groupKey + " ] is configured more than once, please check your config");
                }
            });

            tableShardingConfigMap.put(groupKey, shardingCfgMap);
        }
        return new ShardingInterceptor(tableShardingConfigMap);
    }

}
