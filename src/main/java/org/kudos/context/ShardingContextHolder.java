package org.kudos.context;

import lombok.extern.slf4j.Slf4j;
import org.kudos.sharding.ShardingResult;

import java.util.Map;

/**
 * context holder by thread local.
 *
 * @author suzl
 */
@Slf4j
public class ShardingContextHolder {

    public ShardingContextHolder() {
    }


    /**
     * DB name context.
     */
    private static final ThreadLocal<ShardingResult> SHARDING_RESULT_CTX = new ThreadLocal<>();

    /**
     * whether this mapper need to be sharded or not, carry from  Executor to StatementHandler layer
     */
    private static final ThreadLocal<Boolean> SHARDING_FLAG_CTX = new ThreadLocal<>();

    /**
     * table name(with table number), carry from  Executor to StatementHandler layer
     */
    private static final ThreadLocal<Map<String, String>> TABLE_NAME_MAP_CTX = new ThreadLocal<>();

    public static void setTableNameMap(Map<String, String> tableNameMap) {
        TABLE_NAME_MAP_CTX.set(tableNameMap);
    }

    public static Map<String, String> getTableNameMap() {
        return TABLE_NAME_MAP_CTX.get();
    }


    public static void setShardingResult(ShardingResult shardingResult) {
        SHARDING_RESULT_CTX.set(shardingResult);
    }

    public static ShardingResult getShardingResult() {
        return SHARDING_RESULT_CTX.get();
    }

    public static void setShardingFlag(Boolean shardingFlag) {
        SHARDING_FLAG_CTX.set(shardingFlag);
    }

    public static Boolean getShardingFlag() {
        return SHARDING_FLAG_CTX.get();
    }

    /**
     * clear all the ctx.
     */
    public static void clearAllContexts() {
        SHARDING_RESULT_CTX.remove();
        SHARDING_FLAG_CTX.remove();
        TABLE_NAME_MAP_CTX.remove();
    }

}
