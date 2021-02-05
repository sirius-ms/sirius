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

package de.unijena.bioinf.chemdb;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.babelms.CloseableIterator;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

public abstract class AbstractBlobBasedDatabase implements AbstractChemicalDatabase {
    protected final static String[] SUPPORTED_FORMATS = new String[]{".CSV", ".CSV.GZ", ".JSON", ".JSON.GZ"};

    protected final String name;
    protected String format; // csv or json or csv.gz or json.gz
    protected CompoundReader reader;
    protected MolecularFormula[] formulas;
    protected final TObjectLongMap<MolecularFormula> formulaFlags = new TObjectLongHashMap<>();
    protected FingerprintVersion version;
    protected boolean compressed = false;


    public AbstractBlobBasedDatabase(FingerprintVersion version, String name) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    protected abstract void refresh() throws IOException;


    @NotNull
    public Optional<Reader> getCompoundReader(@NotNull MolecularFormula formula) throws IOException {
        return getReader(formula.toString());
    }

    @NotNull
    public Optional<Reader> getReader(@NotNull String name) throws IOException {
        return getStream(name).map(inputStream -> new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    /**
     * Returns stream for the requested filename and handles decompression if needed
     *
     * @param name resource name
     * @return Stream of the resource
     * @throws IOException if IO goes wrong
     */
    @NotNull
    public Optional<InputStream> getStream(@NotNull String name) throws IOException {
        Optional<InputStream> opt = getRawStream(name);
        if (opt.isEmpty())
            return Optional.empty();
        return compressed ? Optional.of(new GZIPInputStream(opt.get())) : opt;
    }

    /**
     * Returns stream for the filename without handling decompression
     *
     * @param name resource name
     * @return Optional of Stream of the resource
     */
    @NotNull
    public abstract Optional<InputStream> getRawStream(@NotNull String name) throws IOException;


    public boolean containsFormula(MolecularFormula formula) {
        return ChemDBs.containsFormula(formulas, formula);
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(final double ionMass, Deviation
            deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {

        final double mass = ionType.precursorMassToNeutralMass(ionMass);

        final int searchP = Arrays.binarySearch(formulas, mass - deviation.absoluteFor(ionMass), (o1, o2) -> {
            double mzL = (o1 instanceof MolecularFormula ? ((MolecularFormula) o1).getMass() : (Double) o1);
            double mzR = (o2 instanceof MolecularFormula ? ((MolecularFormula) o2).getMass() : (Double) o2);
            return Double.compare(mzL, mzR);
        });

        int insertionPoint;
        if (searchP >= 0) {
            insertionPoint = searchP;
        } else {
            insertionPoint = -searchP - 1;
        }

        final double max = mass + deviation.absoluteFor(ionMass);
        ArrayList<FormulaCandidate> candidates = new ArrayList<>();
        while (insertionPoint < formulas.length && formulas[insertionPoint].getMass() <= max) {
            final MolecularFormula f = formulas[insertionPoint++];
            candidates.add(new FormulaCandidate(f, ionType, formulaFlags.get(f)));
        }

        return candidates;
    }


    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        final ArrayList<CompoundCandidate> candidates = new ArrayList<>();

        try (final Reader blobReader = getCompoundReader(formula).orElse(null)) {
            if (blobReader != null) {
                try (final CloseableIterator<CompoundCandidate> iter = reader.readCompounds(blobReader)) {
                    iter.forEachRemaining(candidates::add);
                }
            }
            return candidates;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<FingerprintCandidate> lookupStructuresAndFingerprintsByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        final ArrayList<FingerprintCandidate> candidates = new ArrayList<>();
        return lookupStructuresAndFingerprintsByFormula(formula, candidates);
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        try (final Reader blobReader = getCompoundReader(formula).orElse(null)) {
            if (blobReader != null) {
                try (final CloseableIterator<FingerprintCandidate> iter = reader.readFingerprints(version, blobReader)) {
                    iter.forEachRemaining(fingerprintCandidates::add);
                }
            }
            return fingerprintCandidates;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FingerprintCandidate> lookupManyFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }


    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException {
        final ArrayList<FingerprintCandidate> candidates = new ArrayList<>();
        final HashMap<String, CompoundCandidate> innerMap = new HashMap<>();
        final Multimap<MolecularFormula, CompoundCandidate> formulas2Candidates = ArrayListMultimap.create();
        for (CompoundCandidate c : compounds) {
            final MolecularFormula f = c.getInchi().extractFormulaOrThrow();
            formulas2Candidates.put(f, c);
            innerMap.put(c.getInchiKey2D(), c);
        }

        for (Map.Entry<MolecularFormula, Collection<CompoundCandidate>> entry : formulas2Candidates.asMap().entrySet()) {
            final MolecularFormula f = entry.getKey();
            final Collection<FingerprintCandidate> pseudoQueue = new AbstractCollection<>() {

                @Override
                public boolean add(FingerprintCandidate fingerprintCandidate) {
                    final CompoundCandidate c = innerMap.get(fingerprintCandidate.getInchiKey2D());
                    if (c != null) {
                        candidates.add(new FingerprintCandidate(c, fingerprintCandidate.fingerprint));
                    }
                    return true;
                }

                @Override
                public Iterator<FingerprintCandidate> iterator() {
                    return null;
                }

                @Override
                public int size() {
                    return 0;
                }
            };
            lookupStructuresAndFingerprintsByFormula(f, pseudoQueue);
        }
        return candidates;
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws ChemicalDatabaseException {
        // compounds are already annotated
        return;
    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {

    }

}