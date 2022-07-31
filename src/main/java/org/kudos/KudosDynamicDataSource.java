package org.kudos;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.kudos.config.DataSourceMapping;
import org.kudos.config.RemoteConfigFetcher;
import org.kudos.context.ShardingContextHolder;
import org.kudos.sharding.ShardingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * DynamicDataSource.
 * <p>
 *
 * @author suzl
 * @see AbstractRoutingDataSource read the doc of this class to figure it out.
 */
@Component
@Slf4j
public class KudosDynamicDataSource extends AbstractRoutingDataSource {

    private static final String SEPARATOR = "_:_";

    public KudosDynamicDataSource() {
    }

    @Autowired
    private RemoteConfigFetcher remoteConfigFetcher;

    private Map<String, DataSourceMapping> dataSourceGroupMappingMap;
    private String defaultDataGroupKey;

    public KudosDynamicDataSource(Map<String, DataSourceMapping> dataSourceGroupMappingMap, String defaultDataGroupKey) {
        this.dataSourceGroupMappingMap = dataSourceGroupMappingMap;
        this.defaultDataGroupKey = defaultDataGroupKey;
    }


    @Override
    protected Object determineCurrentLookupKey() {
        String dataSourceGroupKey = remoteConfigFetcher.fetchCurrDatasourceGroupKey();
        //        String dataSourceGroupKey = "default";
        log.info("determine curr look up key, got dataSource group key = {}", dataSourceGroupKey);
        if (StringUtils.isEmpty(dataSourceGroupKey)) {
            log.info("got nothing from data source group key, use default group key instead");
            dataSourceGroupKey = defaultDataGroupKey;
        }
        // if the group key is not ok.
        if (Strings.isBlank(dataSourceGroupKey)) {
            throw new RuntimeException("data source group key is empty, please check the remote config datasource group key.");
        }
        final DataSourceMapping dataSourceMapping = dataSourceGroupMappingMap.get(dataSourceGroupKey);
        if (dataSourceMapping == null) {
            throw new RuntimeException("data source group is not found with key [ " + dataSourceGroupKey + " ], please check the remote config datasource group key.");
        }

        // no sharding.
        // try to get sharding flag
        Boolean shardingFlag = ShardingContextHolder.getShardingFlag();
        final String defaultDataSourceName = dataSourceMapping.getDefaultDataSourceName();
        if (Boolean.FALSE.equals(shardingFlag)) {
            return getLookupKey(dataSourceGroupKey, defaultDataSourceName);
        }

        // sharding
        final ShardingResult shardingResult = ShardingContextHolder.getShardingResult();
        log.info("[kudos sharding]  ShardingContextHolder sharding result = {}", shardingResult);
        if (shardingResult == null || StringUtils.isEmpty(shardingResult.getDataSourceNo())) {
            throw new RuntimeException("get nothing from sharing result context");
        }
        final String dataSourceNo = shardingResult.getDataSourceNo();
        final Map<String, String> dbNumberMapping = dataSourceMapping.getDbNumberMapping();
        final String dataSourceName = dbNumberMapping.get(dataSourceNo);
        if (StringUtils.isEmpty(dataSourceName)) {
            throw new RuntimeException(String.format("could get data source name by data source no: %s.", dataSourceNo));
        }
        final String lookupKey = getLookupKey(dataSourceGroupKey, dataSourceName);
        log.debug("determineCurrentLookupKey: got lookup key = {}", lookupKey);
        return lookupKey;
    }

    /**
     * get default look up key by a default format.
     *
     * @param dsGroupKey     data source group key
     * @param dataSourceName data source name
     * @return look up key
     */
    private String getLookupKey(String dsGroupKey, String dataSourceName) {
        final String lookupKey = String.format("%s%s%s", dsGroupKey, SEPARATOR, dataSourceName);
        log.info("get lookup key = {}", lookupKey);
        return lookupKey;
    }

    @Override
    public void afterPropertiesSet() {
        final String datasourceGroupKey = defaultDataGroupKey;
        if (StringUtils.isEmpty(datasourceGroupKey)) {
            throw new RuntimeException("data source group key is empty. please check the remote config datasource group key.");
        }
        final DataSourceMapping dsMapping = dataSourceGroupMappingMap.get(datasourceGroupKey);
        if (dsMapping == null) {
            throw new RuntimeException("data source group is not found with key [ " + datasourceGroupKey + " ], please check the remote config datasource group key.");
        }
        final String defaultDataSourceName = dsMapping.getDefaultDataSourceName();

        // set target datasource
        Map<Object, Object> targetDataSourceMap = new HashMap<>(16);
        for (Map.Entry<String, DataSourceMapping> entry : dataSourceGroupMappingMap.entrySet()) {
            final String groupKey = entry.getKey();
            final DataSourceMapping dataSourceMapping = entry.getValue();
            final Map<String, DataSource> dataSourceMap = dataSourceMapping.getDataSourceMap();
            for (Map.Entry<String, DataSource> dataSourceEntry : dataSourceMap.entrySet()) {
                final String dsName = dataSourceEntry.getKey();
                targetDataSourceMap.put(getLookupKey(groupKey, dsName), dataSourceEntry.getValue());
            }
        }

        // all ds
        super.setTargetDataSources(targetDataSourceMap);
        // default ds
        final Object defaultTargetDataSource = targetDataSourceMap.get(getLookupKey(datasourceGroupKey, defaultDataSourceName));
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        super.afterPropertiesSet();
    }
}
