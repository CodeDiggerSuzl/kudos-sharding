package org.kudos.context;

import org.kudos.sharding.ShardingResult;

/**
 * carry stuff by thread local.
 *
 * @author suzl
 */
public class ShardingContextHolder {

    public ShardingContextHolder() {
    }

    /**
     * sharding result.
     */
    public static final ThreadLocal<ShardingResult> DB_CTX = new ThreadLocal<>();


}
