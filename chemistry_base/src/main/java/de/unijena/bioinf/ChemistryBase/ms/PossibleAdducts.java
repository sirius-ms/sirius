package de.unijena.bioinf.ChemistryBase.ms;

import com.google.common.collect.Sets;
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
        this(new LinkedHashSet<>());
    }

    private PossibleAdducts(LinkedHashSet<PrecursorIonType> adducts) {
        this.adducts = adducts;
    }

    public Set<PrecursorIonType> getAdducts() {
        return Collections.unmodifiableSet(adducts);
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

    @Override
    public String toString() {
        return adducts.toString();
    }

    public static PossibleAdducts union(PossibleAdducts p1, Set<PrecursorIonType> p2) {
        return new PossibleAdducts(Sets.union(p1.adducts, p2));
    }

    public static PossibleAdducts union(PossibleAdducts p1, PossibleAdducts p2) {
        return new PossibleAdducts(Sets.union(p1.adducts, p2.adducts));
    }

    public static PossibleAdducts intersection(PossibleAdducts p1, Set<PrecursorIonType> p2) {
        return new PossibleAdducts(Sets.intersection(p1.adducts, p2));
    }

    public static PossibleAdducts intersection(PossibleAdducts p1, PossibleAdducts p2) {
        return new PossibleAdducts(Sets.intersection(p1.adducts, p2.adducts));
    }


}