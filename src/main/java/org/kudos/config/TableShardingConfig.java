package org.kudos.config;

import lombok.Data;

/**
 * Sharding config of a table
 *
 * @author suzl
 */
@Data
public class TableShardingConfig {
    // handle various situations of table sharding
    // write doc here
    private String shardingType;

    private Integer dbCnt;

    private Integer tableCnt;
}
