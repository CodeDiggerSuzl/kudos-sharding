package org.kudos.context;

import org.kudos.sharding.ShardingResult;

import java.util.Map;

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

    /**
     * table name(with table number), carry from  Executor to StatementHandler layer
     */
    public static final ThreadLocal<Map<String, String>> tableNameMapCtx = new ThreadLocal<>();

    /**
     * 清楚全部的上下文
     */
    public static void clearAllContexts() {
        SHARDING_RESULT_CTX.remove();
        SHARDING_FLAG_CTX.remove();
        tableNameMapCtx.remove();
    }

}
