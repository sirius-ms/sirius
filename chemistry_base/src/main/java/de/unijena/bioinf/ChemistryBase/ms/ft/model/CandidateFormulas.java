/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This configuration holds a set of neutral formulas to be used as candidates for SIRIUS.
 * The formulas may be provided by the user, from a database or from the input file.
 * Note: This set might be merged with other sources such as ElGordo predicted lipid.
 */
public class CandidateFormulas implements Ms2ExperimentAnnotation {

    /**
     * maps from the provider class to the set of candidate formulas
     */
    @NotNull
    protected final Map<Class, Set<MolecularFormula>> providersToNeutralFormulas;

    @NotNull
    protected final Set<Class> inputFileProviders;

    @NotNull
    protected final Set<Class> spectralLibraryMatchProviders;

    private CandidateFormulas(@NotNull Set<MolecularFormula> neutralformulas, Class provider) {
        //if non-private contructor is needed, add check for only neutral formulas
        //if we actually want to allow measured formulas, this should be done explicitly, not implicitly
        this.providersToNeutralFormulas = new HashMap<>();
        this.providersToNeutralFormulas.put(provider, neutralformulas);
        this.inputFileProviders = new HashSet<>();
        this.spectralLibraryMatchProviders = new HashSet<>();

    }

    private CandidateFormulas(@NotNull Map<Class, Set<MolecularFormula>> providersToNeutralFormulas, @NotNull Set<Class> inputFileProviders, @NotNull Set<Class> spectralLibraryMatchProviders) {
        //if non-private contructor is needed, add check for only neutral formulas
        //if we actually want to allow measured formulas, this should be done explicitly, not implicitly
        this.providersToNeutralFormulas = providersToNeutralFormulas;
        this.inputFileProviders = inputFileProviders;
        this.spectralLibraryMatchProviders = spectralLibraryMatchProviders;
    }

    /**
     * @param value Set of Molecular Formulas to be used as candidates for molecular formula estimation with SIRIUS
     */
    @DefaultInstanceProvider
    public static CandidateFormulas newInstance(@DefaultProperty List<String> value) {
        return CandidateFormulas.of(value, DefaultInstanceProvider.class);
    }

    public void addAndMerge(CandidateFormulas otherFormulas) {
        for (Class provider : otherFormulas.providersToNeutralFormulas.keySet()) {
            Set<MolecularFormula> neutralFormulas = providersToNeutralFormulas.computeIfAbsent(provider, (x) -> new HashSet<>());
            neutralFormulas.addAll(otherFormulas.providersToNeutralFormulas.get(provider));
        }
        inputFileProviders.addAll(otherFormulas.inputFileProviders);
        spectralLibraryMatchProviders.addAll(spectralLibraryMatchProviders);
    }

    public void addAndMerge(Set<MolecularFormula> neutralFormulas, Class provider) {
        Set<MolecularFormula> formulas = providersToNeutralFormulas.computeIfAbsent(provider, (x) -> new HashSet<>());
        formulas.addAll(neutralFormulas);
    }

    public void addAndMergeInputFileFormulas(Set<MolecularFormula> neutralFormulas, Class provider) {
        Set<MolecularFormula> formulas = providersToNeutralFormulas.computeIfAbsent(provider, (x) -> new HashSet<>());
        formulas.addAll(neutralFormulas);
        inputFileProviders.add(provider);
    }

    public void addAndMergeSpectralLibrarySearchFormulas(Set<MolecularFormula> neutralFormulas, Class provider) {
        Set<MolecularFormula> formulas = providersToNeutralFormulas.computeIfAbsent(provider, (x) -> new HashSet<>());
        formulas.addAll(neutralFormulas);
        spectralLibraryMatchProviders.add(provider);
    }

    public Whiteset toWhiteSet() {
        return Whiteset.ofNeutralizedFormulas(collectFormulasFromAllProviders(), CandidateFormulas.class);
    }

    public Whiteset getWhitesetOfInputFileCandidates() {
        return Whiteset.ofNeutralizedFormulas(getCandidatesFromInputFile(), CandidateFormulas.class);
    }

    public Whiteset getWhitesetOfSpectralLibaryMatches() {
        return Whiteset.ofNeutralizedFormulas(getCandidatesFromSpectralLibraryMatches(), CandidateFormulas.class);
    }

    public Set<MolecularFormula> getFormulas() {
        return Collections.unmodifiableSet(collectFormulasFromAllProviders());
    }

    private Set<MolecularFormula> collectFormulasFromAllProviders() {
        return providersToNeutralFormulas.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
    }

    public int numberOfFormulas() {
        return providersToNeutralFormulas.values().stream().mapToInt(Set::size).sum();
    }

    public boolean notEmpty() {
        return numberOfFormulas()>0;
    }

    /**
     * @return true if candidate formulas were provided by inputFileProviders such as JenaMsParser
     */
    public boolean hasInputFileProvider() {
        return inputFileProviders.size()>0;
    }

    private Set<MolecularFormula> getCandidatesFromInputFile() {
        return providersToNeutralFormulas.entrySet().stream().filter((entry -> inputFileProviders.contains(entry.getKey()))).flatMap(e -> e.getValue().stream()).collect(Collectors.toSet());
    }

    public boolean hasSpectralLibraryMatchProvidersProvider() {
        return spectralLibraryMatchProviders.size()>0;
    }

    public Set<MolecularFormula> getCandidatesFromSpectralLibraryMatches() {
        return providersToNeutralFormulas.entrySet().stream().filter((entry -> spectralLibraryMatchProviders.contains(entry.getKey()))).flatMap(e -> e.getValue().stream()).collect(Collectors.toSet());
    }

    /**
     *
     * @param formulas
     * @param provider the class that provides the list of formulas
     * @return
     */
    public static CandidateFormulas of(List<String> formulas, Class provider) {
        return of(formulas, provider, false, false);
    }

    public static CandidateFormulas of(List<String> formulas, Class provider, boolean isInputFileProvider,  boolean isSpectralLibrarySearchProvider) {
        final Set<MolecularFormula> fs = formulas.stream().map(s -> {
            try {
                return MolecularFormula.parse(s);
            } catch (UnknownElementException e) {
                LoggerFactory.getLogger(CandidateFormulas.class).warn("Could not par Formula String: " + s + " Skipping this Entry!");
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet());
        CandidateFormulas cf =  new CandidateFormulas(fs, provider);
        if (isInputFileProvider) cf.inputFileProviders.add(provider);
        if (isSpectralLibrarySearchProvider) cf.spectralLibraryMatchProviders.add(provider);
        return cf;
    }

    /**
     *
     * @param formulas
     * @param provider the class that provides the set of formulas
     * @return
     */
    public static CandidateFormulas fromSet(Set<MolecularFormula> formulas, Class provider) {
        return new CandidateFormulas(formulas, provider);
    }
}
