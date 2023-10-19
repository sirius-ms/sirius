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

package de.unijena.bioinf.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.fingerid.Fingerprinter;
import gnu.trove.list.array.TShortArrayList;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.IFingerprinter;

import java.io.IOException;
import java.util.*;

/**
 * CDK fingerprints + own extensions
 */
public class ExtendedFingerprintVersion extends FingerprintVersion {

    protected class SearchEngine implements AbstractChemicalDatabase {

        protected final AbstractChemicalDatabase innerDatabase;

        protected SearchEngine(AbstractChemicalDatabase database) {
            innerDatabase = database;
        }


        @Override
        public String getName() {
            return innerDatabase.getName();
        }

        @Override
        public List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
            return innerDatabase.lookupMolecularFormulas(mass, deviation, ionType);
        }

        @Override
        public boolean containsFormula(MolecularFormula formula) throws ChemicalDatabaseException {
            return innerDatabase.containsFormula(formula);
        }

        @Override
        public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
            return innerDatabase.lookupStructuresByFormula(formula);
        }

        protected <T extends Collection<FingerprintCandidate>> T wrap(T orig, List<FingerprintCandidate> collection) throws ChemicalDatabaseException {
            for (FingerprintCandidate fc : collection) {
                final TShortArrayList sc = new TShortArrayList(fc.getFingerprint().toIndizesArray());
                try {
                    extend(sc, fc.getInchi());
                    orig.add(new FingerprintCandidate(fc, new ArrayFingerprint(ExtendedFingerprintVersion.this, sc.toArray())));
                } catch (CDKException e) {
                    throw new ChemicalDatabaseException(e);
                }
            }
            return orig;
        }

        @Override
        public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
            return wrap(fingerprintCandidates, innerDatabase.lookupStructuresAndFingerprintsByFormula(formula, new ArrayList<FingerprintCandidate>()));
        }

        @Override
        public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
            return wrap(new ArrayList<FingerprintCandidate>(), innerDatabase.lookupFingerprintsByInchis(inchi_keys));
        }

        @Override
        public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
            return innerDatabase.lookupManyInchisByInchiKeys(inchi_keys);
        }

        @Override
        public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException {
            return wrap(new ArrayList<FingerprintCandidate>(), innerDatabase.lookupFingerprintsByInchi(compounds));
        }

        @Override
        public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws ChemicalDatabaseException {
            innerDatabase.annotateCompounds(sublist);
        }

        @Override
        public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getChemDbDate() throws ChemicalDatabaseException {
            return innerDatabase.getChemDbDate();
        }

        @Override
        public long countAllFingerprints() throws ChemicalDatabaseException {
            return innerDatabase.countAllFingerprints();
        }

        @Override
        public long countAllFormulas() throws ChemicalDatabaseException {
            return innerDatabase.countAllFormulas();
        }

        @Override
        public void close() throws IOException {
            innerDatabase.close();
        }
    }

    protected final IFingerprinter[] extensions;
    protected final int[] offsets, sizes;
    protected final int size;
    protected final CdkFingerprintVersion underlyingFpVersion;
    protected final MolecularProperty[] properties;

    public ExtendedFingerprintVersion(IFingerprinter... extendedFingerprinters) {
        this.extensions = extendedFingerprinters;
        this.offsets = new int[extendedFingerprinters.length];
        this.underlyingFpVersion = CdkFingerprintVersion.getDefault();
        offsets[0] = underlyingFpVersion.size();
        this.sizes = new int[extendedFingerprinters.length];
        int n=0;
        for (int k=0; k < extendedFingerprinters.length; ++k) {
            sizes[k] = extendedFingerprinters[k].getSize();
            if (k +1 < extendedFingerprinters.length)
                offsets[k+1] = offsets[k]+sizes[k];
            n += sizes[k];
        }
        this.size = n+offsets[0];
        this.properties = new MolecularProperty[this.size];
    }

    public AbstractChemicalDatabase getSearchEngine() throws ChemicalDatabaseException {
        return new SearchEngine(new ChemicalDatabase());
    }

    public AbstractChemicalDatabase getSearchEngine(AbstractChemicalDatabase db) throws ChemicalDatabaseException {
        return new SearchEngine(db);
    }

    public Fingerprint compute(InChI inchi) throws CDKException {
        final TShortArrayList indizes = getDefaults(inchi);
        extend(indizes, inchi);
        return new ArrayFingerprint(this, indizes.toArray());
    }

    public Fingerprint compute(AbstractChemicalDatabase db, InChI inchi) throws CDKException {
        final TShortArrayList indizes = getDefaults(db, inchi);
        extend(indizes, inchi);
        return new ArrayFingerprint(this, indizes.toArray());
    }

    protected void extend(TShortArrayList indizes, InChI inchi) throws CDKException {
        final Fingerprinter fingerprinter = new Fingerprinter(Arrays.asList(extensions));
        final BitSet[] bitsets = fingerprinter.computeFingerprints(fingerprinter.convertInchi2Mol(inchi.in2D));
        for (int k=0; k < bitsets.length; ++k) {
            for (int o=bitsets[k].nextSetBit(0); o >= 0; o = bitsets[k].nextSetBit(o+1)) {
                indizes.add((short)(offsets[k]+o));
            }
        }
    }

    protected TShortArrayList getDefaults(AbstractChemicalDatabase db, InChI inchi) throws CDKException {
        try {
            final Fingerprint fp = db.lookupFingerprintByInChI(inchi);
            if (fp!=null) return new TShortArrayList(fp.toIndizesArray());
        } catch (ChemicalDatabaseException e) {
            e.printStackTrace();
        }
        return getDefaults(inchi);
    }

    protected TShortArrayList getDefaults(InChI inchi) throws CDKException {
        final Fingerprinter defaultFp = new Fingerprinter();
        final BitSet[] bitsets = defaultFp.computeFingerprints(defaultFp.convertInchi2Mol(inchi.in2D));
        final TShortArrayList indizes = new TShortArrayList(128);
        final CdkFingerprintVersion defaultVersion = CdkFingerprintVersion.getDefault();
        int offset=0;
        for (int k=0; k < bitsets.length; ++k) {
            for (int o=bitsets[k].nextSetBit(0); o >= 0; o = bitsets[k].nextSetBit(o+1)) {
                indizes.add((short)(offset+o));
            }
            offset += defaultVersion.getFingerprintTypeAt(k).length;
        }
        return indizes;
    }

    public void setProperty(int fingerprinter, int offset, MolecularProperty property) {
        if (fingerprinter > extensions.length || offset > sizes[fingerprinter])
            throw new IndexOutOfBoundsException();
        final int index = offsets[fingerprinter]+offset;
        properties[index] = property;
    }

    public int absoluteIndex(int fingerprinter, int offset) {
        if (fingerprinter > extensions.length || offset > sizes[fingerprinter])
            throw new IndexOutOfBoundsException();
        return offsets[fingerprinter]+offset;
    }

    @Override
    public MolecularProperty getMolecularProperty(int index) {
        if (index < offsets[0]) return underlyingFpVersion.getMolecularProperty(index);
        else return properties[index-offsets[0]];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean compatible(FingerprintVersion fingerprintVersion) {
        return identical(fingerprintVersion);
    }

    @Override
    public boolean identical(FingerprintVersion fingerprintVersion) {
        return this == fingerprintVersion;
    }
}
