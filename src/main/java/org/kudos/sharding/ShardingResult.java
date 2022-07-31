package org.kudos.sharding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * sharding result
 *
 * @author suzl
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShardingResult {

    /**
     * the no of database
     */
    private String dataSourceNo;
    /**
     * the no of table
     */
    private String tableNo;
}
