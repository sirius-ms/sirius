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

    private static Set<MolecularFormula> EMPTY_SET = Collections.unmodifiableSet(new HashSet<>());
    private static Whiteset EMPTY_WHITESET = new Whiteset(EMPTY_SET,EMPTY_SET);

    public static Whiteset ofMeasuredOrNeutral(Set<MolecularFormula> f) {
        return new Whiteset(f,f);
    }

    // hm
    public static Whiteset of(List<String> formulas) {
        final Set<MolecularFormula> fs = formulas.stream().map(s -> {
            try {
                return MolecularFormula.parse(s);
            } catch (UnknownElementException e) {
                LoggerFactory.getLogger(Whiteset.class).warn("Could not par Formula String: " + s + " Skipping this Entry!");
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet());
        return new Whiteset(fs,fs);
    }

    public static Whiteset ofMeasuredFormulas(Collection<MolecularFormula> formulas) {
        return new Whiteset(EMPTY_SET,new HashSet<>(formulas));
    }

    public static Whiteset ofNeutralizedFormulas(Collection<MolecularFormula> formulas) {
        return new Whiteset(new HashSet<>(formulas), EMPTY_SET);
    }

    // these are molecular formulas which contain no adduct or loss. So the adduct has to be added
    // and the loss has to be removed afterwards.
    // This is typically for molecular formulas received from database search
    protected final Set<MolecularFormula> neutralFormulas;

    // these are formulas as they are derived from the MS. They contain the adduct (but not the ionization)
    protected final Set<MolecularFormula> measuredFormulas;


    public static Whiteset empty() {
        return EMPTY_WHITESET;
    }

    private Whiteset(Set<MolecularFormula> neutralFormulas, Set<MolecularFormula> measuredFormulas) {
        this.neutralFormulas = Set.copyOf(neutralFormulas);
        this.measuredFormulas = Set.copyOf(measuredFormulas);
    }

    public Set<MolecularFormula> getNeutralFormulas() {
        return neutralFormulas;
    }

    public Set<MolecularFormula> getMeasuredFormulas() {
        return measuredFormulas;
    }

    public Whiteset addMeasured(Set<MolecularFormula> measured) {
        return add(EMPTY_SET,measured);
    }

    public Whiteset addNeutral(Set<MolecularFormula> neutral) {
        return add(neutral, EMPTY_SET);
    }

    public Whiteset add(Whiteset other) {
        return add(other.neutralFormulas, other.measuredFormulas);
    }

    public Whiteset add(Set<MolecularFormula> neutralFormulas, Set<MolecularFormula> measuredFormulas) {
        Set<MolecularFormula> n = neutralFormulas;
        if (n.isEmpty()) n = this.neutralFormulas;
        else if (this.neutralFormulas.isEmpty()) n = neutralFormulas;
        else {
            n = new HashSet<>(neutralFormulas);
            n.addAll(this.neutralFormulas);
        }
        Set<MolecularFormula> m = measuredFormulas;
        if (m.isEmpty()) m = this.measuredFormulas;
        else if (this.measuredFormulas.isEmpty()) m = measuredFormulas;
        else {
            m = new HashSet<>(measuredFormulas);
            m.addAll(this.measuredFormulas);
        }
        return new Whiteset(n,m);
    }

    /**
     * returns a new whiteset of all formulas that can be explained with the given mass and one
     * of the precursor iondetection
     */
    public List<Decomposition> resolve(double parentMass, Deviation deviation, Collection<PrecursorIonType> allowedPrecursorIonTypes) {

        final TCustomHashSet<Decomposition> decompositionSet = Decomposition.newDecompositionSet();
        eachFormula:
        for (MolecularFormula formula : neutralFormulas) {
            for (PrecursorIonType ionType : allowedPrecursorIonTypes) {
                if (deviation.inErrorWindow(parentMass, ionType.neutralMassToPrecursorMass(formula.getMass()))) {
                    decompositionSet.add(new Decomposition(ionType.neutralMoleculeToMeasuredNeutralMolecule(formula), ionType.getIonization(), 0d));
                    continue eachFormula;
                }
            }
        }
        eachFormula:
        for (MolecularFormula formula : measuredFormulas) {
            for (PrecursorIonType ionType : allowedPrecursorIonTypes) {
                if (deviation.inErrorWindow(parentMass, ionType.getIonization().addToMass(formula.getMass()))) {
                    decompositionSet.add(new Decomposition(formula, ionType.getIonization(), 0d));
                    continue eachFormula;
                }
            }
        }
        return Arrays.asList(decompositionSet.toArray(new Decomposition[decompositionSet.size()]));
    }

    public boolean isEmpty() {
        return measuredFormulas.isEmpty() && neutralFormulas.isEmpty();
    }

}
