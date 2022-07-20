package org.kudos.annotation;


/**
 * for date sharding strategy.
 *
 * @author suzl
 */
public @interface DateConfig {
    /**
     * only useful for date sharding strategy
     */
    String dateFormat() default "yyyyMMdd";
}
