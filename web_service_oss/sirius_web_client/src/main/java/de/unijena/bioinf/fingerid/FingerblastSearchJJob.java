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
import de.unijena.bioinf.ms.webapi.WebJJob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

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
    public synchronized void handleFinishedRequiredJob(JJob required) {
        super.handleFinishedRequiredJob(required);
        if (candidates == null) {
            if (required instanceof FormulaJob) {
                FormulaJob job = ((FormulaJob) required);
                candidates = job.result();
            }
        }

        if (bayesnetScoring == null) {
            if (required instanceof WebJJob) {
                Object r = required.result();
                if (r instanceof BayesnetScoring)
                    bayesnetScoring = (BayesnetScoring) r;
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
    protected FingerblastResult compute() {
        checkInput();
        //we want to score all available candidates and may create subsets later.
        final Set<FingerprintCandidate> combinedCandidates = candidates.getCombCandidates();

        // to get a prepared FingerblastScorer, an object of BayesnetScoring that is specific to the molecular formula has to be initialized
        List<JJob<List<Scored<FingerprintCandidate>>>> scoreJobs = Fingerblast.makeScoringJobs(
                predictor.getPreparedFingerblastScorer(ParameterStore.of(fp, bayesnetScoring)), combinedCandidates, fp);
        scoreJobs.forEach(this::submitSubJob);

        scoredCandidates = scoreJobs.stream().flatMap(r -> r.takeResult().stream()).sorted(Comparator.reverseOrder()).map(fpc -> new Scored<>(fpc.getCandidate(), fpc.getScore())).collect(Collectors.toList());
        scoredCandidates.forEach(sc -> postprocessCandidate(sc.getCandidate()));

        //create filtered result for FingerblastResult result
        Set<String> requestedCandidatesInChIs = candidates.getReqCandidatesInChIs();
        final List<Scored<FingerprintCandidate>> cds = scoredCandidates.stream().
                filter(sc -> requestedCandidatesInChIs.contains(sc.getCandidate().getInchiKey2D())).collect(Collectors.toList());

        if (this.ftree != null) {
            if (ftree.getAnnotation(LipidSpecies.class).isPresent()) {
                final LipidSpecies l = ftree.getAnnotationOrThrow(LipidSpecies.class);
                final List<DBLink> elGordoLink = List.of(new DBLink("El-Gordo", l.toString()));

                List<BasicJJob<FingerprintCandidate>> lipidAnoJobs = scoredCandidates.stream().map(SScored::getCandidate).map(c -> new BasicJJob<FingerprintCandidate>() {
                    @Override
                    protected FingerprintCandidate compute() throws Exception {
                        IAtomContainer molecule = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(c.getSmiles());
                        LipidStructureMatcher m = new LipidStructureMatcher(l.getLipidClass(), molecule);
                        if (m.isMatched()) {
                            System.out.println("Candidate '" + c.getInchi().in2D + "' belongs to '" + l + "'."); //todo remove
                            c.mergeBits(DataSource.ELGORDO.flag);
                            c.mergeDBLinks(elGordoLink);
                        }
                        return c;
                    }
                }).collect(Collectors.toList());

                submitSubJobsInBatchesByThreads(lipidAnoJobs, jobManager.getCPUThreads()).forEach(j -> {
                    try {
                        j.awaitResult();
                    } catch (ExecutionException e) {
                        logWarn("Error when annotating checking if candidate belongs to lipid species. Annotations might be incomplete!");
                    }
                });
            }
        }

        return new FingerblastResult(cds);
    }

    /*private void addOrReplace(Set<FingerprintCandidate> combinedCandidates, FingerprintCandidate x) {
        for (FingerprintCandidate fc : combinedCandidates) {
            if (fc.getInchi().key2D().equals(x.getInchiKey2D())) {
                if (fc.getName() == null || fc.getName().isBlank()) fc.setName(x.getName());
                fc.mergeBits(x.getBitset());
                fc.mergeDBLinks(x.getLinks());
                return; // is already in list
            }
        }
        combinedCandidates.add(x); //add new candidate
    }*/

    /*private FingerprintCandidate lipid2candidate(LipidSpecies lipid) {
        final Optional<String> smiles = lipid.generateHypotheticalStructure();
        return smiles.map(str->{
            try {
                final IAtomContainer molecule = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(str);
                ArrayFingerprint fp = new FixedFingerprinter(CdkFingerprintVersion.getDefault()).computeFingerprintFromSMILES(str);
                final InChIGenerator gen = InChIGeneratorFactory.getInstance().getInChIGenerator(molecule);
                return new FingerprintCandidate(new CompoundCandidate(
                        new InChI(gen.getInchiKey(), gen.getInchi()),
                        lipid.toString(),
                        str,
                        0,
                        0,
                        ((DoubleResult)(new XLogPDescriptor().calculate(molecule).getValue())).doubleValue(),
                        null,
                        DataSource.ELGORDO.flag,
                        new DBLink[]{new DBLink("El-Gordo", lipid.toString())},
                        new PubmedLinks()
                ), fp);
            } catch (CDKException e) {
                LoggerFactory.getLogger(FingerblastSearchJJob.class).error("Error when parsing lipid SMILES " + str, e);
                return null;
            }
        }).orElse(null);
    }
*/
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
}
