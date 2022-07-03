package org.kudos.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Json utils base on Jackson instead of fastjson.
 * <p>
 *
 * @author suzl
 */
public class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String toStr(Object o) {
        String result = "";
        try {
            result = MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            // do nothing
        }
        return result;
    }

}
