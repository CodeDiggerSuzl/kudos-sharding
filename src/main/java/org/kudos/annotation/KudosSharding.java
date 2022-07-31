package org.kudos.annotation;

import org.kudos.sharding.strategy.HashStrategy;
import org.kudos.sharding.strategy.ShardingStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * sharding conf marked on mybatis interface. aka. dao or mapper interfaces.
 *
 * @author suzl
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface KudosSharding {

    /**
     * main table name, could not be null.
     */
    String mainTableName();

    /**
     * sharding property. not null.
     */
    String shardingKey();

    /**
     * sharding strategy for main table. not null.
     */
    Class<? extends ShardingStrategy> shardingStrategy() default HashStrategy.class;

    /**
     * connector between table and table number.
     */
    String tableConnector() default "_";

    /**
     * Other table names of this sharding, without main table.
     * <p>
     * If other table names is empty, the main table is the only table of this sharding.
     * <p>
     * Main table will use the shardingStrategy in the annotation and the other tables will use the config in config files.
     * <b>
     * <p>
     * All table names after sharding their data source must be the same. the business caller has to guarantee that.
     * </b>
     */
    ShardingConfig[] otherTableShardingConfig() default {}; // other table names, using for join table.

    /**
     * sharding config for date sharding.
     */
    DateConfig dateShardingConfig() default @DateConfig;

}
