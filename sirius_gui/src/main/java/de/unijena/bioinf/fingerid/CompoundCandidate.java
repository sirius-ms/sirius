/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.fingerid.fingerprints.ECFPFingerprinter;
import org.jdesktop.beans.AbstractBean;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.generators.HighlightGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class CompoundCandidate extends AbstractBean implements Comparable<CompoundCandidate> {
    public static final CompoundCandidate PROTOTYPE = new PrototypeCompoundCandidate();

    public static final boolean ECFP_ENABLED = true;

    private static final double THRESHOLD_FP = 0.4;

    protected final FingerIdData data;
    protected final Compound compound;
    protected final int rank, index;
    protected final String molecularFormulaString;

    protected CompoundMatchHighlighter highlighter;

    public void highlightInBackground() {
        CompoundMatchHighlighter h = new CompoundMatchHighlighter(this, getPlatts());
        synchronized (this) {
            this.highlighter = h;
        }
    }


    public double getTanimotoScore() {
        return data.tanimotoScores[index];
    }

    public double getScore() {
        return data.scores[index];
    }

    public ProbabilityFingerprint getPlatts() {
        return data.platts;
    }

    public FingerIdData getData() {
        return data;
    }

    public String getName() {
        return compound.getName();
    }

    public String getInChiKey() {
        return compound.getInchi().key;
    }

    public Compound getCompound() {
        return compound;
    }

    public String getMolecularFormula() {
        return molecularFormulaString; // is that time intensive
    }


    protected boolean prepared = false;//todo fire property change???

    protected CircularFingerprinter.FP[] relevantFps;
    protected int[] ecfpHashs;//todo fire property change???

    protected FingerprintAgreement substructures; //todo fire property change???
    protected final DatabaseLabel[] labels;

    protected boolean atomCoordinatesAreComputed = false;
    protected ReentrantLock compoundLock = new ReentrantLock();

    public CompoundCandidate(int rank, int index, FingerIdData data) {
        this(rank, index, data, data.compounds[index], data.compounds[index].getInchi().extractFormula().toString());
    }

    public CompoundCandidate(int rank, int index, FingerIdData data, String formula) {
        this(rank, index, data, data.compounds[index], formula);
    }

    private CompoundCandidate(int rank, int index, FingerIdData data, Compound compound, String formula) {
        this.rank = rank;
        this.index = index;
        this.data = data;
        this.compound = compound;
        this.molecularFormulaString = formula;
        this.relevantFps = null;


        if (this.compound == null || this.compound.databases == null) {
            this.labels = new DatabaseLabel[0];
        } else {
            List<DatabaseLabel> labels = new ArrayList<>();
            for (String key : this.compound.databases.keySet()) {
                final Collection<String> values = this.compound.databases.get(key);
                final ArrayList<String> cleaned =  new ArrayList<>(values.size());
                for (String value : values) {
                    if (value != null)
                        cleaned.add(value);
                }
                labels.add(new DatabaseLabel(key, cleaned.toArray(new String[cleaned.size()]), new Rectangle(0, 0, 0, 0)));
            }
            Collections.sort(labels);
            this.labels = labels.toArray(new DatabaseLabel[labels.size()]);
        }
    }

    public boolean computeAtomCoordinates() {
        if (atomCoordinatesAreComputed) return false;
        try {
            StructureDiagramGenerator sdg = new StructureDiagramGenerator();
            sdg.setMolecule(compound.getMolecule(), false);
            sdg.generateCoordinates();
        } catch (CDKException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            return false;
        }
        atomCoordinatesAreComputed = true;
        return true;
    }

    public boolean hasFingerprintIndex(int index) {
        return compound.fingerprint.isSet(index);
    }

    public boolean highlightFingerprint(int absoluteIndex) {
        if (!prepared) parseAndPrepare();
        final FingerprintVersion version = compound.fingerprint.getFingerprintVersion();
        final IAtomContainer molecule = compound.getMolecule();
        for (IAtom atom : molecule.atoms()) atom.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
        for (IBond bond : molecule.bonds()) bond.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
        if (!hasFingerprintIndex(absoluteIndex)) {
            molecule.setProperty(HighlightGenerator.ID_MAP, Collections.emptyMap());
            return false;
        }
        final MolecularProperty property = version.getMolecularProperty(absoluteIndex);
        if (property instanceof SubstructureProperty) {
            final String smarts = ((SubstructureProperty) property).getSmarts();
            final HashMap<IAtom, Integer> colorMap = new HashMap<>();

            int minCount;
            if (property instanceof SubstructureCountProperty)
                minCount = ((SubstructureCountProperty) property).getMinimalCount();
            else minCount = 1;

            molecule.setProperty(HighlightGenerator.ID_MAP, Collections.emptyMap());
            FasterSmartsQueryTool tool = new FasterSmartsQueryTool(smarts, DefaultChemObjectBuilder.getInstance());
            try {
                if (tool.matches(molecule)) {
                    final List<List<Integer>> mappings = tool.getUniqueMatchingAtoms();
                    for (List<Integer> mapping : mappings) {
                        --minCount;
                        final HashSet<IAtom> atoms = new HashSet<>(mapping.size());
                        for (int i : mapping) atoms.add(molecule.getAtom(i));
                        for (Integer i : mapping) {
                            if (!colorMap.containsKey(molecule.getAtom(i)))
                                colorMap.put(molecule.getAtom(i), minCount >= 0 ? 0 : 1);
                            if (molecule.getAtom(i).getProperty(StandardGenerator.HIGHLIGHT_COLOR) == null)
                                molecule.getAtom(i).setProperty(StandardGenerator.HIGHLIGHT_COLOR, minCount >= 0 ? CandidateListDetailView.PRIMARY_HIGHLIGHTED_COLOR : CandidateListDetailView.SECONDARY_HIGHLIGHTED_COLOR);
                            for (IBond b : molecule.getConnectedBondsList(molecule.getAtom(i))) {
                                if (atoms.contains(b.getAtom(0)) && atoms.contains(b.getAtom(1))) {
                                    if (b.getProperty(StandardGenerator.HIGHLIGHT_COLOR) == null)
                                        b.setProperty(StandardGenerator.HIGHLIGHT_COLOR, minCount >= 0 ? CandidateListDetailView.PRIMARY_HIGHLIGHTED_COLOR : CandidateListDetailView.SECONDARY_HIGHLIGHTED_COLOR);
                                }
                            }

                        }
                    }
                }
            } catch (CDKException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            }
            molecule.setProperty(HighlightGenerator.ID_MAP, colorMap);
            return true;
        } else if (ECFP_ENABLED && property instanceof ExtendedConnectivityProperty) {
            final HashMap<IAtom, Integer> colorMap = new HashMap<>();
            final int index = Arrays.binarySearch(ecfpHashs, ((ExtendedConnectivityProperty) property).getHash());
            if (index >= 0) {
                final HashSet<IAtom> atoms = new HashSet<>(relevantFps[index].atoms.length);
                for (int i : relevantFps[index].atoms) atoms.add(molecule.getAtom(i));
                for (int atom : relevantFps[index].atoms) {
                    colorMap.put(compound.molecule.getAtom(atom), 0);
                    molecule.getAtom(atom).setProperty(StandardGenerator.HIGHLIGHT_COLOR, CandidateListDetailView.PRIMARY_HIGHLIGHTED_COLOR);
                    for (IBond b : molecule.getConnectedBondsList(molecule.getAtom(atom))) {
                        if (atoms.contains(b.getAtom(0)) && atoms.contains(b.getAtom(1))) {
                            b.setProperty(StandardGenerator.HIGHLIGHT_COLOR, CandidateListDetailView.PRIMARY_HIGHLIGHTED_COLOR);
                        }
                    }
                }
            }
            molecule.setProperty(HighlightGenerator.ID_MAP, colorMap);
            return true;
        } else {
            return false;
        }
    }

    public FingerprintAgreement getSubstructures(CSIFingerIdComputation computations, ProbabilityFingerprint prediction) {
        if (substructures == null)
            substructures = FingerprintAgreement.getSubstructures(prediction.getFingerprintVersion(), prediction.toProbabilityArray(), compound.fingerprint.toBooleanArray(), computations.performances, 0.25);
        return substructures;
    }


    public void parseAndPrepare() {
        try {
            // we do not want to search anything in the compound but just "enforce initialization" of the molecule
            final FasterSmartsQueryTool tool = new FasterSmartsQueryTool("Br", DefaultChemObjectBuilder.getInstance());
            tool.matches(compound.getMolecule());

            if (ECFP_ENABLED) {
                final ECFPFingerprinter ecfpFingerprinter = new ECFPFingerprinter();
                ecfpFingerprinter.getBitFingerprint(compound.getMolecule());
                this.relevantFps = ecfpFingerprinter.getRelevantFingerprintDetails();
                this.ecfpHashs = new int[relevantFps.length];
                for (int k = 0; k < relevantFps.length; ++k) ecfpHashs[k] = relevantFps[k].hashCode;
            }
            prepared = true;
        } catch (CDKException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
        }

    }

    @Override
    public int compareTo(CompoundCandidate o) {
        return Double.compare(o.getScore(), getScore()); //ATTENTION inverse
    }

    public double getXLogP() {
        return getCompound().xlogP;
    }

    private static class PrototypeCompoundCandidate extends CompoundCandidate {
        private PrototypeCompoundCandidate() {
            super(0, 0, null, Compound.getPrototypeCompound(),"PROTO");
        }

        @Override
        public double getTanimotoScore() {
            return 0d;
        }

        @Override
        public double getScore() {
            return 0d;
        }

        @Override
        public ProbabilityFingerprint getPlatts() {
            return null;
        }
    }
}