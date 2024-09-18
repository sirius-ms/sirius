/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import gnu.trove.set.hash.TCustomHashSet;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This annotation defines the set of molecular formulas which have to be checked. This annotation makes only sense to
 * be assigned to each compound separately, e.g. to implement database search. As usual, the annotation is ignored
 * if there is a single molecular assigned to the compound.
 */
public class Whiteset implements Ms2ExperimentAnnotation {

    private static Set<MolecularFormula> EMPTY_SET = Collections.unmodifiableSet(new HashSet<>());
    private static Whiteset EMPTY_WHITESET = new Whiteset(EMPTY_SET,EMPTY_SET,EMPTY_SET, false, false, Whiteset.class);


    /**
     * Rather decide if you have measured or neutral formula candidates and use a method accordingly
     */
    @Deprecated
    public static Whiteset ofMeasuredOrNeutral(Set<MolecularFormula> f) {
        return new Whiteset(f,f,EMPTY_SET, Collections.singletonList(null));
    }

    public static Whiteset ofMeasuredFormulas(Collection<MolecularFormula> formulas, @NotNull Class provider) {
        return Whiteset.ofMeasuredFormulas(formulas, Collections.singletonList(provider));
    }

    protected static Whiteset ofMeasuredFormulas(Collection<MolecularFormula> formulas, @NotNull List<Class> providers) {
        return new Whiteset(EMPTY_SET,new HashSet<>(formulas), EMPTY_SET, providers);
    }

    public static Whiteset ofNeutralizedFormulas(Collection<MolecularFormula> formulas, @NotNull Class provider) {
        return new Whiteset(new HashSet<>(formulas), EMPTY_SET, EMPTY_SET, Collections.singletonList(provider));
    }

    /**
     * these are molecular formulas which contain no adduct or loss. So the adduct has to be added
     * and the loss has to be removed afterwards.
     * This is typically for molecular formulas received from database search
     */
    @Getter
    protected final Set<MolecularFormula> neutralFormulas;

    /**
     * these are formulas as they are derived from the MS. They contain the adduct (but not the ionization)
     */
    @Getter
    protected final Set<MolecularFormula> measuredFormulas;

    /**
     * these are formalas that need to be enforced in any step. These are kept in the top formula results, even if they should not.
     * These formulas may compe from spectral library search.  
     */
    protected final Set<MolecularFormula> enforcedneutralFormulas; //todo is this in addition or as replacement of "ignoreMassDeviationToResolveIonType" and "isFinalized"? I believe, we need both.

    /**
     * indicates to perform de novo molecular formula generation in addition to the whitelist formulas.
     */
    protected final boolean stillRequiresDeNovo;

    /**
     * indicates to perform bottom-up search molecular formula generation in addition to the whitelist formulas.
     */
    protected final boolean stillRequiresBottomUp;


    /**
     * ignores mass deviations when resolving the whiteset. This may be usefull testing a candidate set even if some candidatees have large mass errors.
     * Note: there is still some extremely large max mass error threshold
     */
    @Getter
    protected final boolean ignoreMassDeviationToResolveIonType;

    /**
     * this Whiteset should not be altered.
     */
    protected final boolean isFinalized;

    protected final List<Class> providers;


    public static Whiteset empty() {
        return EMPTY_WHITESET;
    }

    private Whiteset(@NotNull Set<MolecularFormula> neutralFormulas, @NotNull Set<MolecularFormula> measuredFormulas, @NotNull Set<MolecularFormula> enforcedneutralFormulas, @NotNull List<Class> providers) {
        this(neutralFormulas, measuredFormulas, enforcedneutralFormulas,false, false, false, false, providers);
    }

    private Whiteset(@NotNull Set<MolecularFormula> neutralFormulas, @NotNull Set<MolecularFormula> measuredFormulas, @NotNull Set<MolecularFormula> enforcedneutralFormulas, boolean stillRequiresDeNovo, boolean stillRequiresBottomUp, boolean ignoreMassDeviationToResolveIonType, boolean isFinalized, @NotNull List<Class> providers) {
        this.neutralFormulas = Set.copyOf(neutralFormulas);
        this.measuredFormulas = Set.copyOf(measuredFormulas);
        this.enforcedneutralFormulas = Set.copyOf(enforcedneutralFormulas);
        this.stillRequiresDeNovo = stillRequiresDeNovo;
        this.stillRequiresBottomUp = stillRequiresBottomUp;
        this.ignoreMassDeviationToResolveIonType = ignoreMassDeviationToResolveIonType;
        this.isFinalized = isFinalized;
        this.providers = new ArrayList<>(providers);
        checkConsistency(stillRequiresDeNovo, stillRequiresBottomUp, providers);
    }

