package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.*;

/**
 * Can be attached to a Ms2Experiment or ProcessedInput. If PrecursorIonType is unknown, CSI:FingerID will use this
 * object and for all different adducts.
 */
public final class PossibleAdducts implements Iterable<PrecursorIonType> {

    protected final LinkedHashSet<PrecursorIonType> adducts;

    public PossibleAdducts(Collection<? extends PrecursorIonType> c) {
        this.adducts = new LinkedHashSet<>(c);
    }

    public PossibleAdducts(PrecursorIonType... possibleAdducts) {
        this(Arrays.asList(possibleAdducts));
    }

    public PossibleAdducts(PossibleAdducts pa) {
        this(pa.adducts);
    }

    public PossibleAdducts() {
        this.adducts = new LinkedHashSet<>();
    }

    public boolean hasPositiveCharge() {
        for (PrecursorIonType a : adducts)
            if (a.getCharge() > 0)
                return true;
        return false;
    }

    public boolean hasNegativeCharge() {
        for (PrecursorIonType a : adducts)
            if (a.getCharge() < 0)
                return true;
        return false;
    }

    public List<Ionization> getIonModes() {
        final Set<Ionization> ions = new HashSet<>();
        for (PrecursorIonType a : adducts)
            ions.add(a.getIonization());
        return new ArrayList<>(ions);
    }

    public PossibleIonModes merge(PossibleIonModes ionModes) {
        final PossibleIonModes copy = new PossibleIonModes(ionModes);
        for (PrecursorIonType ionType : adducts)
            copy.add(ionType.getIonization());
        return copy;
    }

    public void addAdduct(String adductName) {
        addAdduct(PrecursorIonType.getPrecursorIonType(adductName));
    }

    public void addAdduct(PrecursorIonType adduct) {
        adducts.add(adduct);
    }

    @Override
    public Iterator<PrecursorIonType> iterator() {
        return adducts.iterator();
    }
}