package org.kudos;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.kudos.annotation.KudosSharding;
import org.kudos.config.DataBaseConfig;
import org.kudos.config.DataSourceGroupConfig;
import org.kudos.config.DataSourceMapping;
import org.kudos.config.KudosShardingConfigProperty;
import org.kudos.config.TableShardingConfig;
import org.kudos.utils.JsonUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.HashMap;
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
    @Primary
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


}
