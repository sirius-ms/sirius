package de.unijena.bioinf.sirius;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface SiriusFactory {
    Sirius sirius(@Nullable String profile);

    default Sirius sirius() {
        return sirius(null);
    }
}
