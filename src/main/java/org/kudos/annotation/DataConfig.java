package org.kudos.annotation;

public @interface DataConfig {
    /**
     * only useful for date sharding strategy
     */
    String dateFormat() default "yyyyMMdd";
}
