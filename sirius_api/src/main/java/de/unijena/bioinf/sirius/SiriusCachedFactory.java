package de.unijena.bioinf.sirius;

import de.unijena.bioinf.sirius.plugins.AdductSwitchPlugin;
import de.unijena.bioinf.sirius.plugins.IsotopePatternInMs1Plugin;
import de.unijena.bioinf.sirius.plugins.TreeStatisticPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SiriusCachedFactory implements SiriusFactory {
    private final Map<String, Sirius> instanceCache = new ConcurrentHashMap<>();

    @Override
    public @NotNull Sirius sirius(@NotNull final String profileName) {
        return instanceCache.computeIfAbsent(profileName, (profile) -> {
            final Sirius sirius = new Sirius(profile);
            sirius.getMs2Analyzer().registerPlugin(new TreeStatisticPlugin());
            sirius.getMs2Analyzer().registerPlugin(new AdductSwitchPlugin());
            sirius.getMs2Analyzer().registerPlugin(new IsotopePatternInMs1Plugin());
            return sirius;
        });
    }
}
