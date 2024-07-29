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
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.elgordo.LipidStructureMatcher;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.annotations.AnnotationJJob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class FingerblastSearchJJob extends FingerprintDependentJJob<FingerblastResult> implements AnnotationJJob<FingerblastResult, FingerIdResult> {

    private final CSIPredictor predictor;

    protected BayesnetScoring bayesnetScoring = null;
    private WebWithCustomDatabase.CandidateResult candidates = null;
    private List<Scored<FingerprintCandidate>> scoredCandidates = null;

    public FingerblastSearchJJob(@NotNull CSIPredictor predictor) {
        this(predictor, null, null, null);
    }

    public FingerblastSearchJJob(@NotNull CSIPredictor predictor, @Nullable BayesnetScoring bayesnetScoring) {
        this(predictor);
        this.bayesnetScoring = bayesnetScoring;
    }

    public FingerblastSearchJJob(@NotNull CSIPredictor predictor, FTree tree, ProbabilityFingerprint fp, MolecularFormula formula) {
        super(JobType.CPU, fp, formula, tree);
        this.predictor = predictor;
    }

    protected void checkInput() {
        if (candidates == null)
            throw new IllegalArgumentException("No Input Data found.");
    }

    @Override
    public void handleFinishedRequiredJob(JJob required) {
        super.handleFinishedRequiredJob(required);
        if (candidates == null) {
            if (required instanceof FormulaJob) {
                FormulaJob job = ((FormulaJob) required);
                candidates = job.result();
            }
        }
    }

    public List<Scored<FingerprintCandidate>> getAllScoredCandidates() {
        return scoredCandidates;
    }

    public WebWithCustomDatabase.CandidateResult getCandidates() {
        return candidates;
    }

    @Override
    protected FingerblastResult compute() throws Exception {
        checkInput();
        //we want to score all available candidates and may create subsets later.
        final Set<FingerprintCandidate> combinedCandidates = candidates.getCombCandidates();
        checkForInterruption();

        // to get a prepared FingerblastScorer, an object of BayesnetScoring that is specific to the molecular formula has to be initialized
        List<JJob<List<Scored<FingerprintCandidate>>>> scoreJobs = Fingerblast.makeScoringJobs(
                predictor.getPreparedFingerblastScorer(ParameterStore.of(fp, bayesnetScoring)), combinedCandidates, fp);
        checkForInterruption();
        scoreJobs.forEach(this::submitSubJob);
        checkForInterruption();
        //This sorting here needs to be consistent with the sorting everywhere else
        scoredCandidates = scoreJobs.stream().flatMap(r -> r.takeResult().stream()).sorted(Comparator.<Scored<FingerprintCandidate>>reverseOrder().thenComparing((Scored<FingerprintCandidate> s) -> s.getCandidate().getInchiKey2D())).map(fpc -> new Scored<>(fpc.getCandidate(), fpc.getScore())).collect(Collectors.toList());
        checkForInterruption();
        scoredCandidates.forEach(sc -> postprocessCandidate(sc.getCandidate()));
        checkForInterruption();

        //create filtered result for FingerblastResult result
        Set<String> requestedCandidatesInChIs = candidates.getReqCandidatesInChIs();
        checkForInterruption();
        final List<Scored<FingerprintCandidate>> cds = scoredCandidates.stream().
                filter(sc -> requestedCandidatesInChIs.contains(sc.getCandidate().getInchiKey2D())).collect(Collectors.toList());
        checkForInterruption();

        if (this.ftree != null) {
            if (ftree.getAnnotation(LipidSpecies.class).isPresent()) {
                final LipidSpecies l = ftree.getAnnotationOrThrow(LipidSpecies.class);
                final List<DBLink> elGordoLink = List.of(new DBLink(DataSource.LIPID.name(), l.toString()));

                List<BasicJJob<FingerprintCandidate>> lipidAnoJobs = scoredCandidates.stream().map(SScored::getCandidate).map(c -> new BasicJJob<FingerprintCandidate>() {
                    @Override
                    protected FingerprintCandidate compute() throws Exception {
                        checkForInterruption();
                        IAtomContainer molecule = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(c.getSmiles());
                        checkForInterruption();
                        LipidStructureMatcher m = new LipidStructureMatcher(l.getLipidClass(), molecule);
                        checkForInterruption();
                        if (m.isMatched()) {
                            c.mergeBits(DataSource.LIPID.flag);
                            c.mergeDBLinks(elGordoLink);
                        }
                        return c;
                    }
                }).collect(Collectors.toList());
                checkForInterruption();
                submitSubJobsInBatchesByThreads(lipidAnoJobs, jobManager.getCPUThreads()).forEach(j -> {
                    try {
                        j.awaitResult();
                    } catch (ExecutionException e) {
                        logWarn("Error when annotating checking if candidate belongs to lipid class. Annotations might be incomplete!");
                    }
                });
            }
        }
        return new FingerblastResult(cds);
    }
    protected void postprocessCandidate(CompoundCandidate candidate) {
        //annotate training compounds;
        if (predictor.getTrainingStructures().isInTrainingData(candidate.getInchi())) {
            long flags = candidate.getBitset();
            candidate.setBitset(flags | DataSource.TRAIN.flag);
        }
    }

    public static FingerblastSearchJJob of(@NotNull CSIPredictor predictor, @NotNull FingerIdResult input) {
        return of(predictor, null, input);
    }

    public static FingerblastSearchJJob of(@NotNull CSIPredictor predictor, @Nullable BayesnetScoring bayesnetScoring, @NotNull FingerIdResult input) {
        FingerblastSearchJJob nu = new FingerblastSearchJJob(predictor, bayesnetScoring);
        nu.setFormula(input.getMolecularFormula());
        nu.setFtree(input.getSourceTree());
        nu.setFingerprint(input.getPredictedFingerprint());
        return nu;
    }

    @Override
    public String identifier() {
        String filePath = ftree==null ? "Null" : ftree.getAnnotation(de.unijena.bioinf.ChemistryBase.data.DataSource.class).orElse(new de.unijena.bioinf.ChemistryBase.data.DataSource(new File(""))).toString();
        return super.identifier() + " | " + filePath;
    }
}
