package org.kudos.config;

import lombok.Data;

import java.util.Map;

/**
 * Mapping sharding  config from yaml.
 *
 * <p>
 * includes:
 * </p>
 * <ol>
 *     <li>
 *         data group map
 *     </li>
 *     <li>
 *         datasource map
 *     </li>
 *     <li> default data group</li>
 *     <li>
 *         default datasource
 *     </li>
 * </ol>
 *
 * @author suzl
 */
@Data
public class KudosShardingConfigProperty {
    /**
     * data source group config.
     * <p>
     * k: shardingStrategyName of the data source group
     * <p>
     * v: data sources of this group;
     */
    private Map<String, DataSourceGroupConfig> dataSourceGroupMap;
    /**
     * default sharding group
     */
    private String defaultDataSourceGroup;
}
