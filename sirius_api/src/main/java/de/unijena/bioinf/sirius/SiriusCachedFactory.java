package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SiriusCachedFactory implements SiriusFactory {
    private final Map<String, Sirius> instanceCache = new ConcurrentHashMap<>();

    @Override
    public @NotNull Sirius sirius(@Nullable String profileName) {
        if (profileName == null || profileName.isEmpty())
            profileName = PropertyManager.DEFAULTS.getConfigValue("AlgorithmProfile");

        return instanceCache.computeIfAbsent(profileName, Sirius::new);
    }
}
