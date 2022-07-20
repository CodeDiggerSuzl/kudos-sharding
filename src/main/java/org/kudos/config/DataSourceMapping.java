package org.kudos.config;

import lombok.Data;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Mapping sharding  config from yaml.
 *
 * @author suzl
 */
@Data
public class DataSourceMapping {

    /**
     * default data source mapping
     */
    private String defaultDataSourceName;

    /**
     * key: data source name, value: the datasource
     */
    private Map<String, DataSource> dataSourceMap;


    private Map<String, TableShardingConfig> tableShardingConfigMap;

    /**
     * key: data source number, value: data source name
     */
    private Map<String, String> dbNumberMapping;
}
