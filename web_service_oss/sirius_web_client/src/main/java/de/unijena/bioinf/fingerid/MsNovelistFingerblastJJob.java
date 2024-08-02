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

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.InChISMILESUtils;
import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import de.unijena.bioinf.fingerid.fingerprints.cache.IFingerprinterCache;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.Partition;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.ms.rest.model.msnovelist.MsNovelistCandidate;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.webapi.WebAPI;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.*;
import java.util.stream.Collectors;

public class MsNovelistFingerblastJJob extends BasicMasterJJob<List<Scored<FingerprintCandidate>>> {

    private final WebAPI<?> webAPI;
    private final CSIPredictor predictor;
    private final IFingerprinterCache cache;
    // input data
    private FingerIdResult idResult;
    private List<MsNovelistCandidate> candidates;

    public MsNovelistFingerblastJJob(@NotNull CSIPredictor predictor, @NotNull WebAPI<?> webAPI, @Nullable IFingerprinterCache cache, @Nullable FingerIdResult idResult, @Nullable List<MsNovelistCandidate> candidates) {
        super(JobType.SCHEDULER);
        this.predictor = predictor;
        this.idResult = idResult;
        this.webAPI = webAPI;
        this.cache = cache == null ? IFingerprinterCache.NOOP_CACHE : cache;
        this.candidates = candidates;
    }

    public void setInput(FingerIdResult idResult, List<MsNovelistCandidate> candidates) {
        notSubmittedOrThrow();
        this.idResult = idResult;
        this.candidates = candidates;
    }

    public void setFingerIdResult(FingerIdResult idResult) {
        notSubmittedOrThrow();
        this.idResult = idResult;
    }

    public void setMsNovelistCandidates(List<MsNovelistCandidate> candidates) {
        notSubmittedOrThrow();
        this.candidates = candidates;
    }

    protected void checkInput() {
        if (idResult == null)
            throw new IllegalArgumentException("No Input Data found.");
        if (candidates == null)
            throw new IllegalArgumentException("No MSNovelist predictions found.");
    }

    @Override
    protected List<Scored<FingerprintCandidate>> compute() throws Exception {

        checkInput();

        if (candidates.isEmpty())
            return null;

        //filter 2d duplicates
        // needed for fingerprinting and FingerprintCandidate generation
        final FingerIdData fingerIdData = idResult.getPrecursorIonType().getCharge() > 0
                ? NetUtils.tryAndWait(() -> webAPI.getFingerIdData(PredictorType.CSI_FINGERID_POSITIVE), this::checkForInterruption)
                : NetUtils.tryAndWait(() -> webAPI.getFingerIdData(PredictorType.CSI_FINGERID_NEGATIVE), this::checkForInterruption);
        final MaskedFingerprintVersion fpMask = fingerIdData.getFingerprintVersion();

        final FixedFingerprinter fixedFingerprinter = new FixedFingerprinter(fingerIdData.getCdkFingerprintVersion(), cache);

        // needed to perceive aromaticity
        final CDKHydrogenAdder hydrogenAdder = CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance());
        final CycleFinder cycles = Cycles.or(Cycles.all(), Cycles.all(6));
        final Aromaticity aromaticity = new Aromaticity(ElectronDonation.daylight(), cycles);

        checkForInterruption();

        final FormulaJob formulaJobs = new FormulaJob(
                idResult.getMolecularFormula(),
                predictor.database,
                CustomDataSources.getSources(),
                idResult.getPrecursorIonType(),
                true);

        checkForInterruption();

        //no error handling since this info is optional
        WebWithCustomDatabase.CandidateResult candidateResult = submitJob(formulaJobs).getResult();
        Map<String, FingerprintCandidate> dbCandidates = candidateResult == null ? Map.of() : candidateResult.getCombCandidatesStr().collect(Collectors.toMap(CompoundCandidate::getInchiKey2D, c -> c));

        checkForInterruption();

