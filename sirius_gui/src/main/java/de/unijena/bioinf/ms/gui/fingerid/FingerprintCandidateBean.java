

/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.InChISMILESUtils;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.fingerid.fingerprints.ECFPFingerprinter;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.ms.nightsky.sdk.model.BinaryFingerprint;
import de.unijena.bioinf.ms.nightsky.sdk.model.DBLink;
import de.unijena.bioinf.ms.nightsky.sdk.model.StructureCandidateFormula;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.generators.HighlightGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * This is the wrapper for the FingerprintCandidate class to interact with the gui
 * elements. It uses a special property change support that executes the events
 * in the EDT. So you can change all fields from any thread, the gui will still
 * be updated in the EDT. Some operations may NOT be Thread save, so you may have
 * to care about Synchronization.
 * <p>
 * WARNING: This class is wor in progress
 */

public class FingerprintCandidateBean implements SiriusPCS, Comparable<FingerprintCandidateBean> {
    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);

    @Override
    public HiddenChangeSupport pcs() {
        return pcs;
    }

    public static final FingerprintCandidateBean PROTOTYPE = new PrototypeCompoundCandidate();
    public static final boolean ECFP_ENABLED = true;
    private static final double THRESHOLD_FP = 0.4;

    //data
    @NotNull
    private final ProbabilityFingerprint fp;
    @NotNull
    protected final StructureCandidateFormula candidate;


    //view
    private volatile IAtomContainer molecule;
    protected CompoundMatchHighlighter highlighter;

    protected boolean prepared = false;

    protected CircularFingerprinter.FP[] relevantFps;
    protected int[] ecfpHashs;

    protected FingerprintAgreement substructures;
    protected final DatabaseLabel[] labels;

    protected final EmptyLabel referenceLabel;

    protected final EmptyLabel moreLabel;

    protected boolean atomCoordinatesAreComputed = false;
    protected ReentrantLock compoundLock = new ReentrantLock();


    public FingerprintCandidateBean(@NotNull StructureCandidateFormula candidate, @NotNull ProbabilityFingerprint fp) {
        this.fp = fp; //todo nightsky: ->  do we want to lazy load the fp instead?
        this.candidate = candidate;
        this.relevantFps = null;


        if (this.candidate.getDbLinks() == null || this.candidate.getDbLinks().isEmpty()) {
            this.labels = new DatabaseLabel[0];
        } else {
            List<DatabaseLabel> labels = new ArrayList<>();
            @NotNull Map<String, List<DBLink>> linkeDBs = this.candidate.getDbLinks().stream()
                    .collect(Collectors.groupingBy(DBLink::getName, toList()));

            for (Map.Entry<String, List<DBLink>> entry : linkeDBs.entrySet()) {
                final List<String> cleaned = entry.getValue().stream().map(DBLink::getId)
                        .filter(Objects::nonNull).distinct().toList();

                if (entry.getKey().equals(DataSource.LIPID.realName))
                    labels.add(new DatabaseLabel(entry.getKey(), "Lipid - " + entry.getValue().iterator().next(), cleaned.toArray(String[]::new), new Rectangle(0, 0, 0, 0)));
                else
                    labels.add(new DatabaseLabel(entry.getKey(), cleaned.toArray(String[]::new), new Rectangle(0, 0, 0, 0)));
            }
            Collections.sort(labels);
            this.labels = labels.toArray(DatabaseLabel[]::new);
        }

        this.referenceLabel = new EmptyLabel();
        this.moreLabel = new EmptyLabel();
    }

    public void highlightInBackground() {
        CompoundMatchHighlighter h = new CompoundMatchHighlighter(this, getPlatts());
        synchronized (this) {
            this.highlighter = h;
        }
    }

    public Double getTanimotoScore() {
        return candidate.getTanimotoSimilarity();
    }

    public double getScore() {
        return candidate.getCsiScore();
    }

    public ProbabilityFingerprint getPlatts() {
        return fp;
    }

    public String getName() {
        return candidate.getStructureName();
    }

    public String getInChiKey() {
        return candidate.getInchiKey();
    }

    public InChI getInChI() {
        try {
            return InChISMILESUtils.getInchiFromSmiles(candidate.getSmiles(), true);
        } catch (CDKException e) {
            throw new RuntimeException("Could not generate InChI from Smiles", e);
        }
    }

    public String getSmiles(){
        return candidate.getSmiles();
    }

    public @NotNull StructureCandidateFormula getCandidate() {
        return candidate;
    }

    public long getMergedDBFlags() {
        return CustomDataSources.getDBFlagsFromNames(candidate.getDbLinks().stream().map(DBLink::getName).collect(toList()));
    }

    public String getMolecularFormula() {
        return candidate.getMolecularFormula();
    }

    public IAtomContainer getMolecule() {
        if (molecule == null) {
            molecule = parseMoleculeFromSmiles();
        }
        return molecule;
    }

    @Override
    public int compareTo(FingerprintCandidateBean o) {
        return Double.compare(o.getScore(), getScore()); //ATTENTION inverse
    }

    public double getXLogP() {
        return candidate.getXlogP();
    }

    public OptionalDouble getXLogPOpt() {
        double xLogP = getXLogP();
        if (Double.isNaN(xLogP))
            return OptionalDouble.empty();
        return OptionalDouble.of(xLogP);
    }

    public boolean computeAtomCoordinates() {
        if (atomCoordinatesAreComputed) return false;
        try {
            StructureDiagramGenerator sdg = new StructureDiagramGenerator();
            sdg.setMolecule(getMolecule(), false);
            sdg.generateCoordinates();
        } catch (CDKException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            return false;
        }
        atomCoordinatesAreComputed = true;
        return true;
    }

    public boolean hasFingerprintIndex(int relativeIndex) {
        return candidate.getFingerprint().getBitsSet().contains(relativeIndex); //todo nightsky this is currently not O(1)
    }

    public boolean highlightFingerprint(int absoluteIndex) {
        if (!prepared) parseAndPrepare();
        final FingerprintVersion version = fp.getFingerprintVersion();
        final IAtomContainer molecule = getMolecule();
        for (IAtom atom : molecule.atoms()) atom.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
        for (IBond bond : molecule.bonds()) bond.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
        if (!hasFingerprintIndex(version.getRelativeIndexOf(absoluteIndex))) {
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
                    colorMap.put(getMolecule().getAtom(atom), 0);
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

    public FingerprintAgreement getSubstructures(ProbabilityFingerprint prediction, PredictionPerformance[] performances) {
        if (substructures == null) {
            final boolean[] boolFP = new boolean[candidate.getFingerprint().getLength()];
            candidate.getFingerprint().getBitsSet().forEach(i -> boolFP[i] = true);
            substructures = FingerprintAgreement.getSubstructures(
                    prediction.getFingerprintVersion(), prediction.toProbabilityArray(), boolFP, performances, 0.25);
        }
        return substructures;
    }


    public void parseAndPrepare() {
        try {
            // we do not want to search anything in the compound but just "enforce initialization" of the molecule
            final FasterSmartsQueryTool tool = new FasterSmartsQueryTool("Br", DefaultChemObjectBuilder.getInstance());
            tool.matches(getMolecule());

            if (ECFP_ENABLED) {
                final ECFPFingerprinter ecfpFingerprinter = new ECFPFingerprinter();
                ecfpFingerprinter.getBitFingerprint(getMolecule());
                this.relevantFps = ecfpFingerprinter.getRelevantFingerprintDetails();
                this.ecfpHashs = new int[relevantFps.length];
                for (int k = 0; k < relevantFps.length; ++k) ecfpHashs[k] = relevantFps[k].hashCode;
            }
            prepared = true;
        } catch (CDKException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
        }

    }

    private IAtomContainer parseMoleculeFromSmiles() {
        try {
            final IAtomContainer c = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(candidate.getSmiles());
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(c);
            return c;
        } catch (CDKException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public boolean hasAtomContainer() {
        return molecule != null;
    }

    public double getXlogp() {
        return candidate.getXlogP();
    }

    private static class PrototypeCompoundCandidate extends FingerprintCandidateBean {
        private static StructureCandidateFormula makeSourceCandidate() {
            final StructureCandidateFormula candidate = new StructureCandidateFormula()
                    .inchiKey("WQZGKKKJIJFFOK-GASJEMHNSA-N")
                    .smiles(new Smiles("OC[C@H]1OC(O)[C@H](O)[C@@H](O)[C@@H]1O").smiles)
                    .structureName("Glucose");

            candidate.dbLinks(List.of(new DBLink().name("PubChem").id("5793")))
                    .fingerprint(new BinaryFingerprint()
                            .bitsSet(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 34, 35, 38, 80, 120))
                            .length(130));
            return candidate;
        }

        private PrototypeCompoundCandidate() {
            super(makeSourceCandidate(), null);
        }


        @Override
        public Double getTanimotoScore() {
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