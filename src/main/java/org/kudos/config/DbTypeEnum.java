package org.kudos.config;

/**
 * data source type. not using yet.
 *
 * @author suzl
 */
public enum DbTypeEnum {

    READ(1, "read/follower"),

    WRITE(2, "write/leader");
    private final Integer type;

    private final String description;

    DbTypeEnum(Integer type, String description) {
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
