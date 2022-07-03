package org.kudos.config;

import lombok.Data;

/**
 * Sharding config of a table
 *
 * @author suzl
 */
@Data
public class TableShardingConfig {

    private String shardingType;

    private Integer dbCnt;

    private Integer tableCnt;

}
