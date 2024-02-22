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

    private CandidateFormulas(@NotNull Set<MolecularFormula> neutralformulas, Class provider) {
        //if non-private contructor is needed, add check for only neutral formulas
        //if we actually want to allow measured formulas, this should be done explicitly, not implicitly
        this.providersToNeutralFormulas = new HashMap<>();
        this.providersToNeutralFormulas.put(provider, neutralformulas);
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
    }

    public Whiteset toWhiteSet() {
        return Whiteset.ofNeutralizedFormulas(collectFormulasFromAllProviders(), CandidateFormulas.class);
    }

    public Whiteset getWhitesetOfInputFileCandidates() {
        return getCandidatesFromInputFile().map(set -> Whiteset.ofNeutralizedFormulas(set, CandidateFormulas.class)).orElse(null);
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
     * For now only the JenaMsParser can provide a candidate list.
     * IF NEW INPUT FORMATS SUPPORT CANDIDATE LISTS, THIS NEEDS BE UPDATED
     * @return true if candidate formulas were provided by JenaMsParser
     */
    public boolean hasInputFileProvider() {
        return getCandidatesFromInputFile().isPresent(); //todo ElementFilter: test
    }

    private Optional<Set<MolecularFormula>> getCandidatesFromInputFile() {
        //currently, only our .ms format can specify candidate lists
        return providersToNeutralFormulas.entrySet().stream().filter((entry -> entry.getKey().getSimpleName().equals("JenaMsParser"))).map(Map.Entry::getValue).findFirst();
    }

    /**
     *
     * @param formulas
     * @param provider the class that provides the list of formulas
     * @return
     */
    public static CandidateFormulas of(List<String> formulas, Class provider) {
        final Set<MolecularFormula> fs = formulas.stream().map(s -> {
            try {
                return MolecularFormula.parse(s);
            } catch (UnknownElementException e) {
                LoggerFactory.getLogger(CandidateFormulas.class).warn("Could not par Formula String: " + s + " Skipping this Entry!");
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet());
        return new CandidateFormulas(fs, provider);
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
