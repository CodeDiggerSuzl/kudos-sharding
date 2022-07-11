package org.kudos.sharding.strategy;

import org.kudos.sharding.strategy.ShardingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * sharding context
 *
 * @author suzl
 */
@Component
public class ShardingContext {

    /**
     * key: sharing strategy name
     * <p></p>
     * value: sharing strategy
     */
    private final Map<String, ShardingStrategy> strategyMap = new ConcurrentHashMap<>();

    @Autowired
    private ShardingContext(Map<String, ShardingStrategy> strategyMap) {
        strategyMap.forEach((k, v) -> this.strategyMap.put(v.getStrategyName(), v));
    }

    public ShardingStrategy getStrategy(String shardingType) {
        if (shardingType == null) {
            return null;
        }
        return strategyMap.get(shardingType);
    }

}
