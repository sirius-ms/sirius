package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import gnu.trove.set.hash.TCustomHashSet;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This annotation defines the set of molecular formulas which have to be checked. This annotation makes only sense to
 * be assigned to each compound separately, e.g. to implement database search. As usual, the annotation is ignored
 * if there is a single molecular assigned to the compound.
 */
public class Whiteset implements Ms2ExperimentAnnotation {

    public static Whiteset of(MolecularFormula... formulas) {
        return new Whiteset(new HashSet<>(Arrays.asList(formulas)));
    }

    public static Whiteset of(Collection<MolecularFormula> formulas) {
        return new Whiteset(new HashSet<>(formulas));
    }

    public static Whiteset of(List<String> formulas) {
        return of(formulas.stream().map(s -> {
            try {
                return MolecularFormula.parse(s);
            } catch (UnknownElementException e) {
                LoggerFactory.getLogger(Whiteset.class).warn("Could not par Formula String: " + s + " Skipping this Entry!");
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet()));
    }

    protected final Set<MolecularFormula> formulas;

    private Whiteset(Set<MolecularFormula> formulas) {
        this.formulas = formulas;
    }

    public Set<MolecularFormula> getFormulas() {
        return formulas;
    }

    /**
     * returns a new whiteset of all formulas that can be explained with the given mass and one
     * of the precursor iondetection
     */
    public List<Decomposition> resolve(double parentMass, Deviation deviation, Collection<PrecursorIonType> allowedPrecursorIonTypes) {

        final TCustomHashSet<Decomposition> decompositionSet = Decomposition.newDecompositionSet();
        eachFormula:
        for (MolecularFormula formula : formulas) {
            for (PrecursorIonType ionType : allowedPrecursorIonTypes) {
                if (deviation.inErrorWindow(parentMass, ionType.neutralMassToPrecursorMass(formula.getMass()))) {
                    decompositionSet.add(new Decomposition(ionType.neutralMoleculeToMeasuredNeutralMolecule(formula), ionType.getIonization(), 0d));
                    continue eachFormula;
                }
            }
        }
        return Arrays.asList(decompositionSet.toArray(new Decomposition[decompositionSet.size()]));
    }

    public Whiteset withPrecursor(PrecursorIonType ionType) {
        if (ionType.hasNeitherAdductNorInsource()) return this;
        return new Whiteset(formulas.stream().map(ionType::measuredNeutralMoleculeToNeutralMolecule).collect(Collectors.toSet()));
    }

    public Whiteset union(Whiteset set) {
        final Set<MolecularFormula> union = new HashSet<>(this.formulas);
        union.addAll(set.formulas);
        return new Whiteset(union);
    }

    public Whiteset intersection(Whiteset set) {
        final Set<MolecularFormula> intersection = new HashSet<>(this.formulas);
        intersection.retainAll(set.formulas);
        return new Whiteset(intersection);
    }

    public Whiteset intersection(MolecularFormula[] formulas) {
        final Set<MolecularFormula> intersection = new HashSet<>(this.formulas);
        intersection.retainAll(Arrays.asList(formulas));
        return new Whiteset(intersection);
    }
}
