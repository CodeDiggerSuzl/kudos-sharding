package org.kudos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * This config is to mock outer control of the dynamic data source.
 * <p>
 * You can config the current data source group config(or other config)in nacos, apollo, etcd, zk, redis, etc.
 * <p>
 * Dynamically change the data source group by a switch or a trigger.
 * <p>
 * Currently, we just use redis to store the data source group config.
 *
 * @author suzl
 */
@Component
@Slf4j
public class RemoteConfigFetcher {
    private static final JedisPool pool = new JedisPool("localhost", 6379); //NOTICE:only temporary solution.

    private static final String DEFAULT_CONFIG = "default";

    private static final String CONFIG_KEY = "kudos_sharding_default_data_source_group";

    public String fetchCurrDatasourceGroupKey() {
        try (Jedis resource = pool.getResource()) {
            String remoteConfig = resource.get(CONFIG_KEY);
            log.info("get remote data source group config: {}", remoteConfig);
            if (StringUtils.isEmpty(remoteConfig)) {
                return DEFAULT_CONFIG;
            }
            return remoteConfig;
        } catch (Exception e) {
            log.error("error fetching remote group key", e);
            return DEFAULT_CONFIG;
        }
    }

}
