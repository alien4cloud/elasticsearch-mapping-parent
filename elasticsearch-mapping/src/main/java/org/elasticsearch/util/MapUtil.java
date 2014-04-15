package org.elasticsearch.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility to help working with maps in the ES mapping project..
 * 
 * @author luc boutier
 */
public final class MapUtil {
    /** Utility class have private constructor. */
    private MapUtil() {
    }

    /**
     * Create a map that contains a unique entry based on given parameters.
     * 
     * @param key The key of the unique map entry.
     * @param value The value of the unique map entry.
     * @return A map that contains a single entry from the given key and value.
     */
    public static Map<String, Object> getMap(String key, Object value) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(key, value);
        return map;
    }

    /**
     * Create a map that contains a entries based on given parameters.
     * 
     * @param key An array of the keys of the entries to add to the map.
     * @param value An array of the values for every key provided.
     * @return A map that is already filled with entries from the given key and values arrays.
     */
    public static Map<String, Object> getMap(String[] keys, Object[] values) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }
        return map;
    }
}