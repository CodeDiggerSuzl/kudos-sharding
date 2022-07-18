package org.kudos.intercepter;


import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.mapping.MappedStatement;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * TODO add this
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Component
public class ShardingHelper {
    /**
     * Judge all kind of arg type,and get the sharding key's value.
     *
     * @param key             sharding key's name
     * @param arg             mybatis argument
     * @param mappedStatement mybatis mapped statement
     * @return sharding key's value
     */
    public Object getShardingKeyValue(String key, Object arg, MappedStatement mappedStatement)
            throws NoSuchFieldException, IllegalAccessException {
        // multiple parameters in the mapper interface
        if (arg instanceof MapperMethod.ParamMap) {
            MapperMethod.ParamMap parameterMap = (MapperMethod.ParamMap) arg;
            // get sharding key value by parameterMap
            Object valueObject = parameterMap.get(key);
            if (valueObject == null) {
                // will not loop the parameterMap after directly getting the value failed, e.g. 'valueObject' is null.
                // cause even if we could get the value from one of the Object in the parameterMap, but is unnecessary and could be wrong.
                // it might cause unexpected results.
                // it's better and highly recommend to use the mybatis '@Param("key")' to mark the sharding key. it's much more simple and efficient.
                throw new RuntimeException(String.format("Can't get sharding value via sharding key:[%s]", key));
            }
            // Array and Map & List
            return valueObject;
        }

        // single parameter in the mapper interface
        else {
            // if the parameter is a base type and not marked by @Param.
            if (currTypeIsBasic(arg)) {
                throw new RuntimeException(String.format("Please use @Param to mark the sharding key:[%s]", key));
            }

            if (arg instanceof Map) {
                Map<String, Object> parameterMap = (Map<String, Object>) arg;
                return parameterMap.get(key);
            } else {
                Class<?> parameterObjectClass = arg.getClass();
                Field declaredField = parameterObjectClass.getDeclaredField(key);
                declaredField.setAccessible(true);
                return declaredField.get(arg);
            }
        }
    }

    /**
     * judge the arg type is base type or not.
     *
     * @param object arg
     * @return true or false
     */
    private boolean currTypeIsBasic(Object object) {
        return object.getClass().isPrimitive()
                || object instanceof String
                || object instanceof Integer
                || object instanceof Double
                || object instanceof Float
                || object instanceof Long
                || object instanceof Boolean
                || object instanceof Byte
                || object instanceof Short;
    }


}
