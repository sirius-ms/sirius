package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.*;

/**
 * Can be attached to a Ms2Experiment or ProcessedInput. If PrecursorIonType is unknown, CSI:FingerID will use this
 * object and for all different adducts.
 */
public class PossibleAdducts extends ArrayList<PrecursorIonType> {

    public PossibleAdducts(Collection<? extends PrecursorIonType> c) {
        super(c);
    }

    public PossibleAdducts(PrecursorIonType... possibleAdducts) {
        super(Arrays.asList(possibleAdducts));
    }

    public PossibleAdducts() {
        super();
    }

    public boolean hasPositiveCharge() {
        for (PrecursorIonType a : this)
            if (a.getCharge() > 0)
                return true;
        return false;
    }

    public boolean hasNegativeCharge() {
        for (PrecursorIonType a : this)
            if (a.getCharge() < 0)
                return true;
        return false;
    }

    public List<Ionization> getIonModes() {
        final Set<Ionization> ions = new HashSet<>();
        for (PrecursorIonType a : this)
            ions.add(a.getIonization());
        return new ArrayList<>(ions);
    }
}