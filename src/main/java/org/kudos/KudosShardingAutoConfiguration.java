package org.kudos;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.plugin.Interceptor;
import org.kudos.annotation.KudosSharding;
import org.kudos.config.DataBaseConfig;
import org.kudos.config.DataSourceGroupConfig;
import org.kudos.config.DataSourceMapping;
import org.kudos.config.KudosShardingConfigProperty;
import org.kudos.config.TableShardingConfig;
import org.kudos.intercepter.ShardingInterceptor;
import org.kudos.utils.JsonUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * kudos sharding auto configuration.
 *
 * @author suzl
 */
@Configuration
@Slf4j
@ConditionalOnClass({DruidDataSource.class, KudosSharding.class})
public class KudosShardingAutoConfiguration {

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

        final Set<Map.Entry<String, DataSourceGroupConfig>> entries = dataSourceGroupMap.entrySet();

        Map<String, DataSourceMapping> dsgMappingMap = new HashMap<>(entries.size());

        for (Map.Entry<String, DataSourceGroupConfig> entry : entries) {
            final String key = entry.getKey();
            final DataSourceGroupConfig datasourceGroup = entry.getValue();

            if (datasourceGroup == null) {
                throw new IllegalArgumentException("data source group config of [" + key + "] is null, please check your config");
            }

            final String defaultDsName = datasourceGroup.getDefaultDataSourceName();
            if (StringUtils.isEmpty(defaultDsName)) {
                throw new IllegalArgumentException("default data source shardingStrategyName of [" + key + "] is empty, please check your config");
            }

            final Map<String, DataBaseConfig> dataBaseConfigMap = datasourceGroup.getDataBaseConfigMap();
            if (dataBaseConfigMap == null || dataBaseConfigMap.isEmpty()) {
                throw new IllegalArgumentException("data base config map of [" + key + "] is empty, please check your config");
            }

            final DataBaseConfig defaultDatasource = dataBaseConfigMap.get(defaultDsName);
            if (defaultDatasource == null) {
                throw new IllegalArgumentException("default data source of [" + key + "] is not found, please check your config");
            }

            final Map<String, TableShardingConfig> tableShardingConfigMap = datasourceGroup.getTableShardingConfigMap();
            if (tableShardingConfigMap == null || tableShardingConfigMap.isEmpty()) {
                throw new IllegalArgumentException("table sharding config map of [" + key + "] is empty, please check your config");
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
            dsgMappingMap.put(key, dsMapping);
        }
        return new KudosDynamicDataSource(dsgMappingMap);
    }


    @Bean
    @ConditionalOnMissingBean(name = "kudosShardingInterceptor")
    public Interceptor kudosShardingInterceptor(KudosShardingConfigProperty property) {
        log.info("initializing kudosShardingInterceptor through kudos sharding config = {}", JsonUtils.toStr(property));
        final Map<String, DataSourceGroupConfig> dataSourceGroupMap = property.getDataSourceGroupMap();

        Map<String, Map<String, String>> dbNoMapping = new HashMap<>(dataSourceGroupMap.size());
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

            final Map<String, String> dbMapping = group.getDbMapping();
            if (dbMapping == null || dbMapping.isEmpty()) {
                throw new IllegalArgumentException("db mapping of [" + groupKey + "] is empty, please check your config");
            }
            dbMapping.forEach((dbNo, dbName) -> {
                if (StringUtils.isEmpty(dbNo) || StringUtils.isEmpty(dbName)) {
                    throw new IllegalArgumentException("db mapping of [" + groupKey + "] is empty, please check your config");
                }
                // if dbNo is not an umber, then throw exception
                if (!org.apache.commons.lang3.StringUtils.isNumeric(dbNo)) {
                    throw new IllegalArgumentException("db mapping of [" + groupKey + "], [" + dbNo + "] is not a number, please check your config");
                }
                if (dbNo.length() > 4) {
                    throw new IllegalArgumentException("db mapping of [" + groupKey + "], [" + dbNo + "] is too long, you might don't need " + dbNo + " databases. check your config");
                }
                if (dbNo.length() < 4) {
                    dbMapping.put(new DecimalFormat("0000").format(new BigDecimal(dbNo)), dbName);
                    dbMapping.remove(dbNo);
                }
            });
            dbNoMapping.put(groupKey, dbMapping);
            tableShardingConfigMap.put(groupKey, shardingCfgMap);
        }
        return new ShardingInterceptor(tableShardingConfigMap, dbNoMapping);
    }

    public static void main(String[] args) {
        final String format = new DecimalFormat("0000").format(new BigDecimal("01"));
        System.out.println(format);

        final String format1 = new DecimalFormat("0000").format(new BigDecimal("0101"));
        System.out.println(format1);

        final String format2 = new DecimalFormat("0000").format("1011");
        System.out.println(format2);


    }

}
