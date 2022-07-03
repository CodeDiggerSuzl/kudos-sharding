package org.kudos.config;

import org.springframework.stereotype.Component;

/**
 * This config is to mock outer control of the dynamic data source.
 * <p>
 * You can config the current data source group config(or other config)in nacos, apollo, etcd, zk, redis, etc.
 * <p>
 * Dynamically change the data source group by a switch or a trigger.
 * TODO: implement this later.
 *
 * @author suzl
 */
@Component
public class RemoteConfigFetcher {

    public String fetchCurrDatasourceGroupKey() {
        return "default";
    }

}
