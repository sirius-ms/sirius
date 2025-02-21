package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import java.util.HashMap;
import java.util.Map;

/**
 * A custom Map that supports prefix-based lookups.
 */
public class PrefixAwareString2ObjectHashMap<V> extends HashMap<String, V> {

    public PrefixAwareString2ObjectHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public PrefixAwareString2ObjectHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public PrefixAwareString2ObjectHashMap() {
        super();
    }

    public PrefixAwareString2ObjectHashMap(Map<String, V> initialMappings) {
        super(initialMappings);
    }

    @Override
    public V get(Object key) {
        //search for direct match
        V value = super.get(key);
        if (value != null)
            return value;


        //check for prefixes
        String currentPrefix = (String) key;

        while (!currentPrefix.isEmpty()) {
            int lastDot = currentPrefix.lastIndexOf('.');
            if (lastDot == -1) {
                break;
            }

            currentPrefix = currentPrefix.substring(0, lastDot);

            String lookupKey = currentPrefix + ".*";
            if (super.containsKey(lookupKey)) {
                return super.get(lookupKey);
            }
        }

        return null; // No matching prefix found, fallback to Lucene default behavior
    }
}
