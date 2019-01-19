package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public class AdductSettings implements Ms2ExperimentAnnotation {

    @NotNull protected final Set<PrecursorIonType> enforced;
    @NotNull protected final Set<PrecursorIonType> detectable;
    @NotNull protected final Set<PrecursorIonType> fallback;

    /**
     * @param enforced ion modes that are always considered
     * @param detectable ion modes which are only considered if there is an indication in the MS1 scan (e.g. right mass delta)
     * @param fallback ion modes which are considered if the auto detection did not find any indication for an ion mode.
     */
    public static AdductSettings newInstance(@DefaultProperty Set<PrecursorIonType> enforced, @DefaultProperty Set<PrecursorIonType> detectable, @DefaultProperty  Set<PrecursorIonType> fallback) {
        return new AdductSettings(enforced, detectable, fallback);
    }


    protected AdductSettings() {
        this.enforced = new HashSet<>();
        this.detectable = new HashSet<>();
        this.fallback = new HashSet<>();
    }

    public AdductSettings(@NotNull Set<PrecursorIonType> enforced, @NotNull Set<PrecursorIonType> detectable, @NotNull Set<PrecursorIonType> fallback) {
        this.enforced = enforced;
        this.detectable = detectable;
        this.fallback = fallback;
    }

    public Set<PrecursorIonType> getEnforced() {
        return enforced;
    }

    public Set<PrecursorIonType> getDetectable() {
        return detectable;
    }

    public Set<PrecursorIonType> getFallback() {
        return fallback;
    }


    public Set<PrecursorIonType> getEnforced(int polarity) {
        return ensureRightPolarity(enforced, polarity);
    }

    public Set<PrecursorIonType> getDetectable(int polarity) {
        return ensureRightPolarity(detectable, polarity);
    }

    public Set<PrecursorIonType> getFallback(int polarity) {
        return ensureRightPolarity(fallback, polarity);
    }

    private Set<PrecursorIonType> ensureRightPolarity(Set<PrecursorIonType> set, int polarity) {
        return set.stream().filter(x->x.getCharge()==polarity).collect(Collectors.toSet());
    }

}
