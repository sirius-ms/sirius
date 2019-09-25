package de.unijena.bioinf.sirius;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@FunctionalInterface
public interface SiriusFactory {
    Sirius sirius(String profile);
}
