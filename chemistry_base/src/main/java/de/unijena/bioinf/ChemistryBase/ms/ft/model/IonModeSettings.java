package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ChemistryBase.chem.IonMode;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class IonModeSettings implements Ms2ExperimentAnnotation  {

    protected final Set<IonMode> enforced;
    protected final Set<IonMode> detectable;
    protected final Set<IonMode> fallback;

    protected IonModeSettings() {
        this.enforced = new HashSet<>();
        this.detectable = new HashSet<>();
        this.fallback = new HashSet<>();
    }

    public IonModeSettings(Set<IonMode> enforced, Set<IonMode> detectable, Set<IonMode> fallback) {
        this.enforced = enforced;
        this.detectable = detectable;
        this.fallback = fallback;
    }

    public static IonModeSettings newInstance(@DefaultProperty Set<IonMode> enforced, @DefaultProperty Set<IonMode> detectable, @DefaultProperty  Set<IonMode> fallback) {
        return new IonModeSettings(enforced, detectable, fallback);
    }

    public Set<IonMode> getEnforced() {
        return enforced;
    }

    public Set<IonMode> getDetectable() {
        return detectable;
    }

    public Set<IonMode> getFallback() {
        return fallback;
    }


    public Set<IonMode> getEnforced(int polarity) {
        return ensureRightPolarity(enforced, polarity);
    }

    public Set<IonMode> getDetectable(int polarity) {
        return ensureRightPolarity(detectable, polarity);
    }

    public Set<IonMode> getFallback(int polarity) {
        return ensureRightPolarity(fallback, polarity);
    }

    private Set<IonMode> ensureRightPolarity(Set<IonMode> set, int polarity) {
        return set.stream().filter(x->x.getCharge()==polarity).collect(Collectors.toSet());
    }


}
