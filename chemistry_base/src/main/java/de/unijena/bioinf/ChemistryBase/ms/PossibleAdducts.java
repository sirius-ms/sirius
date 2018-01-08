package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.*;

/**
 * Can be attached to a Ms2Experiment or ProcessedInput. If PrecursorIonType is unknown, CSI:FingerID will use this
 * object and for all different adducts.
 */
public final class PossibleAdducts implements Iterable<PrecursorIonType> {

    protected final ArrayList<PrecursorIonType> ionTypes;

    public PossibleAdducts(Collection<? extends PrecursorIonType> c) {
        this.ionTypes = new ArrayList<>(c);
    }

    public PossibleAdducts(PrecursorIonType... possibleAdducts) {
        this(Arrays.asList(possibleAdducts));
    }

    public PossibleAdducts(PossibleAdducts pa) {
        this(pa.ionTypes);
    }

    public PossibleAdducts() {
        this.ionTypes = new ArrayList<>();
    }

    public List<PrecursorIonType> getAdducts() {
        return ionTypes;
    }

    public boolean hasPositiveCharge() {
        for (PrecursorIonType a : ionTypes)
            if (a.getCharge() > 0)
                return true;
        return false;
    }

    public boolean hasNegativeCharge() {
        for (PrecursorIonType a : ionTypes)
            if (a.getCharge() < 0)
                return true;
        return false;
    }

    public List<Ionization> getIonModes() {
        final Set<Ionization> ions = new HashSet<>();
        for (PrecursorIonType a : ionTypes)
            ions.add(a.getIonization());
        return new ArrayList<>(ions);
    }

    public PossibleIonModes merge(PossibleIonModes ionModes) {
        final PossibleIonModes copy = new PossibleIonModes(ionModes);
        for (PrecursorIonType ionType : ionTypes)
            copy.add(ionType.getIonization());
        return copy;
    }

    @Override
    public Iterator<PrecursorIonType> iterator() {
        return ionTypes.iterator();
    }
}