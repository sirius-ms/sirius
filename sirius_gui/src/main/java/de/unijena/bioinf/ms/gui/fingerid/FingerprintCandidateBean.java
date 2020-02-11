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

package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.chemdb.CompoundCandidateChargeLayer;
import de.unijena.bioinf.chemdb.CompoundCandidateChargeState;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.custom.CustomDataSourceService;
import de.unijena.bioinf.fingerid.CSIPredictor;
import de.unijena.bioinf.fingerid.fingerprints.ECFPFingerprinter;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import net.sf.jniinchi.INCHI_RET;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
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
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is the wrapper for the FingerprintCandidate class to interact with the gui
 * elements. It uses a special property change support that executes the events
 * in the EDT. So you can change all fields from any thread, the gui will still
 * be updated in the EDT. Some operations may NOT be Thread save, so you may have
 * to care about Synchronization.
 * <p>
 * WARNING: This class is wor in progress
 */

//todo can we create a dummy PCS for Immuatable Beans
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
    protected final PrecursorIonType adduct;
    protected final ProbabilityFingerprint fp;
    protected final FingerprintCandidate candidate;
    protected final double score;
    protected final int rank;


    //view
    protected final String molecularFormulaString;
    private volatile IAtomContainer molecule; //todo check if we need to cache this this
    protected CompoundMatchHighlighter highlighter;

    protected boolean prepared = false;//todo fire property change???

    protected CircularFingerprinter.FP[] relevantFps;
    protected int[] ecfpHashs;//todo fire property change???

    protected FingerprintAgreement substructures; //todo fire property change???
    protected final DatabaseLabel[] labels;

    protected boolean atomCoordinatesAreComputed = false;
    protected ReentrantLock compoundLock = new ReentrantLock();


    public FingerprintCandidateBean(int rank, ProbabilityFingerprint fp, Scored<FingerprintCandidate> scoredCandidate, PrecursorIonType adduct) {
        this(rank, fp, scoredCandidate.getCandidate(), scoredCandidate.getScore(), adduct);
    }

    private FingerprintCandidateBean(int rank, ProbabilityFingerprint fp, FingerprintCandidate candidate, double candidateScore, PrecursorIonType adduct) {
        this.rank = rank;
        this.fp = fp;
        this.score = candidateScore;
        this.candidate = candidate;
        if (candidate.getTanimoto() == null || candidate.getTanimoto().isNaN()) {
            if (this.fp != null && candidate.getFingerprint() != null) {
                candidate.setTanimoto(null);
                BasicJJob<Double> j = new BasicJJob<>() {
                    @Override
                    protected Double compute() {
                        double t = Tanimoto.probabilisticTanimoto(fp, candidate.getFingerprint()).expectationValue();
                        setTanimoto(t);
                        return t;
                    }
                };
                j.setPriority(JJob.JobPriority.NOW);
                SiriusJobs.getGlobalJobManager().submitJob(j);
            } else {
                candidate.setTanimoto(Double.NaN);
            }
        }
        this.molecularFormulaString = candidate.getInchi().extractFormulaOrThrow().toString();
        this.adduct = adduct;
        this.relevantFps = null;


        if (this.candidate.getLinkedDatabases().isEmpty()) {
            this.labels = new DatabaseLabel[0];
        } else {
            List<DatabaseLabel> labels = new ArrayList<>();
            for (String key : this.candidate.getLinkedDatabases().keySet()) {
                final Collection<String> values = this.candidate.getLinkedDatabases().get(key);
                final ArrayList<String> cleaned = new ArrayList<>(values.size());
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

    protected CSIPredictor getCorrespondingCSIPredictor() throws IOException {
        return (CSIPredictor) ApplicationCore.WEB_API.getStructurePredictor(adduct.getCharge() > 0 ? PredictorType.CSI_FINGERID_POSITIVE : PredictorType.CSI_FINGERID_POSITIVE);
    }


    public void highlightInBackground() {
        CompoundMatchHighlighter h = new CompoundMatchHighlighter(this, getPlatts());
        synchronized (this) {
            this.highlighter = h;
        }
    }

    public void setTanimoto(Double tanimoto) {
        Double old = candidate.getTanimoto();
        candidate.setTanimoto(tanimoto);
        pcs.firePropertyChange("fpc.tanimoto", old, candidate.getTanimoto());
    }

    public Double getTanimotoScore() {
        return candidate.getTanimoto();
    }

    public double getScore() {
        return score;
    }

    public ProbabilityFingerprint getPlatts() {
        return fp;
    }

    public String getName() {
        return candidate.getName();
    }

    public String getInChiKey() {
        return candidate.getInchi().key;
    }

    public FingerprintCandidate getFingerprintCandidate() {
        return candidate;
    }

    public String getMolecularFormula() {
        return molecularFormulaString;
    }

    public IAtomContainer getMolecule() {
        if (molecule == null) {
            molecule = parseMoleculeFromSmiles();
        }
        return molecule;
    }

    public boolean canBeNeutralCharged() {
        return hasChargeState(CompoundCandidateChargeState.NEUTRAL_CHARGE);
    }

    public boolean canBePositivelyCharged() {
        return hasChargeState(CompoundCandidateChargeState.POSITIVE_CHARGE);
    }

    public boolean canBeNegativelyCharged() {
        return hasChargeState(CompoundCandidateChargeState.NEGATIVE_CHARGE);
    }

    public boolean hasChargeState(CompoundCandidateChargeState chargeState) {
        return (hasChargeState(candidate.getpLayer(), chargeState.getValue()) || hasChargeState(candidate.getqLayer(), chargeState.getValue()));
    }

    public boolean hasChargeState(CompoundCandidateChargeLayer chargeLayer, CompoundCandidateChargeState chargeState) {
        return (chargeLayer == CompoundCandidateChargeLayer.P_LAYER ?
                hasChargeState(candidate.getpLayer(), chargeState.getValue()) :
                hasChargeState(candidate.getqLayer(), chargeState.getValue())
        );
    }

    private boolean hasChargeState(int chargeLayer, int chargeState) {
        return ((chargeLayer & chargeState) == chargeState);
    }

    @Override
    public int compareTo(FingerprintCandidateBean o) {
        return Double.compare(o.getScore(), getScore()); //ATTENTION inverse
    }

    public double getXLogP() {
        return getFingerprintCandidate().getXlogp();
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

    public boolean hasFingerprintIndex(int index) {
        return candidate.getFingerprint().isSet(index);
    }

    public boolean highlightFingerprint(int absoluteIndex) {
        if (!prepared) parseAndPrepare();
        final FingerprintVersion version = candidate.getFingerprint().getFingerprintVersion();
        final IAtomContainer molecule = getMolecule();
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

    public FingerprintAgreement getSubstructures(ProbabilityFingerprint prediction) {
        if (substructures == null) {
            try {
                substructures = FingerprintAgreement.getSubstructures(
                        prediction.getFingerprintVersion(), prediction.toProbabilityArray(),
                        candidate.getFingerprint().toBooleanArray(), getCorrespondingCSIPredictor().getPerformances(),
                        0.25);
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).warn("Could not reach WebAPI. Cause: " + e.getMessage());
            }
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

    private IAtomContainer parseMoleculeFromInChi() {
        try {
            final InChIGeneratorFactory f = InChIGeneratorFactory.getInstance();
            final InChIToStructure s = f.getInChIToStructure(candidate.getInchi().in2D, SilentChemObjectBuilder.getInstance());
            if (s.getReturnStatus() == INCHI_RET.OKAY && (s.getReturnStatus() == INCHI_RET.OKAY || s.getReturnStatus() == INCHI_RET.WARNING)) {
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(s.getAtomContainer());

                return s.getAtomContainer();
            } else {
                LoggerFactory.getLogger(getClass()).warn("Cannot parse InChI: " + candidate.getInchi().in2D + " due to the following error: " + s.getMessage() + " Return code: " + s.getReturnStatus() + ", Return status: " + s.getReturnStatus().toString());
                // try to parse smiles instead
                return parseMoleculeFromSmiles();
            }
            // calculate xlogP
        } catch (CDKException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            return parseMoleculeFromSmiles();
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
        return candidate.getXlogp();
    }

    public int index() {
        //todo check if we really need that for the detail list reloading stuff
        return rank - 1;
    }

    private static class PrototypeCompoundCandidate extends FingerprintCandidateBean {
        private static FingerprintCandidate makeSourceCandidate() {
            final FingerprintCandidate candidate = new FingerprintCandidate(
                    new InChI("WQZGKKKJIJFFOK-GASJEMHNSA-N", "InChI=1S/C6H12O6/c7-1-2-3(8)4(9)5(10)6(11)12-2/h2-11H,1H2/t2-,3-,4+,5-,6?/m1/s1"),
                    new ArrayFingerprint(CdkFingerprintVersion.getDefault(), new short[]{
                            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 34, 35, 38, 80, 120
                    })
            );
            candidate.setSmiles(new Smiles("OC[C@H]1OC(O)[C@H](O)[C@@H](O)[C@@H]1O").smiles);
            candidate.setName("Glucose");

            CustomDataSourceService.Source c = CustomDataSourceService.getSourceFromName("PubChem");
            long bit = c.flag();
            candidate.getLinkedDatabases().put("PubChem", "5793");
            candidate.setBitset(candidate.getBitset() | bit);
            return candidate;
        }

        private PrototypeCompoundCandidate() {
            super(0, null, makeSourceCandidate(), -12.22, PrecursorIonType.getPrecursorIonType("[M + C2H3N + Na]+"));
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