    private void checkConsistency(boolean stillRequiresDeNovo, boolean stillRequiresBottomUp, @NotNull List<Class> providers) {
        if (stillRequiresDeNovo && providers.stream().anyMatch(p -> p.getSimpleName().equals("AddDeNovoDecompositionsToWhiteset"))) {
            LoggerFactory.getLogger(getClass()).warn("Whiteset flag is set to still require de novo formula decomposition, but seems to be already contained.");
        }
        if (stillRequiresBottomUp && providers.stream().anyMatch(p -> p.getSimpleName().equals("BottomUpSearch"))) {
            LoggerFactory.getLogger(getClass()).warn("Whiteset flag is set to still require bottom-up formula generation, but seems to be already contained.");
        }
    }

    private Whiteset(@NotNull Set<MolecularFormula> neutralFormulas, @NotNull Set<MolecularFormula> measuredFormulas, @NotNull Set<MolecularFormula> enforcedneutralFormulas, boolean stillRequiresDeNovo, boolean stillRequiresBottomUp,  @NotNull Class provider) {
        this(neutralFormulas, measuredFormulas, enforcedneutralFormulas, stillRequiresDeNovo, stillRequiresBottomUp, false, false, Collections.singletonList(provider));
    }

    public Set<MolecularFormula> getEnforcedNeutralFormulas() {
        return enforcedneutralFormulas;
    }

    public Set<MolecularFormula> getAllNeutralFormulasIncludingEnforced() {
        Set<MolecularFormula> all = new HashSet<>(neutralFormulas);
        all.addAll(enforcedneutralFormulas);
        return all;
    }

    protected Stream<MolecularFormula> getAllNeutralFormulasIncludingEnforcedAsStream() {
        return Stream.concat(neutralFormulas.stream(), enforcedneutralFormulas.stream());
    }

    /**
     * @return true if de novo still formula generation needs to be performed. Basically, either no denovo/bottup-up should be performed at all or
     * MS1 analysis sets this to false, so it is not re-performed in FragmentationPatternAnalysis
     */
    public boolean stillRequiresDeNovoToBeAdded() {
        return stillRequiresDeNovo;
    }

    /**
     * @return true if bottom-up search formula generation needs to be performed. Basically, either no denovo/bottup-up should be performed at all or
     * MS1 analysis sets this to false, so it is not re-performed in FragmentationPatternAnalysis
     */
    public boolean stillRequiresBottomUpBeAdded() {
        return stillRequiresBottomUp;
    }

    public boolean isFinalized() {
        return isFinalized;
    }


    public Whiteset addMeasured(@NotNull Set<MolecularFormula> measured, @NotNull Class provider) {
        if (warnIfFinalized()) return this;
        return add(EMPTY_SET,measured, EMPTY_SET, Collections.singletonList(provider));
    }

    public Whiteset addNeutral(@NotNull Set<MolecularFormula> neutral, @NotNull Class provider) {
        if (warnIfFinalized()) return this;
        return add(neutral, EMPTY_SET, EMPTY_SET, Collections.singletonList(provider));
    }

    public Whiteset addEnforedNeutral(@NotNull Set<MolecularFormula> enforcedNeutral, @NotNull Class provider) {
        if (warnIfFinalized()) return this;
        return add(EMPTY_SET, EMPTY_SET, enforcedNeutral, Collections.singletonList(provider));
    }

    /**
     * set to false directly before adding de novo candidates.
     */
    public Whiteset setRequiresDeNovo(boolean value) {
        if (value && warnIfFinalized()) return this;
        //we could set this automatically by checking the provider class. But for this we should move the AddDeNovoDecompositionsToWhiteset class to be accesible from here
        return new Whiteset(getNeutralFormulas(), getMeasuredFormulas(), getEnforcedNeutralFormulas(), value, stillRequiresBottomUp, ignoreMassDeviationToResolveIonType, isFinalized, providers);
    }

    public Whiteset setRequiresDeNovo() {
        return setRequiresDeNovo(true);
    }

    /**
     * set to false directly before adding bottom-up candidates.
     */
    public Whiteset setRequiresBottomUp(boolean value) {
        if (value && warnIfFinalized()) return this;
        //we could set this automatically by checking the provider class. But for this we should move the BottomUpSearch class to be accesible from here
        return new Whiteset(getNeutralFormulas(), getMeasuredFormulas(), getEnforcedNeutralFormulas(), stillRequiresDeNovo, value, ignoreMassDeviationToResolveIonType, isFinalized, providers);
    }

