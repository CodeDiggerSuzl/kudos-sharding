package org.kudos.sharding;

/**
 * support sharding strategy type.
 *
 * @author suzl
 */
public enum ShardingStrategyType {
    /**
     * sharding by hash code
     */
    hash,
    /**
     * sharding by date
     */
    date,
    /**
     * sharding by specific db number and table num
     */
    specific,
    /**
     * sharding by city code
     */
    city,

}
