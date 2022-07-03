package org.kudos;

import org.kudos.config.DataSourceMapping;
import org.kudos.config.RemoteConfigFetcher;
import org.kudos.sharding.ShardingResult;
import org.kudos.context.ShardingContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * DynamicDataSource
 * <p>
 *
 * @author suzl
 * @see AbstractRoutingDataSource read the doc of this class to figure it out.
 */
@Component
public class KudosDynamicDataSource extends AbstractRoutingDataSource {

    private static final String SEPARATOR = ":";

    @Autowired
    private RemoteConfigFetcher remoteConfigFetcher;

    public KudosDynamicDataSource() {
    }

    private Map<String, DataSourceMapping> dataSourceGroupMappingMap;

    public KudosDynamicDataSource(Map<String, DataSourceMapping> dataSourceGroupMappingMap) {
        this.dataSourceGroupMappingMap = dataSourceGroupMappingMap;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String datasourceGroupKey = remoteConfigFetcher.fetchCurrDatasourceGroupKey();
        // TODO: get datasource and table number by sharding strategy.
        ShardingResult shardingResult = ShardingContextHolder.DB_CTX.get();
        String dataSourceNo = shardingResult.getDataSourceNo();
        final String dataSourceLookupKey = datasourceGroupKey + SEPARATOR + "daily_test" + "_" + dataSourceNo;
        System.out.println("dataSourceLookupKey = " + dataSourceLookupKey);
        return dataSourceLookupKey;
    }

    @Override
    public void afterPropertiesSet() {
        // set target datasource
        Map<Object, Object> targetDataSourceMap = new HashMap<>();
        for (Map.Entry<String, DataSourceMapping> entry : dataSourceGroupMappingMap.entrySet()) {
            final String groupKey = entry.getKey();
            final DataSourceMapping dataSourceMapping = entry.getValue();
            final Map<String, DataSource> dataSourceMap = dataSourceMapping.getDataSourceMap();
            for (Map.Entry<String, DataSource> dataSourceEntry : dataSourceMap.entrySet()) {
                final String key = dataSourceEntry.getKey();
                targetDataSourceMap.put(groupKey + SEPARATOR + key, dataSourceEntry.getValue());
            }
        }
        // all ds
        super.setTargetDataSources(targetDataSourceMap);
        // default ds
        super.setDefaultTargetDataSource(targetDataSourceMap.get(remoteConfigFetcher.fetchCurrDatasourceGroupKey() + SEPARATOR + "daily_test"));
        // have to call this method
        super.afterPropertiesSet();
    }
}