    public Whiteset setRequiresBottomUp() {
        return setRequiresBottomUp(true);
    }

    public Whiteset setIgnoreMassDeviationToResolveIonType(boolean value) {
        return new Whiteset(getNeutralFormulas(), getMeasuredFormulas(), getEnforcedNeutralFormulas(), stillRequiresDeNovo, stillRequiresBottomUp, value, isFinalized, providers);
    }

    /**
     * If true, indicates that the Whiteset should not be altered anymore - meaning noting added or filtered. This is usually used to force a single molecular formula (e.g. as specified in the input or for the FT recalibration routine)
     * @param value
     * @return
     */
    public Whiteset setFinalized(boolean value) {
        return new Whiteset(getNeutralFormulas(), getMeasuredFormulas(), getEnforcedNeutralFormulas(), stillRequiresDeNovo, stillRequiresBottomUp, ignoreMassDeviationToResolveIonType, value, providers);
    }

    //todo ElementFiler: maybe change to a "addIfNotFixed". Or rename to mergeWith to better communicate that it creates new object.
    public Whiteset add(Whiteset other) {
        if (warnIfFinalized()) return this;
        return add(other.neutralFormulas, other.measuredFormulas, other.enforcedneutralFormulas, stillRequiresDeNovo |other.stillRequiresDeNovo, stillRequiresBottomUp |other.stillRequiresBottomUp, other.providers);
    }

    /**
     * only keep formula that match the provided measuredFormulas
     * @param measuredFormulas
     * @param allowedIonTypes
     * @param provider
     * @return
     */
    public Whiteset filterByMeasuredFormulas(@NotNull Set<MolecularFormula> measuredFormulas, @NotNull Set<PrecursorIonType> allowedIonTypes, Class provider) {
        if (warnIfFinalized()) return this;
        List<Class> newProviders = new ArrayList<>(providers);
        newProviders.add(provider);
        return new Whiteset(
                getNeutralFormulas().stream().filter(mf -> allowedIonTypes.stream().anyMatch(ionType -> ionType.isApplicableToNeutralFormula(mf) && measuredFormulas.contains(ionType.neutralMoleculeToMeasuredNeutralMolecule(mf)))).collect(Collectors.toSet()),
                getMeasuredFormulas().stream().filter(mf -> measuredFormulas.contains(mf)).collect(Collectors.toSet()),
                getEnforcedNeutralFormulas(), //these are never filtered
                stillRequiresDeNovo,
                stillRequiresBottomUp,
                ignoreMassDeviationToResolveIonType,
                isFinalized,
                newProviders
        );
    }

    /**
     * only keep formula that match the provided neutralFormulas
     * @param neutralFormulas
     * @param allowedIonTypes
     * @param provider
     * @return
     */
    public Whiteset filterByNeutralFormulas(@NotNull Set<MolecularFormula> neutralFormulas, @NotNull Set<PrecursorIonType> allowedIonTypes, Class provider) {
        if (warnIfFinalized()) return this;
        List<Class> newProviders = new ArrayList<>(providers);
        newProviders.add(provider);
        return new Whiteset(
                getNeutralFormulas().stream().filter(mf -> neutralFormulas.contains(mf)).collect(Collectors.toSet()),
                getMeasuredFormulas().stream().filter(mf -> allowedIonTypes.stream().anyMatch(ionType -> ionType.isApplicableToMeasuredFormula(mf) && neutralFormulas.contains(ionType.measuredNeutralMoleculeToNeutralMolecule(mf)))).collect(Collectors.toSet()),
                getEnforcedNeutralFormulas(), //these are never filtered
                stillRequiresDeNovo,
                stillRequiresBottomUp,
                ignoreMassDeviationToResolveIonType,
                isFinalized,
                newProviders
        );
    }

