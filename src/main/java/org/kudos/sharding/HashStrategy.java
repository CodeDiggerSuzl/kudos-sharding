package org.kudos.sharding;

/**
 * hash sharding strategy. default sharding strategy.
 *
 * @author suzl
 */

public class HashStrategy implements ShardingStrategy {

    /**
     * sharding by hash code.
     *
     * @param value         sharding field
     * @param dsTotalCnt    total data source count
     * @param tableTotalCnt total table count
     * @return sharding result
     */
    @Override
    public ShardingResult sharding(Object value, int dsTotalCnt, int tableTotalCnt) {
        int hash = Math.abs(value.toString().hashCode());
        final int eachDbTableCnt = tableTotalCnt / dsTotalCnt;
        int dsNo = hash % dsTotalCnt;
        int tableNo = (hash % eachDbTableCnt) + (dsNo * eachDbTableCnt);
        String dataSourceNum = String.format("%04d", dsNo);
        String tableNum = String.format("%04d", tableNo);
        return new ShardingResult(dataSourceNum, tableNum);
    }

    @Override
    public String shardingStrategyName() {
        return ShardingStrategyType.hash.name();
    }


}
