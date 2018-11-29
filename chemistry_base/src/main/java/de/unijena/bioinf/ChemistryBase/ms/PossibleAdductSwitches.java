package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import de.unijena.bioinf.ms.properties.PropertyManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DefaultProperty
public class PossibleAdductSwitches implements Ms2ExperimentAnnotation {
    public static PossibleAdductSwitches DEFAULT() {
        return PropertyManager.DEFAULTS.createInstanceWithDefaults(PossibleAdductSwitches.class);
    }
    //todo what about K?!

    private final Map<Ionization, List<Ionization>> precursorIonizationToFragmentIonizations;

    public PossibleAdductSwitches(Map<Ionization, List<Ionization>> precursorIonizationToFragmentIonizations) {
            this.precursorIonizationToFragmentIonizations = precursorIonizationToFragmentIonizations;
        }

        public List<Ionization> getPossibleIonizations(Ionization precursorIonization) {
            List<Ionization> ionizations = precursorIonizationToFragmentIonizations.get(precursorIonization);
            if (ionizations==null) return Collections.singletonList(precursorIonization);
            return ionizations;
        }

    public static PossibleAdductSwitches fromString(String stringValue) {
        String[] mapEntries = stringValue.split("}\\s*,");
        Map<Ionization, List<Ionization>> precursorIonizationToFragmentIonizations = Arrays.stream(mapEntries)
                .map(s -> s.trim().replaceAll("\\{", "").split(":"))
                .collect(Collectors.toMap(
                        p -> PrecursorIonType.fromString(p[0].trim()).getIonization(),
                        p -> Arrays.stream(p[1].split(","))
                                .map(i -> PrecursorIonType.fromString(i.trim()).getIonization())
                                .distinct()
                                .collect(Collectors.toList()))
                );
        return new PossibleAdductSwitches(precursorIonizationToFragmentIonizations);
    }

    public static void main(String[] args) {
        PossibleAdductSwitches pp = fromString("[M+H ]+ :{ [M+Na ]+,[M +K]+, [M+K]+}    ,[M+    Na]+:   {[M+H]+   ,[M+K]+},[M+K]+:{[M+H]+}");
        System.out.printf("Blub");
    }
}