    /**
     *
     * @param formulaConstraints
     * @return a whiteset including only formulas that satisfy the formulaConstraints
     */
    public Whiteset filter(FormulaConstraints formulaConstraints, @NotNull Collection<PrecursorIonType> allowedIonTypes, Class provider) {
        if (warnIfFinalized()) return this;
        //todo ElementFilter: how do the different formulaConstraints isSatisfied() with and without ionization behave - hopefully identical?. Did we only introduce these because of the ionization-adduct issue - so measured vs neutral MF? Can we make this simpler again?
        Set<MolecularFormula> newNeutralFormulas = filterNeutralFormulas(neutralFormulas, formulaConstraints);
        Set<MolecularFormula> newMeasuredFormulas = filterMeasuredFormulas(measuredFormulas, formulaConstraints, allowedIonTypes);

        List<Class> newProviders = new ArrayList<>(providers);
        newProviders.add(provider);
        return new Whiteset(newNeutralFormulas, newMeasuredFormulas, enforcedneutralFormulas, stillRequiresDeNovo, stillRequiresBottomUp, ignoreMassDeviationToResolveIonType, isFinalized, newProviders);
    }

    public static Set<MolecularFormula> filterNeutralFormulas(@NotNull Set<MolecularFormula> neutralFormulas, @NotNull FormulaConstraints formulaConstraints) {
        return neutralFormulas.stream().filter(mf -> formulaConstraints.isSatisfied(mf)).collect(Collectors.toSet());
    }

    public static Set<MolecularFormula> filterMeasuredFormulas(@NotNull Set<MolecularFormula> measuredFormulas, @NotNull FormulaConstraints formulaConstraints, @NotNull Collection<PrecursorIonType> allowedIonTypes) {
        return measuredFormulas.stream().filter(mf -> allowedIonTypes.stream().anyMatch(ionType -> formulaConstraints.isSatisfied(ionType.measuredNeutralMoleculeToNeutralMolecule(mf), ionType.getIonization()))).collect(Collectors.toSet());
    }

    protected Whiteset add(@NotNull Set<MolecularFormula> neutralFormulas, @NotNull Set<MolecularFormula> measuredFormulas, @NotNull Set<MolecularFormula> enforcedneutralFormulas, @NotNull List<Class> providers){
        if (warnIfFinalized()) return this;
        return add(neutralFormulas,measuredFormulas, enforcedneutralFormulas, stillRequiresDeNovo, stillRequiresBottomUp, providers);
    }

    protected Whiteset add(@NotNull Set<MolecularFormula> neutralFormulas, @NotNull Set<MolecularFormula> measuredFormulas, @NotNull Set<MolecularFormula> enforcedneutralFormulas, boolean stillRequiresDeNovo, boolean stillRequiresBottomUp, @NotNull List<Class> providers) {
        if (warnIfFinalized()) return this;
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
        Set<MolecularFormula> ne = enforcedneutralFormulas;
        if (ne.isEmpty()) ne = this.enforcedneutralFormulas;
        else if (this.enforcedneutralFormulas.isEmpty()) ne = enforcedneutralFormulas;
        else {
            ne = new HashSet<>(enforcedneutralFormulas);
            ne.addAll(this.enforcedneutralFormulas);
        }
        return new Whiteset(n, m, ne, stillRequiresDeNovo, stillRequiresBottomUp, ignoreMassDeviationToResolveIonType, isFinalized, Stream.concat(this.providers.stream(), providers.stream()).collect(Collectors.toList()));
    }

