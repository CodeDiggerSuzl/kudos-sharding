package org.kudos.enums;

/**
 * data source type. not using yet.
 *
 * @author suzl
 */
public enum DatabaseType {

    READ(1, "read/follower"),

    WRITE(2, "write/leader");
    private final Integer type;

    private final String description;

    DatabaseType(Integer type, String description) {
        this.type = type;
        this.description = description;
    }

    public Integer getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}
