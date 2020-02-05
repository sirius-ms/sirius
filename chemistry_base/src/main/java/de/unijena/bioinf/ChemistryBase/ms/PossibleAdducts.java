package de.unijena.bioinf.ChemistryBase.ms;

import com.google.common.collect.Sets;
import de.unijena.bioinf.ChemistryBase.chem.IonMode;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Can be attached to a Ms2Experiment or ProcessedInput. If PrecursorIonType is unknown, CSI:FingerID will use this
 * object and for all different adducts.
 */
public final class PossibleAdducts implements Iterable<PrecursorIonType>, Ms2ExperimentAnnotation {

    protected final LinkedHashSet<PrecursorIonType> value;

    public PossibleAdducts(Collection<? extends PrecursorIonType> c) {
        this.value = new LinkedHashSet<>(c);
    }

    public PossibleAdducts(PrecursorIonType... possibleAdducts) {
        this(Arrays.asList(possibleAdducts));
    }

    public PossibleAdducts(PossibleAdducts pa) {
        this(pa.value);
    }

    public PossibleAdducts() {
        this(new LinkedHashSet<>());
    }

    private PossibleAdducts(LinkedHashSet<PrecursorIonType> adducts) {
        this.value = adducts;
    }

    public Set<PrecursorIonType> getAdducts() {
        return Collections.unmodifiableSet(value);
    }

    public Set<PrecursorIonType> getAdducts(Ionization ionMode) {
        return value.stream().filter((a) -> a.getIonization().equals(ionMode)).collect(Collectors.toSet());
    }

    public boolean hasPositiveCharge() {
        for (PrecursorIonType a : value)
            if (a.getCharge() > 0)
                return true;
        return false;
    }

    public boolean hasNegativeCharge() {
        for (PrecursorIonType a : value)
            if (a.getCharge() < 0)
                return true;
        return false;
    }

    public void keepOnlyPositive() {
        value.removeIf(it -> it.getCharge() > 1);
    }

    public void keepOnlyNegative() {
        value.removeIf(it -> it.getCharge() < 1);
    }

    public void keepOnly(final int charge) {
        value.removeIf(it -> it.getCharge() != charge);
    }

    public Set<IonMode> getIonModes() {
        final Set<IonMode> ions = new HashSet<>();
        for (PrecursorIonType a : value) {
            final Ionization ion = a.getIonization();
            if (ion instanceof IonMode) {
                ions.add((IonMode) ion);
            }
        }
        return ions;
    }

    public void addAdduct(String adductName) {
        addAdduct(PrecursorIonType.getPrecursorIonType(adductName));
    }

    public void addAdduct(PrecursorIonType adduct) {
        value.add(adduct);
    }

    public void addAdducts(Collection<PrecursorIonType> adductsToAdd) {
        value.addAll(adductsToAdd);
    }

    public void addAdducts(PrecursorIonType... adductsToAdd) {
        value.addAll(Arrays.asList(adductsToAdd));
    }

    public void addAdducts(PossibleAdducts adductsToAdd) {
        value.addAll(adductsToAdd.value);
    }

    @Override
    public Iterator<PrecursorIonType> iterator() {
        return value.iterator();
    }

    @Override
    public String toString() {
        if (value.isEmpty())
            return ",";
        return value.toString();
    }

    public static PossibleAdducts union(PossibleAdducts p1, Set<PrecursorIonType> p2) {
        return new PossibleAdducts(Sets.union(p1.value, p2));
    }

    public static PossibleAdducts union(PossibleAdducts p1, PossibleAdducts p2) {
        return new PossibleAdducts(Sets.union(p1.value, p2.value));
    }

    public static PossibleAdducts intersection(PossibleAdducts p1, Set<PrecursorIonType> p2) {
        return new PossibleAdducts(Sets.intersection(p1.value, p2));
    }

    public static PossibleAdducts intersection(PossibleAdducts p1, PossibleAdducts p2) {
        return new PossibleAdducts(Sets.intersection(p1.value, p2.value));
    }

    public int size() {
        return value.size();
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    public boolean contains(PrecursorIonType o) {
        return value.contains(o);
    }


    //if the list are a single PrecursorIonType we can convert it into one
    public boolean isPrecursorIonType() {
        return size() == 1;
    }

    public PrecursorIonType asPrecursorIonType() {
        if (isPrecursorIonType()) return value.iterator().next();
        return null;
    }
}