    /**
     * returns a new whiteset of all formulas that can be explained with the given mass and one
     * of the precursor iondetection
     */
    public List<Decomposition> resolve(double parentMass, @NotNull Deviation deviation, @NotNull Collection<PrecursorIonType> allowedPrecursorIonTypes) {
        final TCustomHashSet<Decomposition> decompositionSet = Decomposition.newDecompositionSet();
        //neutralFormulas
        for (MolecularFormula formula : neutralFormulas) {
            for (PrecursorIonType ionType : allowedPrecursorIonTypes) {
                if (ionType.isApplicableToNeutralFormula(formula) && (deviation.inErrorWindow(parentMass, ionType.neutralMassToPrecursorMass(formula.getMass())))) {
                    decompositionSet.add(new Decomposition(ionType.neutralMoleculeToMeasuredNeutralMolecule(formula), ionType.getIonization(), 0d));
                }
            }
            if (ignoreMassDeviationToResolveIonType) {
                //in principle, the above loop should find at most one ionType anyways. However, just if weird edge cases may exist, it is still performed additionally when we "forceAllCandidates"
                //we run the next function to guarantee that each formula is forced into the set with at least one adduct even if none has the correct mass deviation
                addClosestFormulaWithAbsurdlyLargeMassErrorAllowed(formula, allowedPrecursorIonTypes, parentMass, decompositionSet, true);
            }
        }
        //measuredFormulas
        for (MolecularFormula formula : measuredFormulas) {
            for (PrecursorIonType ionType : allowedPrecursorIonTypes) {
                if (ionType.isApplicableToMeasuredFormula(formula) && (deviation.inErrorWindow(parentMass, ionType.getIonization().addToMass(formula.getMass())))) {
                    decompositionSet.add(new Decomposition(formula, ionType.getIonization(), 0d));
                }
            }
            if (ignoreMassDeviationToResolveIonType) {
                //in principle, the above loop should find at most one ionType anyways. However, just if weird edge cases may exist, it is still performed additionally when we "forceAllCandidates"
                //we run the next function to guarantee that each formula is forced into the set with at least one adduct even if none has the correct mass deviation
                addClosestFormulaWithAbsurdlyLargeMassErrorAllowed(formula, allowedPrecursorIonTypes, parentMass, decompositionSet, false);
            }
        }
        //enforcedNeutralFormulas
        for (MolecularFormula formula : enforcedneutralFormulas) {
            for (PrecursorIonType ionType : allowedPrecursorIonTypes) {
                if (ionType.isApplicableToNeutralFormula(formula) && (deviation.inErrorWindow(parentMass, ionType.neutralMassToPrecursorMass(formula.getMass())))) {
                    decompositionSet.add(new Decomposition(ionType.neutralMoleculeToMeasuredNeutralMolecule(formula), ionType.getIonization(), 0d));
                }
            }
            addClosestFormulaWithAbsurdlyLargeMassErrorAllowed(formula, allowedPrecursorIonTypes, parentMass, decompositionSet, true);
        }

        return Arrays.asList(decompositionSet.toArray(new Decomposition[decompositionSet.size()]));
    }

    /**
     *
     * @param formula
     * @param allowedPrecursorIonTypes
     * @param parentMass
     * @param isNeutralFormula if true input formula is a neutral formula. Else, it is a measured formula
     */
    private void addClosestFormulaWithAbsurdlyLargeMassErrorAllowed(MolecularFormula formula, Collection<PrecursorIonType> allowedPrecursorIonTypes, double parentMass, TCustomHashSet<Decomposition> decompositionSet, boolean isNeutralFormula) {
        PrecursorIonType bestAdduct =  findBestAdduct(formula, allowedPrecursorIonTypes, parentMass, isNeutralFormula);
        if (bestAdduct == null) {
            LoggerFactory.getLogger(getClass()).warn((isNeutralFormula ? "Neutral " : "Measured ") + "molecular formula cannot be forced in the whiteset as no matching adduct could be found: "+formula);
        } else {
            MolecularFormula measuredFormula = isNeutralFormula ? bestAdduct.neutralMoleculeToMeasuredNeutralMolecule(formula) : formula;
            decompositionSet.add(new Decomposition(measuredFormula, bestAdduct.getIonization(), 0d));
        }
    }

    /**
     * in principle there should be at most one ionType matching even remotely.
     */
    private PrecursorIonType findBestAdduct(MolecularFormula formula, Collection<PrecursorIonType> allowedPrecursorIonTypes, double parentMass, boolean isNeutralFormula) {
        if (allowedPrecursorIonTypes.size() == 0) return null;
        double maxAllowedAbsError = 0.1; //this is huge. If the error is even largen, we won't return any adduct.
        double ppmToWarn = 30;
        double neutralMass = formula.getMass();
        PrecursorIonType best = null;
        double absDeviation = Double.POSITIVE_INFINITY;
        for (PrecursorIonType ionType : allowedPrecursorIonTypes) {
            if (!isApplicable(formula, ionType, isNeutralFormula)) continue;
            double otherDeviation  = getAbsMassDev(neutralMass, ionType, parentMass, isNeutralFormula);
            if (best == null || otherDeviation < absDeviation) {
                best = ionType;
                absDeviation = otherDeviation;
            }
        }
        if (absDeviation > maxAllowedAbsError) {
            //no matching adduct available
            return null;
        }
        double bestPpm = errorInPPm(absDeviation, parentMass);
        if (bestPpm > ppmToWarn) {
            LoggerFactory.getLogger(getClass()).warn("The " + (isNeutralFormula ? "neutral " : "measured ") + " molecular formula forced into the whiteset has a extremely large mass deviation: " + formula + " with adduct " + best + "(ppm " + bestPpm + ").");
        }
        return best;
    }

