/*
 * Class: JsonMapFlattener
 *
 * Created on Oct 29, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.sps.cdipp.kafkaconnect.application.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public final class JsonMapFlattener {
    
    private JsonMapFlattener() {
    }

    /**
     * Flatten a hierarchical {@link Map} into a flat {@link Map} with key names using
     * property dot notation.
     * @param inputMap must not be {@literal null}.
     * @return the resulting {@link Map}.
     */
    public static Map<String, Object> flatten(final Map<String, Object> inputMap) {
        final Map<String, Object> result = new HashMap<>();

        if(inputMap == null || inputMap.isEmpty()) {
            return result;
        }
        final Iterator<? extends Entry<String, ?>> mapIterator = inputMap.entrySet().iterator();

        while(mapIterator.hasNext()) {
            
            final Entry<String, ? extends Object> entry = mapIterator.next();
            doFlatten(entry.getKey(), entry.getValue(), result);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void doFlatten(String propertyName, Object value, Map<String, Object> properties) {
        if (value instanceof Map) {
            ((Map<Object, Object>) value).forEach(
                (k, v) -> doFlatten((propertyName != null ? propertyName + "." : "") + k, v, properties));
        } else {    
            properties.put(propertyName, value);
        }
    }
}