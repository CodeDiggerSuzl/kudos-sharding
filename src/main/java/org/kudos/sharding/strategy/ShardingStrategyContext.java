package org.kudos.sharding.strategy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.kudos.utils.JsonUtils;
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
@Slf4j
public class ShardingStrategyContext {

    /**
     * key: sharing strategy name
     * <p></p>
     * value: sharing strategy
     */
    private final Map<String, ShardingStrategy> strategyMap = new ConcurrentHashMap<>();

    @Autowired
    private ShardingStrategyContext(Map<String, ShardingStrategy> strategyMap) {
        log.info("init all sharding strategies = {}", JsonUtils.toStr(strategyMap));
        strategyMap.forEach((k, v) -> this.strategyMap.put(v.getStrategyName(), v));
    }

    public ShardingStrategy getStrategy(String shardingStrategyName) {
        if (StringUtils.isBlank(shardingStrategyName)) {
            log.info("sharding strategy name is empty, return null");
            return null;
        }

        return strategyMap.get(shardingStrategyName);
    }

}
