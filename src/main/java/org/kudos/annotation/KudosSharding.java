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
     * table names of this sharding, could not be null.
     * <p>
     * if allTableNames is only one and equals main tableName, then just use main tableName
     * <p>
     * if allTableNames is more than one(have to contain the main tableName), the mainTableShardingStrategy is for main
     * class, and the rest of tableNames will use the sharding strategy in config file.
     * <b>
     * <p>
     * all table names after sharding their data source has to be the same. the business caller has to guarantee that.
     * </b>
     */
    String[] allTableNames();

    String mainTableName(); // necessary or not ?

    /**
     * sharding property. not null
     */
    String shardingKey();

    /**
     * could be null.
     */
    Class<? extends ShardingStrategy> mainTableShardingStrategy() default HashStrategy.class;


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