    private boolean isApplicable(MolecularFormula formula, PrecursorIonType ionType, boolean isNeutralFormula) {
        if (isNeutralFormula) {
            return ionType.isApplicableToNeutralFormula(formula);
        } else {
            return ionType.isApplicableToMeasuredFormula(formula);
        }
    }
    private double getAbsMassDev(double neutralMass, PrecursorIonType ionType, double parentMass, boolean isNeutralFormula) {
        if (isNeutralFormula) {
            return Math.abs(ionType.neutralMassToPrecursorMass(neutralMass) - parentMass);
        } else {
            return Math.abs(ionType.getIonization().addToMass(neutralMass) - parentMass);
        }
    }

    public double errorInPPm(double error, double mass) {
        return  error / mass * 1e6;
    }

    /**
     *
     * @param measuredFormula
     * @param ionType
     * @return true if measuredFormula is either directly contained in this {@link Whiteset} or if the neutral formula based on ionType is contained.
     */
    public boolean containsMeasuredFormula(@NotNull MolecularFormula measuredFormula, @NotNull PrecursorIonType ionType) {
        if (measuredFormulas.contains(measuredFormula)) return true;
        else {
            if (!ionType.isApplicableToMeasuredFormula(measuredFormula)) return false;
            MolecularFormula neutralFormula = ionType.measuredNeutralMoleculeToNeutralMolecule(measuredFormula);
            return neutralFormulas.contains(neutralFormula) || enforcedneutralFormulas.contains(neutralFormula);
        }
    }

    /**
     * @param possiblePrecursorIonTypes these are usually the {@link de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts}
     * @return a whiteset with only measured formulas by converting neutral formulas based on {@link PrecursorIonType}s
     */
    public Whiteset asMeasuredFormulas(@NotNull Collection<PrecursorIonType> possiblePrecursorIonTypes) {
        Set<MolecularFormula> allMeasured = new HashSet<>(measuredFormulas);
        possiblePrecursorIonTypes.stream().forEach(ionType -> {
            getAllNeutralFormulasIncludingEnforcedAsStream()
                    .filter(mf -> ionType.isApplicableToNeutralFormula(mf))
                    .map(mf -> ionType.neutralMoleculeToMeasuredNeutralMolecule(mf))
                    .forEach(mf -> allMeasured.add(mf));
        });
        return Whiteset.ofMeasuredFormulas(allMeasured, providers);
    }

    /**
     * @param possiblePrecursorIonTypes these are usually the {@link de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts}
     * @return a whiteset with only measured formulas by converting neutral formulas based on {@link PrecursorIonType}s
     */
    public Set<MolecularFormula> getNeutralEnforcedAsMeasuredFormulasSet(@NotNull Collection<PrecursorIonType> possiblePrecursorIonTypes) {
        Set<MolecularFormula> formulaSet = new HashSet<>();
        possiblePrecursorIonTypes.stream().forEach(ionType -> {
            enforcedneutralFormulas.stream()
                    .filter(mf -> ionType.isApplicableToNeutralFormula(mf))
                    .map(mf -> ionType.neutralMoleculeToMeasuredNeutralMolecule(mf))
                    .forEach(mf -> formulaSet.add(mf));
        });
        return formulaSet;
    }


    /*
     * Bad hack.
     * Basically, the >formula field in .ms files expect neutral formulas. However, as sometimes strange stuff happens
     * (like adducts or intrinsical charged affect the formula), we re-apply the ionization to all formulas such that
     * the correct formula is definitely in the whiteset.

    public Whiteset applyIonizationBothWays(PrecursorIonType ionType) {
        Set<MolecularFormula> n = new HashSet<>(neutralFormulas);
        Set<MolecularFormula> m = new HashSet<>(measuredFormulas);
        for (MolecularFormula f : measuredFormulas) {
            n.add(ionType.precursorIonToNeutralMolecule(f));
        }
        for (MolecularFormula f : neutralFormulas) {
            n.add(ionType.neutralMoleculeToPrecursorIon(f));
        }
        return new Whiteset(n,m);
    }
     */

    private boolean warnIfFinalized() {
        if (isFinalized) {
            LoggerFactory.getLogger(getClass()).warn("The formula whiteset has been finalized. However, a method tries to alter it. Keep it unchanged.");
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return measuredFormulas.isEmpty() && neutralFormulas.isEmpty();
    }

    public boolean notEmpty() {
        return !isEmpty();
    }

    @Override
    public String toString() {
        return "Whiteset{" +
                "neutralFormulas=" + neutralFormulas +
                ", measuredFormulas=" + measuredFormulas +
                '}';
    }
}
