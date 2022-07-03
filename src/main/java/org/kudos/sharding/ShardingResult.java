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

    private String dataSourceNo;

    private String tableNo;
}
