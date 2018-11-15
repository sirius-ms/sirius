package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

import java.util.*;

public abstract class PossibleAdductSwitches implements Ms2ExperimentAnnotation {

    public abstract List<Ionization> getPossibleIonizations(Ionization precursorIonization);
    
    public static PossibleAdductSwitches getDefault() {
        //todo what about K?!
        Map<Ionization, List<Ionization>> precursorIonizationToFragmentIonizations = new HashMap<>();
        PeriodicTable table = PeriodicTable.getInstance();
        Ionization hIon = table.ionByName("[M+H]+").getIonization();
        Ionization naIon = table.ionByName("[M+Na]+").getIonization();
        List<Ionization> list = new ArrayList<>();
        list.add(hIon); list.add(naIon);
        precursorIonizationToFragmentIonizations.put(naIon, list);
        return new BasicPossibleAdductSwitches(precursorIonizationToFragmentIonizations);
    }
    
    protected static class BasicPossibleAdductSwitches extends PossibleAdductSwitches {

        private final Map<Ionization, List<Ionization>> precursorIonizationToFragmentIonizations;

        public BasicPossibleAdductSwitches(Map<Ionization, List<Ionization>> precursorIonizationToFragmentIonizations) {
            this.precursorIonizationToFragmentIonizations = precursorIonizationToFragmentIonizations;
        }

        @Override
        public List<Ionization> getPossibleIonizations(Ionization precursorIonization) {
            List<Ionization> ionizations = precursorIonizationToFragmentIonizations.get(precursorIonization);
            if (ionizations==null) return Collections.singletonList(precursorIonization);
            return ionizations;
        }
    }
    
    
}
