package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ChemistryBase.chem.IonMode;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This configurations define which ion modes have to be considered, as well as the auto detection of ion modes via MS1.
 * Note: An ion mode is an elementary modification of the neutral molecular formula. For example, protonation is an
 * ion mode, formid acid, however, is an adduct. [M+H]+, [M+K]+, and [M+Na]+ are ion modes, [M+NH3+H]+ is an adduct.
 * Note: When a compound is assigned to a specific adduct or ion mode, this setting is ignored.
 */
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

    /**
     * @param enforced ion modes that are always considered
     * @param detectable ion modes which are only considered if there is an indication in the MS1 scan (e.g. right mass delta)
     * @param fallback ion modes which are considered if the auto detection did not find any indication for an ion mode.
     */
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
