package org.kudos.annotation;

import org.kudos.sharding.HashStrategy;
import org.kudos.sharding.ShardingStrategy;

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

    /*table names of this sharding, could not be num*/
    String[] tableName();

    /**
     * sharding property. not null
     */
    String shardingKey();

    /**
     * weather to use sharding key or not
     */
    // is this filed necessary?
    /// boolean useShardingKey() default true;

    /**
     * could be null.
     */
    Class<? extends ShardingStrategy> shardingStrategy() default HashStrategy.class;

    /**
     * connector between table and table number
     */
    String tableConnector() default "_";

    /*
     * only useful for specific sharding strategy
     */
    /**
     * specific db number, always choose this datasource
     */
    String specificDataSourceNumber();

    /**
     * same as specificDataSourceNumber
     */
    String specificTableNumber();

    /**
     * only useful for date sharding strategy
     */
    String dateFormat() default "yyyyMMdd";

}
