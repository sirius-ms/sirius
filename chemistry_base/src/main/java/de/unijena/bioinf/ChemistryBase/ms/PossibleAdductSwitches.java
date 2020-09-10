package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.IonMode;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;

import java.util.*;

/**
 * An adduct switch is a switch of the ionization mode within a spectrum, e.g. an ion replaces an sodium adduct
 * with a protonation during fragmentation. Such adduct switches heavily increase the complexity of the
 * analysis, but for certain adducts they might happen regularly. Adduct switches are written in the
 * form "a -> b, a -> c, d -> c" where a, b, c, and d are adducts and a -> b denotes an allowed switch from
 * a to b within the MS/MS spectrum.
 */
@DefaultProperty
public class PossibleAdductSwitches implements Ms2ExperimentAnnotation {

    private final Map<Ionization, Set<Ionization>> precursorIonizationToFragmentIonizations;

    public PossibleAdductSwitches(Map<Ionization, Set<Ionization>> precursorIonizationToFragmentIonizations) {
        this.precursorIonizationToFragmentIonizations = precursorIonizationToFragmentIonizations;
        for (Ionization keys : precursorIonizationToFragmentIonizations.keySet()) {
            precursorIonizationToFragmentIonizations.get(keys).add(keys);
        }
    }

    public Map<Ionization,Set<Ionization>> getTransitions() {
        return Collections.unmodifiableMap(precursorIonizationToFragmentIonizations);
    }

    private final static PossibleAdductSwitches DISABLED = new PossibleAdductSwitches(Collections.emptyMap());

    @DefaultInstanceProvider
    protected static PossibleAdductSwitches fromListOfAdductsSwitches(@DefaultProperty List<String> value) {
        final HashMap<Ionization, Set<Ionization>> map = new HashMap<>();
        for (String ad : value) {
            String[] parts = ad.split("\\s*(:|->)\\s*",2); //->,
            Ionization left = IonMode.fromString(parts[0]);
            Ionization right = IonMode.fromString(parts[1]);
            map.computeIfAbsent(left, (k)->new HashSet<>()).add(right);
        }
        return new PossibleAdductSwitches(map);
    }

    public Set<Ionization> getPossibleIonizations(Ionization precursorIonization) {
        Set<Ionization> ionizations = precursorIonizationToFragmentIonizations.get(precursorIonization);
        if (ionizations==null) return Collections.singleton(precursorIonization);
        return ionizations;
    }

    public static PossibleAdductSwitches disabled() {
        return DISABLED;
    }

    @Override
    public String toString() {
        if (precursorIonizationToFragmentIonizations.isEmpty())
            return "AdductSwitch:disabled";
        else return "AdductSwitch" + precursorIonizationToFragmentIonizations.toString();
    }
}