package de.unijena.bioinf.ms.middleware.compounds;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.PubmedLinks;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.ms.middleware.BaseApiController;
import de.unijena.bioinf.ms.middleware.SiriusContext;
import de.unijena.bioinf.ms.middleware.spectrum.AnnotatedSpectrum;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/projects/{pid}")
public class CompoundController extends BaseApiController {


    @Autowired
    public CompoundController(SiriusContext context) {
        super(context);
    }

    @GetMapping(value = "/compounds", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<CompoundId> getCompoundIds(@PathVariable String pid, @RequestParam(required = false) boolean includeSummary, @RequestParam(required = false) boolean includeMsData) {
        final SiriusProjectSpace space = projectSpace(pid);
        LoggerFactory.getLogger(CompoundController.class).info("Started collecting compounds...");

        final ArrayList<CompoundId> compoundIds = new ArrayList<>();
        space.iterator().forEachRemaining(ccid -> compoundIds.add(asCompoundId(ccid, pid, includeSummary, includeMsData)));

        LoggerFactory.getLogger(CompoundController.class).info("Finished parsing compounds...");
        return compoundIds;
    }

    @GetMapping(value = "/compounds/{cid}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public CompoundId getCompound(@PathVariable String pid, @PathVariable String cid, @RequestParam(required = false) boolean includeSummary, @RequestParam(required = false) boolean includeMsData) {
        final SiriusProjectSpace space = projectSpace(pid);
        final CompoundContainerId ccid = space.findCompound(cid).
                orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no Compound with ID '" + cid + "' in project with name '" + pid + "'"));
        return asCompoundId(ccid, pid, includeSummary, includeMsData);

    }


    private CompoundSummary asCompoundSummary(CompoundContainerId cid, String pid) {
        final CompoundSummary cSum = new CompoundSummary();
        try {
            final SiriusProjectSpace space = projectSpace(pid);
            final CompoundContainer c = space.getCompound(cid);
            if (!c.getResults().isEmpty()) {
                SScored<FormulaResult, ? extends FormulaScore> scoredTopHit =
                        space.getFormulaResultsOrderedBy(cid, cid.getRankingScoreType().orElse(SiriusScore.class), FormulaScoring.class).get(0);

                final FormulaResult topHit = scoredTopHit.getCandidate();
                final FormulaScoring scorings = topHit.getAnnotationOrThrow(FormulaScoring.class);

                //add formula summary
                final FormulaResultSummary frs = new FormulaResultSummary();
                cSum.setFormulaResultSummary(frs);

                frs.setMolecularFormula(topHit.getId().getMolecularFormula().toString());
                frs.setAdduct(topHit.getId().getIonType().toString());

                scorings.getAnnotation(SiriusScore.class).
                        ifPresent(sscore -> frs.setSiriusScore(sscore.score()));
                scorings.getAnnotation(IsotopeScore.class).
                        ifPresent(iscore -> frs.setIsotopeScore(iscore.score()));
                scorings.getAnnotation(TreeScore.class).
                        ifPresent(tscore -> frs.setTreeScore(tscore.score()));
                scorings.getAnnotation(ZodiacScore.class).
                        ifPresent(zscore -> frs.setZodiacScore(zscore.score()));

                space.getFormulaResult(topHit.getId(), FTree.class).getAnnotation(FTree.class).
                        ifPresent(fTree -> {
                            final FTreeMetricsHelper metrHelp = new FTreeMetricsHelper(fTree);
                            frs.setNumOfexplainedPeaks(metrHelp.getNumOfExplainedPeaks());
                            frs.setNumOfexplainablePeaks(metrHelp.getNumberOfExplainablePeaks());
                            frs.setTotalExplainedIntensity(metrHelp.getExplainedIntensityRatio());
                            frs.setMedianMassDeviation(metrHelp.getMedianMassDeviation());
                        });

                // fingerid result
                space.getFormulaResult(topHit.getId(), FingerblastResult.class).getAnnotation(FingerblastResult.class).
                        ifPresent(fbres -> {
                            final StructureResultSummary sSum = new StructureResultSummary();
                            cSum.setStructureResultSummary(sSum);

                            if (!fbres.getResults().isEmpty()) {
                                final Scored<CompoundCandidate> can = fbres.getResults().get(0);

                                // scores
                                sSum.setCsiScore(can.getScore());
                                sSum.setSimilarity(Double.NaN); //todo calculate Tanimoto or drop because we have to read another file for that
                                scorings.getAnnotation(ConfidenceScore.class).
                                        ifPresent(cScore -> sSum.setConfidenceScore(cScore.score()));

                                //Structure information
                                //todo ugly workaround until "null" strings are fixed
                                final String n = can.getCandidate().getName();
                                if (n != null && !n.isEmpty() && !n.equals("null"))
                                    sSum.setStructureName(n);

                                sSum.setSmiles(can.getCandidate().getSmiles());
                                sSum.setInchiKey(can.getCandidate().getInchiKey2D());
                                sSum.setXlogP(can.getCandidate().getXlogp());

                                //meta data
                                PubmedLinks pubMedIds = can.getCandidate().getPubmedIDs();
                                if (pubMedIds != null)
                                    sSum.setNumOfPubMedIds(pubMedIds.getNumberOfPubmedIDs());
                            }
                        });

                // canopus results //todo extract useful canopus summary
                space.getFormulaResult(topHit.getId(), CanopusResult.class).getAnnotation(CanopusResult.class).
                        ifPresent(cRes -> {
                            cSum.setCategoryResultSummary(new CategoryResultSummary());
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return cSum;
    }

    private CompoundMsData asCompoundMsData(CompoundContainerId cid, String pid) {
        try {
            CompoundContainer compound = projectSpace(pid).getCompound(cid, Ms2Experiment.class);
            @NotNull Ms2Experiment experiment = compound.getAnnotationOrThrow(Ms2Experiment.class, () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compound with ID '" + compound + "' has no input Data!"));
            return new CompoundMsData(
                    opt(experiment.getMergedMs1Spectrum(), this::asSpectrum),
                    Optional.empty(),
                    experiment.getMs1Spectra().stream().map(this::asSpectrum).collect(Collectors.toList()),
                    experiment.getMs2Spectra().stream().map(this::asSpectrum).collect(Collectors.toList())
            );
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private CompoundId asCompoundId(CompoundContainerId cid, String pid, boolean includeSummary, boolean includeMsData) {
        final CompoundId compoundId = asCompoundId(cid);
        if (includeSummary)
            compoundId.setSummary(asCompoundSummary(cid, pid));
        if (includeMsData)
            compoundId.setMsData(asCompoundMsData(cid, pid));
        return compoundId;
    }

    private CompoundId asCompoundId(CompoundContainerId cid) {
        return new CompoundId(
                cid.getDirectoryName(),
                cid.getCompoundName(),
                cid.getCompoundIndex(),
                cid.getIonMass().orElse(0d),
                cid.getIonType().map(PrecursorIonType::toString).orElse(null)
        );
    }

    private <S, T> Optional<T> opt(S input, Function<S, T> convert) {
        return Optional.ofNullable(input).map(convert);
    }

    private AnnotatedSpectrum asSpectrum(Spectrum<Peak> spec) {
        return new AnnotatedSpectrum(Spectrums.copyMasses(spec), Spectrums.copyIntensities(spec), new HashMap<>());
    }


}