        // create and submit jobs for transformation and fingerprinting
        final Collection<BasicJJob<List<Pair<FingerprintCandidate, MsNovelistCandidate>>>> candidateJobs =
                new ArrayList<>(Partition.ofSize(candidates, SiriusJobs.getCPUThreads()).stream()
                        .map(l -> new BasicJJob<List<Pair<FingerprintCandidate, MsNovelistCandidate>>>(JobType.CPU) {
                            @Override
                            protected List<Pair<FingerprintCandidate, MsNovelistCandidate>> compute() {
                                List<Pair<FingerprintCandidate, MsNovelistCandidate>> result = new ArrayList<>(l.size());
                                l.forEach(candidate -> {
                                    InChI inchi = InChISMILESUtils.getInchiFromSmilesOrThrow(candidate.getSmiles(), false);
                                    FingerprintCandidate fingerprintCandidate = dbCandidates.get(inchi.key2D());

                                    if (fingerprintCandidate == null) {
                                        IAtomContainer molecule = perceiveAromaticityOnSMILES(
                                                candidate.getSmiles(), hydrogenAdder, aromaticity);
                                        if (Objects.isNull(molecule)) return;

                                        fingerprintCandidate = new FingerprintCandidate(
                                                inchi,
                                                Objects.requireNonNull(fpMask.mask(fixedFingerprinter.computeFingerprint(molecule)))
                                        );
                                        fingerprintCandidate.setSmiles(candidate.getSmiles());
                                    }
                                    result.add(Pair.of(fingerprintCandidate, candidate));
                                });
                                return result;
                            }
                        }).peek(this::submitSubJob).toList());

        checkForInterruption();


        // collect job results to turn MSNovelist candidate list into scoring-compatible FingerprintCandidates
        // if two 3d structures with same 2d inchi we keep the one that has higher rnn scorem because the csi score should be identical
        Map<String, FingerprintCandidate> combinedCandidates = new HashMap<>();
        Map<String, MsNovelistCandidate> inchiKeyToCandidate = new HashMap<>();
        candidateJobs.stream().map(JJob::takeResult).flatMap(List::stream).forEach(p -> {
            MsNovelistCandidate mn2 = inchiKeyToCandidate.putIfAbsent(p.key().getInchiKey2D(), p.value());
            combinedCandidates.putIfAbsent(p.key().getInchiKey2D(), p.key());

            if (mn2 != null && mn2.getRnnScore() < p.value().getRnnScore()) {
                inchiKeyToCandidate.put(p.key().getInchiKey2D(), p.value());
                combinedCandidates.put(p.key().getInchiKey2D(), p.key());
            }
        });

        checkForInterruption();

        // check if no candidate was valid
        if (combinedCandidates.isEmpty()) return null;

        checkForInterruption();

        // try and get bayessian network (covTree) for molecular formula
        BayesnetScoring bayesnetScoring = NetUtils.tryAndWait(() -> webAPI.getBayesnetScoring(
                predictor.predictorType,
                webAPI.getFingerIdData(predictor.predictorType),
                idResult.getMolecularFormula()), this::checkForInterruption);

        checkForInterruption();

        // bayesnetScoring is null --> make a job which computes the bayessian network (covTree) for the
        // given molecular formula
        //todo can probably be optimized by starting them before other local computations
        if (bayesnetScoring == null) {
            WebJJob<CovtreeJobInput, ?, BayesnetScoring, ?> covTreeJob =
                    webAPI.submitCovtreeJob(idResult.getMolecularFormula(), predictor.predictorType);
            bayesnetScoring = covTreeJob.awaitResult();
        }

        checkForInterruption();

        // to get a prepared FingerblastScorer, an object of BayesnetScoring that is specific to the molecular formula
        // has to be initialized
        ProbabilityFingerprint fp = idResult.getPredictedFingerprint();
        List<JJob<List<Scored<FingerprintCandidate>>>> scoreJobs = Fingerblast.makeScoringJobs(
                predictor.getPreparedFingerblastScorer(ParameterStore.of(fp, bayesnetScoring)), combinedCandidates.values(), fp);

        checkForInterruption();
        scoreJobs.forEach(this::submitSubJob);
        checkForInterruption();

        List<Scored<FingerprintCandidate>> scoredCandidates = scoreJobs.stream().flatMap(r -> r.takeResult().stream())
                .sorted(Comparator.reverseOrder())
                .map(fpc -> new Scored<>(fpc.getCandidate(), fpc.getScore())).collect(Collectors.toList());

        checkForInterruption();

        // annotate result of BayesNet scoring
        idResult.annotate(new MsNovelistFingerblastResult(scoredCandidates, scoredCandidates.stream()
                .map(SScored::getCandidate).map(FingerprintCandidate::getInchiKey2D).map(inchiKeyToCandidate::get)
                .mapToDouble(MsNovelistCandidate::getRnnScore).toArray()));
        return scoredCandidates;
    }

    public IAtomContainer perceiveAromaticityOnSMILES(@NotNull String smiles, @NotNull CDKHydrogenAdder hydrogenAdder, @NotNull Aromaticity aromaticity) {
        IAtomContainer molecule = FixedFingerprinter.parseStructureFromStandardizedSMILES(smiles);
        try {
            aromaticity.apply(molecule);
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
            hydrogenAdder.addImplicitHydrogens(molecule);
        } catch (CDKException e) {
            e.printStackTrace();
            return null;
        }
        return molecule;
    }
}
