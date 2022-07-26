package org.kudos.sharding.strategy;

import org.kudos.enums.ShardingStrategyType;
import org.kudos.sharding.ShardingResult;
import org.springframework.stereotype.Component;

/**
 * hash sharding strategy. default sharding strategy.
 *
 * @author suzl
 */
@Component
public class HashStrategy implements ShardingStrategy {

    /**
     * sharding by hash code.
     *
     * @param value         sharding field
     * @param datasourceCnt total data source count
     * @param tableTotalCnt total table count
     * @return sharding result
     */
    @Override
    public ShardingResult sharding(Object value, int datasourceCnt, int tableTotalCnt) {
        final String val = value.toString();
        int hash = val.hashCode() < 0 ? Math.abs(val.hashCode()) : val.hashCode();
        final int eachDbTableCnt = tableTotalCnt / datasourceCnt;
        final int tableNo = hash % tableTotalCnt;
        final int dsNo = tableNo / eachDbTableCnt;
        String dataSourceNum = String.format("%04d", dsNo);
        String tableNum = String.format("%04d", tableNo);
        return new ShardingResult(dataSourceNum, tableNum);
    }

    @Override
    public String getStrategyName() {
        return ShardingStrategyType.hash.name();
    }


}
