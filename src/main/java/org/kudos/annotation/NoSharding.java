package org.kudos.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * no sharding mark.
 * <p>
 * once marked a mapper, it will not be sharded.
 *
 * @author suzl
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface NoSharding {
}
