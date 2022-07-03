package org.kudos.config;

import lombok.Data;

/**
 * basic config of a database.
 *
 * @author suzl
 */
@Data
public class DataBaseConfig {

    /**
     * db type read or write
     */
    private DbTypeEnum dbType;

    private String url;

    private String password;

    private String username;

    private String driverClassName;

}
