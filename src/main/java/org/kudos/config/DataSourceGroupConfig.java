package org.kudos.config;

import lombok.Data;

import java.util.Map;

/**
 * data source group config.
 *
 * @author suzl
 */
@Data
public class DataSourceGroupConfig {

    /**
     * k: table_name, v: sharding config of this table
     */
    private Map<String, TableShardingConfig> tableShardingConfigMap;
    /**
     * key: db number, value: db name
     */
    private Map<String, String> dbMapping;

    /**
     * key db name, value: db config
     */
    private Map<String, DataBaseConfig> dataBaseConfigMap;

    private String defaultDataSourceName;

}

