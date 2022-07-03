package org.kudos.sharding;

/**
 * Base sharding strategy.
 *
 * <p>You can customize your own sharding strategy by implement this class.</p>
 *
 * <p>
 * <b>Notice:</b> if there is 4 datasource and 128 tables total, the db and table number will be:
 * <ol>
 * <li>the first db is db_0 and table range is 0-31</li>
 * <li>the second db is db_1 and table range is 32-63</li>
 * <li>the third db is db_2 and table range is 64-95</li>
 * <li>the fourth db is db_3 and table range is 96-127</li>
 * </ol>
 * </p>
 *
 * @author suzl
 */
public interface ShardingStrategy {

    /**
     * sharding method
     *
     * @param value    the value of  sharding key
     * @param dsCnt    the total count of datasource
     * @param tableCnt the total count of table
     * @return the sharding result
     */
    ShardingResult sharding(Object value, int dsCnt, int tableCnt);

    /**
     * the name of sharding strategy
     *
     * @return the name of sharding strategy
     */
    String shardingStrategyName();
}
