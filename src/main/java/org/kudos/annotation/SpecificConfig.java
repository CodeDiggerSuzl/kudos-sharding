package org.kudos.annotation;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 制定分库分表的策略
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface SpecificConfig {


    /**
     * table name
     */
    String tableName();

    /**
     * specific db number, always choose this datasource
     */
    String databaseNo();

    /**
     * same as specificDataSourceNumber
     */
    String tableNumber();


}

