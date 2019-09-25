package de.unijena.bioinf.sirius;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SiriusCachedFactory implements SiriusFactory {
    private final Map<String, Sirius> instanceCache = new ConcurrentHashMap<>();

    @Override
    public @NotNull Sirius sirius(@NotNull final String profileName) {
        return instanceCache.computeIfAbsent(profileName, Sirius::new);
    }
}
