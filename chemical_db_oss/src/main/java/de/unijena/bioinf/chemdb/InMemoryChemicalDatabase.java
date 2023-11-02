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

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class InMemoryChemicalDatabase implements AbstractChemicalDatabase {

    protected List<FingerprintCandidate> fingerprintCandidates;
    protected HashMap<MolecularFormula, List<FingerprintCandidate>> candidatesPerFormula;
    protected HashMap<String, FingerprintCandidate> candidatePerKey;
    protected MolecularFormula[] formulas;
    protected final String dbData;
    protected final String name;

    public InMemoryChemicalDatabase(List<FingerprintCandidate> candidates) {
        this(candidates, null, null);
    }

    public InMemoryChemicalDatabase(List<FingerprintCandidate> candidates, @Nullable String chemDBdate, @Nullable String name) {
        this.dbData = chemDBdate;
        this.name = name;
        setCandidates(candidates);
    }

    @Override
    public String getChemDbDate() throws ChemicalDatabaseException {
        return dbData;
    }

    @Override
    public long countAllFingerprints() throws ChemicalDatabaseException {
        return fingerprintCandidates.size();
    }

    @Override
    public long countAllFormulas() throws ChemicalDatabaseException {
        return formulas.length;
    }

    public void addCandidates(Iterable<FingerprintCandidate> candidates) {
        for (FingerprintCandidate fc : candidates)
            addCandidate(fc);
    }

    public void addCandidate(FingerprintCandidate fc) {
        if (candidatePerKey.put(fc.getInchiKey2D(), fc) == null) {
            final MolecularFormula formula = fc.getInchi().extractFormulaOrThrow();
            if (!candidatesPerFormula.containsKey(formula)) {
                candidatesPerFormula.put(formula, new ArrayList<FingerprintCandidate>());
                this.formulas = Arrays.copyOf(formulas, formulas.length + 1);
                this.formulas[formulas.length - 1] = formula;
                Arrays.sort(formulas);
            }
            candidatesPerFormula.get(formula).add(fc);
            fingerprintCandidates.add(fc);
        }
    }

    public void setCandidates(List<FingerprintCandidate> candidates) {
        this.candidatePerKey = new HashMap<>();
        for (FingerprintCandidate fc : candidates) {
            candidatePerKey.put(fc.getInchiKey2D(), fc);
        }
        this.fingerprintCandidates = new ArrayList<>(candidatePerKey.values());
        this.candidatesPerFormula = new HashMap<>();
        for (FingerprintCandidate fc : this.fingerprintCandidates) {
            final MolecularFormula formula = fc.getInchi().extractFormulaOrThrow();
            if (!candidatesPerFormula.containsKey(formula)) {
                candidatesPerFormula.put(formula, new ArrayList<FingerprintCandidate>());
            }
            candidatesPerFormula.get(formula).add(fc);
        }
        this.formulas = new MolecularFormula[candidatesPerFormula.keySet().size()];
        int k = 0;
        for (MolecularFormula f : candidatesPerFormula.keySet()) formulas[k++] = f;
        Arrays.sort(formulas);
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        final double exactMass = ionType.precursorMassToNeutralMass(mass);
        int index = Arrays.binarySearch(formulas, exactMass, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                double a1, a2;
                if (o1 instanceof MolecularFormula) a1 = ((MolecularFormula) o1).getMass();
                else a1 = (double) o1;
                if (o2 instanceof MolecularFormula) a2 = ((MolecularFormula) o2).getMass();
                else a2 = (double) o2;
                return Double.compare(a1, a2);
            }
        });
        if (index < 0) {
            index = (-index + 1);
        }

        final double minMass = exactMass - deviation.absoluteFor(mass);
        final double maxMass = exactMass + deviation.absoluteFor(mass);
        while (index >= 0 && formulas[index].getMass() >= minMass) --index;
        ++index;
        int endIndex = index;
        while (endIndex < formulas.length && formulas[endIndex].getMass() <= maxMass) {
            ++endIndex;
        }
        final ArrayList<FormulaCandidate> candidates = new ArrayList<>();
        for (int i = index; i < endIndex; ++i) {
            candidates.add(new FormulaCandidate(formulas[i], ionType, 0));
        }
        return candidates;
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        final List<FingerprintCandidate> fps = candidatesPerFormula.get(formula);
        if (fps == null) return Collections.emptyList();
        return new AbstractList<CompoundCandidate>() {
            @Override
            public CompoundCandidate get(int index) {
                return fps.get(index);
            }

            @Override
            public int size() {
                return fps.size();
            }
        };
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        if (!candidatesPerFormula.containsKey(formula)) return fingerprintCandidates;
        for (FingerprintCandidate fc : candidatesPerFormula.get(formula))
            fingerprintCandidates.add(fc);
        return fingerprintCandidates;
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        final ArrayList<FingerprintCandidate> candidates = new ArrayList<>();
        for (String key : inchi_keys)
            if (candidatePerKey.containsKey(key))
                candidates.add(candidatePerKey.get(key));
        return candidates;
    }

    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        final ArrayList<InChI> candidates = new ArrayList<>();
        for (String key : inchi_keys)
            if (candidatePerKey.containsKey(key))
                candidates.add(candidatePerKey.get(key).getInchi());
        return candidates;
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException {
        final ArrayList<FingerprintCandidate> candidates = new ArrayList<>();
        for (CompoundCandidate key : compounds)
            if (candidatePerKey.containsKey(key))
                candidates.add(candidatePerKey.get(key.getInchiKey2D()));
        return candidates;
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws ChemicalDatabaseException {

    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public boolean containsFormula(MolecularFormula formula) {
        return ChemDBs.containsFormula(formulas, formula);
    }
}
