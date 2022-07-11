package org.kudos.context;

import org.kudos.sharding.ShardingResult;

/**
 * context holder by thread local.
 *
 * @author suzl
 */
public class ShardingContextHolder {

    public ShardingContextHolder() {
    }

    /**
     * DB name context.
     */
    public static final ThreadLocal<ShardingResult> SHARDING_RESULT_CTX = new ThreadLocal<>();

    /**
     * whether this mapper need to be sharded or not, carry from  Executor to StatementHandler layer
     */
    public static final ThreadLocal<Boolean> SHARDING_FLAG_CTX = new ThreadLocal<>();

}
