package org.kudos.config;

import lombok.Data;

import javax.sql.DataSource;
import java.util.Map;

/**
 * @author suzl
 */
@Data
public class DataSourceMapping {

    private String defaultDataSourceName;

    private Map<String, DataSource> dataSourceMap;

    private Map<String, TableShardingConfig> tableShardingConfigMap;
}
