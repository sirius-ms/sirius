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

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.annotations.StructureSearchDB;
import de.unijena.bioinf.confidence_score.ConfidenceScorer;
import de.unijena.bioinf.fingerid.blast.ScoringMethodFactory;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import de.unijena.bioinf.fragmenter.CombinatorialFragment;
import de.unijena.bioinf.fragmenter.CombinatorialSubtree;
import de.unijena.bioinf.jjobs.BasicDependentMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Created by martin on 08.08.18.
 */
public class ConfidenceJJob extends BasicDependentMasterJJob<ConfidenceResult> {

    //fina inputs
    protected final ConfidenceScorer confidenceScorer;
    protected final Ms2Experiment experiment;

    private final static SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());


    // inputs that can be either set by a setter or from dependent jobs
    protected final ScoringMethodFactory.CSIFingerIdScoringMethod csiScoring;

    protected CanopusResult canopusResultRequested;

    protected CanopusResult canopusResultTopHit;

    protected ParameterStore parameterStoreRequested;

    protected final int mcesIndex;

    protected StructureSearchDB searchDB;

    final ArrayList<Scored<FingerprintCandidate>> allMergedCandidates;
    final ArrayList<Scored<FingerprintCandidate>> requestedMergedCandidates;

    final ArrayList<Scored<FingerprintCandidate>> requestedMergedCandidatesMCESCondensed;


    protected CombinatorialSubtree[] epiTreesExact;
    protected CombinatorialSubtree[] epiTreesApprox;

    protected FTree[] fTreesExact;
    protected FTree[] fTreesApprox;

    protected Map<Fragment, ArrayList<CombinatorialFragment>>[] originalMappingsExact;

    protected Map<Fragment, ArrayList<CombinatorialFragment>>[] originalMappingsApprox;


    protected Supplier<Map<FTree, SubstructureAnnotationResult>> epiExact;
    protected Supplier<Map<FTree, SubstructureAnnotationResult>> epiApprox;

    //INPUT
    // puchem resultlist
    // filterflag oder filtered list
    // ConfidenceScoreComputer
    // Scorings: CovarianceScoring, CSIFingerIDScoring (reuse)
    // IdentificationResult
    // Experiment -> CollisionEnergies

    //OUTPUT
    // ConfidenceResult -> Annotate to

    public ConfidenceJJob(@NotNull CSIPredictor predictor,
                          Ms2Experiment experiment,
                          ArrayList<Scored<FingerprintCandidate>> allMergedCandidates,
                          ArrayList<Scored<FingerprintCandidate>> requestedMergedCandidates,
                          ArrayList<Scored<FingerprintCandidate>> requestedMergedCandidatesMCESCondensed,
                          StructureSearchDB searchDB,
                          ParameterStore parameterStore,
                          CanopusResult canopusResult, CanopusResult canopusResultTopHit, int mcesIndex) {
        super(JobType.CPU);
        this.confidenceScorer = predictor.getConfidenceScorer();
        this.csiScoring = new ScoringMethodFactory.CSIFingerIdScoringMethod(predictor.performances);
        this.experiment = experiment;
        this.parameterStoreRequested=parameterStore;
        this.searchDB=searchDB;
        this.canopusResultRequested=canopusResult;
        this.allMergedCandidates=allMergedCandidates;
        this.requestedMergedCandidates=requestedMergedCandidates;
        this.requestedMergedCandidatesMCESCondensed=requestedMergedCandidatesMCESCondensed;
        this.mcesIndex=mcesIndex;
        this.canopusResultTopHit = canopusResultTopHit;

    }




    @Override
    public void handleFinishedRequiredJob(JJob required) {}


    public void setEpiExact(Supplier<Map<FTree, SubstructureAnnotationResult>> epiExact) {
        this.epiExact = epiExact;
    }

    public void setEpiApprox(Supplier<Map<FTree, SubstructureAnnotationResult>> epiApprox) {
        this.epiApprox = epiApprox;
    }

    public void setCanopusResultTopHit(CanopusResult canopusResultTopHit) {
        this.canopusResultTopHit = canopusResultTopHit;
    }

    @Override
    protected ConfidenceResult compute() throws Exception {
        checkForInterruption();
        if (this.requestedMergedCandidates.isEmpty())
            return new ConfidenceResult(Double.NaN, Double.NaN, null);


            parseEpiResults();


            boolean structureSearchDBIsPubChem = ((searchDB.getDBFlag() & 2L) !=0)? true : false;


            checkForInterruption();


            //compute exact confidence
            final double score = confidenceScorer.computeConfidence(experiment,
                    allMergedCandidates,
                    requestedMergedCandidates,parameterStoreRequested
                    , structureSearchDBIsPubChem,fTreesExact,epiTreesExact,originalMappingsExact,canopusResultRequested,canopusResultTopHit);


            //compute approximate confidence


            final double scoreApproximate = confidenceScorer.computeConfidence(experiment,
                    allMergedCandidates,
                    requestedMergedCandidatesMCESCondensed,parameterStoreRequested
                    , structureSearchDBIsPubChem,fTreesApprox,epiTreesApprox,originalMappingsApprox,canopusResultRequested,canopusResultTopHit);


            checkForInterruption();
            return new ConfidenceResult(score, scoreApproximate, requestedMergedCandidates.size() > 0 ? requestedMergedCandidates.get(0) : null);
        }


    private void parseEpiResults(){

        Map<FTree, SubstructureAnnotationResult> epiApproxMap = epiApprox.get();
        Map<FTree, SubstructureAnnotationResult> epiExactMap = epiExact.get();

        fTreesExact = requestedMergedCandidates.size()>=5 ? new FTree[5] : requestedMergedCandidates.size()>=2 ? new FTree[2] : requestedMergedCandidates.size()>=1 ? new FTree[1] : null;
        fTreesApprox = requestedMergedCandidatesMCESCondensed.size()>=5 ? new FTree[5] : requestedMergedCandidatesMCESCondensed.size()>=2 ? new FTree[2] : requestedMergedCandidatesMCESCondensed.size()>=1 ? new FTree[1] : null;

        epiTreesExact = requestedMergedCandidates.size()>=5 ? new CombinatorialSubtree[5] : requestedMergedCandidates.size()>=2 ? new CombinatorialSubtree[2] : requestedMergedCandidates.size()>=1 ? new CombinatorialSubtree[1] : null;
        epiTreesApprox = requestedMergedCandidatesMCESCondensed.size()>=5 ? new CombinatorialSubtree[5] : requestedMergedCandidatesMCESCondensed.size()>=2 ? new CombinatorialSubtree[2] : requestedMergedCandidatesMCESCondensed.size()>=1 ? new CombinatorialSubtree[1] : null;

        originalMappingsExact = requestedMergedCandidates.size()>=5 ? new Map[5] : requestedMergedCandidates.size()>=2 ? new Map[2] : requestedMergedCandidates.size()>=1 ? new Map[1] : null;
        originalMappingsApprox = requestedMergedCandidatesMCESCondensed.size()>=5 ? new Map[5] : requestedMergedCandidatesMCESCondensed.size()>=2 ? new Map[2] : requestedMergedCandidatesMCESCondensed.size()>=1 ? new Map[1] : null;





        for (FTree tree : epiExactMap.keySet()){
            for(String key : epiExactMap.get(tree).getInchiToFragmentationResult().keySet()){
                CombinatorialSubtree subtree = epiExactMap.get(tree).getInchiToFragmentationResult().get(key).getSubtree();
                Map<Fragment, ArrayList<CombinatorialFragment>> mapping = epiExactMap.get(tree).getInchiToFragmentationResult().get(key).getFragmentMapping();
                for(int i=0;i<requestedMergedCandidates.size();i++){
                    if(requestedMergedCandidates.get(i).getCandidate().getInchiKey2D().equals(key) && i<fTreesExact.length){
                        fTreesExact[i]=tree;
                        epiTreesExact[i]=subtree;
                        originalMappingsExact[i]=mapping;
                    break;
                    }
                }

            }
        }

        for (FTree tree : epiApproxMap.keySet()){
            for(String key : epiApproxMap.get(tree).getInchiToFragmentationResult().keySet()){
                CombinatorialSubtree subtree = epiApproxMap.get(tree).getInchiToFragmentationResult().get(key).getSubtree();
                Map<Fragment, ArrayList<CombinatorialFragment>> mapping = epiApproxMap.get(tree).getInchiToFragmentationResult().get(key).getFragmentMapping();
                for(int i=0;i<requestedMergedCandidatesMCESCondensed.size();i++){
                    if(requestedMergedCandidatesMCESCondensed.get(i).getCandidate().getInchiKey2D().equals(key) && i<fTreesApprox.length){
                        fTreesApprox[i]=tree;
                        epiTreesApprox[i]=subtree;
                        originalMappingsApprox[i]=mapping;
                        break;
                    }
                }

            }
        }
    }
}
