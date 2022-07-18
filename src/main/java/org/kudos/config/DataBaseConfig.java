package org.kudos.config;

import lombok.Data;
import org.kudos.enums.DatabaseType;

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
    private DatabaseType dbType;

    private String url;

    private String password;

    private String username;

    private String driverClassName;

}
