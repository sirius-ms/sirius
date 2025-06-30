package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.Tanimoto;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.InChISMILESUtils;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;
import java.util.Objects;

public class AddExternalStructureJJob extends BasicMasterJJob<Scored<FingerprintCandidate>> {
    final String smiles;
    final List<FingerIdResult> idResults;
    final int charge;

    private final WebAPI<?> webAPI;

    public AddExternalStructureJJob(String smiles, List<FingerIdResult> idResults, int charge, WebAPI<?> webAPI) {
        super(JobType.CPU);
        this.smiles = smiles;
        this.idResults = idResults.stream()
                .filter(c -> c.hasAnnotation(FingerprintResult.class))
                .toList();
        this.webAPI = webAPI;
        this.charge = charge;
    }

    @Override
    protected Scored<FingerprintCandidate> compute() throws Exception {
        final @NotNull CSIPredictor predictor = NetUtils.tryAndWait(() -> (CSIPredictor)
                        webAPI.getStructurePredictor(charge),
                this::checkForInterruption);

        final FingerIdData fingerIdData = charge > 0
                ? NetUtils.tryAndWait(() -> webAPI.getFingerIdData(PredictorType.CSI_FINGERID_POSITIVE), this::checkForInterruption)
                : NetUtils.tryAndWait(() -> webAPI.getFingerIdData(PredictorType.CSI_FINGERID_NEGATIVE), this::checkForInterruption);
        final MaskedFingerprintVersion fpMask = fingerIdData.getFingerprintVersion();
        final FixedFingerprinter fixedFingerprinter = new FixedFingerprinter(fingerIdData.getCdkFingerprintVersion(), false);

        FingerprintCandidate fingerprintCandidate = computeFingerprint(smiles, fixedFingerprinter, fpMask);
        if (fingerprintCandidate == null) throw new  RuntimeException("Cannot compute fingerprint for " + smiles);

        MolecularFormula molecularFormula = InChIs.extractNeutralFormulaByAdjustingHsOrThrow(fingerprintCandidate.getInchi().in2D);

        FingerIdResult idResult = idResults.stream()
                .filter(r -> r.getMolecularFormula().equals(molecularFormula)).findFirst().orElseThrow(() -> new RuntimeException("Cannot find FingerIdResult for molecular formula '"+molecularFormula+"' of SMILES '"+smiles+"'"));
        ProbabilityFingerprint fp = idResult.getPredictedFingerprint();

        checkForInterruption();

        Scored<FingerprintCandidate> scoredCandidate = scoreNew(predictor, webAPI, molecularFormula, fp, fingerprintCandidate);

        scoredCandidate.getCandidate().setTanimoto(Tanimoto.nonProbabilisticTanimoto(scoredCandidate.getCandidate().getFingerprint(), fp));
        scoredCandidate.getCandidate().setLinks(List.of(new DBLink("Sketched Structure", null))); //are not stored
        scoredCandidate.getCandidate().setName("Sketched Structure");


        MsNovelistFingerblastResult result = new MsNovelistFingerblastResult(List.of(scoredCandidate), new double[]{0});

        idResult.annotate(result);
        return scoredCandidate;
    }

    protected Scored<FingerprintCandidate> scoreNew(@NotNull CSIPredictor predictor, @NotNull WebAPI<?> webAPI, MolecularFormula molecularFormula, ProbabilityFingerprint fp, FingerprintCandidate fingerprintCandidate) throws Exception {
        // try and get bayessian network (covTree) for molecular formula
        BayesnetScoring bayesnetScoring = NetUtils.tryAndWait(() -> webAPI.getBayesnetScoring(
                predictor.predictorType,
                webAPI.getFingerIdData(predictor.predictorType),
                molecularFormula), this::checkForInterruption);

        checkForInterruption();

        // bayesnetScoring is null --> make a job which computes the bayessian network (covTree) for the
        // given molecular formula
        if (bayesnetScoring == null) {
            WebJJob<CovtreeJobInput, ?, BayesnetScoring, ?> covTreeJob =
                    webAPI.submitCovtreeJob(molecularFormula, predictor.predictorType);
            bayesnetScoring = covTreeJob.awaitResult();
        }

        checkForInterruption();

        Scored<FingerprintCandidate> scoredCandidate  = Fingerblast.score(predictor.getPreparedFingerblastScorer(ParameterStore.of(fp, bayesnetScoring)), List.of(fingerprintCandidate), fp).get(0);

        checkForInterruption();

        return scoredCandidate;
    }

    private FingerprintCandidate computeFingerprint(String smiles, FixedFingerprinter fixedFingerprinter, MaskedFingerprintVersion fpMask) {
        IAtomContainer molecule = new DeNovoStructureUtils().perceiveAromaticityOnSMILES(smiles);
        if (Objects.isNull(molecule)) return null;
        InChI inchi = InChISMILESUtils.getInchiFromSmilesOrThrow(smiles, false);

        FingerprintCandidate fingerprintCandidate = new FingerprintCandidate(
                inchi,
                Objects.requireNonNull(fpMask.mask(fixedFingerprinter.computeFingerprint(molecule)))
        );
        fingerprintCandidate.setSmiles(smiles);
        return fingerprintCandidate;
    }
}
