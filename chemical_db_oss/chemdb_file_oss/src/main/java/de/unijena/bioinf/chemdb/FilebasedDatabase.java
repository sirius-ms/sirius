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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.babelms.CloseableIterator;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.GZIPInputStream;

/*
    A file based database consists of a directory of files (either .csv or .json), each file contains compounds from the
    same molecular formula. The filenames consists of the molecular formula strings.
 */
public class FilebasedDatabase extends AbstractChemicalDatabase {

    private final String name;
    private File dir;
    private String format; // csv or json or csv.gz or json.gz
    private CompoundReader reader;
    private MolecularFormula[] formulas;
    protected FingerprintVersion version;
    protected boolean compressed = false;

    public FilebasedDatabase(FingerprintVersion version, File dir) throws IOException {
        setDir(dir);
        this.name = dir.getName();
        this.version = version;
    }

    public String getName() {
        return name;
    }

    protected File getFileFor(MolecularFormula formula) {
        return new File(dir, formula.toString() + format);
    }


    public File getDir() {
        return dir;
    }

    public void setDir(File dir) throws IOException {
        this.dir = dir;
        refresh();
    }

    protected final static String[] SUPPORTED_FORMATS = new String[]{".CSV", ".CSV.GZ", ".JSON", ".JSON.GZ"};

    protected void refresh() throws IOException {
        final ArrayList<MolecularFormula> formulas = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) throw new IOException("Database have to be a directory of .csv xor .json files");
        for (File f : dir.listFiles()) {
            final String name = f.getName();
            final String upName = name.toUpperCase(Locale.US);
            if (upName.startsWith("SETTINGS")) continue;
            boolean found = false;

            for (String s : SUPPORTED_FORMATS) {
                if (upName.endsWith(s)) {
                    if (format == null || format.equals(s)) {
                        format = s;
                        found = true;
                        break;
                    } else {
                        throw new IOException("Database contains several formats. Only one format is allowed! Given format is " + String.valueOf(format) + " but " + name + " found.");
                    }
                }
            }

            if (!found) continue;
            final String form = name.substring(0, name.length() - format.length());
            MolecularFormula.parseAndExecute(form, formulas::add);
        }
        if (format == null) throw new IOException("Couldn't find any compounds in given database");
        format = format.toLowerCase();
        this.reader = format.equals(".json") || format.equals(".json.gz") ? new JSONReader() : new CSVReader();
        this.compressed = format.endsWith(".gz");
        this.formulas = formulas.toArray(MolecularFormula[]::new);
        Arrays.sort(this.formulas);
    }

    public boolean containsFormula(MolecularFormula formula){
        return getFileFor(formula).exists(); // should be faster that searching the array for large numbers
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(final double ionMass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {

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

        final double max = mass+deviation.absoluteFor(ionMass);
        ArrayList<FormulaCandidate> candidates = new ArrayList<>();
        while (insertionPoint < formulas.length && formulas[insertionPoint].getMass() <= max) {
            candidates.add(new FormulaCandidate(formulas[insertionPoint++], ionType, 0));
        }

        return candidates;
    }


    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        final ArrayList<CompoundCandidate> candidates = new ArrayList<>();
        final File structureFile = getFileFor(formula);
        if (structureFile.exists()) {
            try (final CloseableIterator<CompoundCandidate> iter = reader.readCompounds(getReaderFor(structureFile))){
                while (iter.hasNext()) {
                    candidates.add(iter.next());
                }
            } catch (IOException e) {
                throw new ChemicalDatabaseException(e);
            }
            return candidates;
        } else return candidates;
    }

    private Reader getReaderFor(File structureFile) throws IOException {
        if (compressed) {
            return new InputStreamReader(new GZIPInputStream(new FileInputStream(structureFile)), StandardCharsets.UTF_8);
        } else {
            return Files.newBufferedReader(structureFile.toPath(), StandardCharsets.UTF_8);
        }
    }

    @Override
    public List<FingerprintCandidate> lookupStructuresAndFingerprintsByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        final ArrayList<FingerprintCandidate> candidates = new ArrayList<>();
        return lookupStructuresAndFingerprintsByFormula(formula, candidates);
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        final File structureFile = getFileFor(formula);
        if (structureFile.exists()) {
            try (final CloseableIterator<FingerprintCandidate> iter = reader.readFingerprints(version, getReaderFor(structureFile))){
                while (iter.hasNext()) {
                    fingerprintCandidates.add(iter.next());
                }
            } catch (IOException e) {
                throw new ChemicalDatabaseException(e);
            }
            return fingerprintCandidates;
        } else return fingerprintCandidates;
    }

    /*@Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {

        final File name = new File(databasePath, formula.toString() + ".json.gz");
        if (name.exists()) {
            try (final GZIPInputStream zin = new GZIPInputStream(new BufferedInputStream(new FileInputStream(name)))) {
                try (final CloseableIterator<FingerprintCandidate> fciter = new JSONReader().readFingerprints(version, new InputStreamReader(zin))) {
                    while (fciter.hasNext()) fingerprintCandidates.add(fciter.next());
                }
            } catch (IOException e) {
                throw new ChemicalDatabaseException(e);
            }
            return fingerprintCandidates;
        } else {
            return fingerprintCandidates;
        }
    }*/

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
            final Collection<FingerprintCandidate> pseudoQueue = new AbstractCollection<FingerprintCandidate>() {

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
