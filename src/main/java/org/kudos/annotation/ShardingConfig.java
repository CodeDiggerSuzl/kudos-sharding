package org.kudos.annotation;


import org.kudos.sharding.strategy.ShardingStrategy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * sharding config for other tables.
 *
 * @author suzl
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ShardingConfig {


    /**
     * table name
     */
    String tableName();

    Class<? extends ShardingStrategy> shardingStrategy();


    /**
     * specific db number, always choose this datasource
     */
    String databaseNo() default "";

    /**
     * same as specificDataSourceNumber
     */
    String tableNo() default "";
}

