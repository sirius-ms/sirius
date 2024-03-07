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

package de.unijena.bioinf.ms.frontend.subtools.sirius;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.CandidateFormulas;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.chemdb.annotations.FormulaSearchDB;
import de.unijena.bioinf.fingerid.FormulaWhiteListJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.projectspace.FormulaResultRankingScore;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Ms1Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SiriusSubToolJob extends InstanceJob {
//    JobProgressMerger merger = new JobProgressMerger(pcs);
    public SiriusSubToolJob(JobSubmitter jobSubmitter) {
        super(jobSubmitter);
    }

    @Override
    protected boolean needsMs2() {
        return false;
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return inst.loadCompoundContainer().hasResults();
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        final Ms2Experiment exp = inst.getExperiment();
        // set whiteSet or merge with whiteSet from db search if available
        CandidateFormulas wSet = null;

        {
            checkForInterruption();

            // create WhiteSet from DB if necessary
            final Optional<FormulaSearchDB> searchDB = exp.getAnnotation(FormulaSearchDB.class);
            if (searchDB.isPresent() && !searchDB.get().isEmpty())
                wSet = submitSubJob(FormulaWhiteListJob.create(ApplicationCore.WEB_API.getChemDB(), searchDB.get().searchDBs, exp, detectPossibleAdducts(exp), true))
                        .awaitResult();

            checkForInterruption();


            if (exp.getAnnotation(CandidateFormulas.class).map(CandidateFormulas::notEmpty).orElse(false)) {
                final CandidateFormulas userFormulas = exp.getAnnotation(CandidateFormulas.class).orElseThrow();
                if (wSet != null)
                    wSet.addAndMerge(userFormulas);
                else
                    wSet = userFormulas;
            }
            if (wSet != null) exp.setAnnotation(CandidateFormulas.class, wSet);
        }
        updateProgress(5);
        checkForInterruption();
        //todo improve progress with progress merger
        final Sirius sirius = ApplicationCore.SIRIUS_PROVIDER.sirius(inst.loadCompoundContainer(FinalConfig.class).getAnnotationOrThrow(FinalConfig.class).config.getConfigValue("AlgorithmProfile"));
        Sirius.SiriusIdentificationJob idjob = sirius.makeIdentificationJob(exp);
        idjob.addJobProgressListener(evt -> updateProgress(evt.getMinValue() + 5, evt.getMaxValue() + 10, evt.getProgress() + 5));
        List<IdentificationResult<SiriusScore>> results = submitSubJob(idjob).awaitResult();

//        updateProgress(90, 110);
        checkForInterruption();

        //write results to project space
        for (IdentificationResult<SiriusScore> result : results)
            inst.newFormulaResultWithUniqueId(result.getTree());

//        checkForInterruption();

        // set sirius to ranking score
        if (exp.getAnnotation(FormulaResultRankingScore.class).orElse(FormulaResultRankingScore.AUTO).isAuto())
            inst.getID().setRankingScoreTypes(new ArrayList<>(List.of(SiriusScore.class)));

        updateProgress(currentProgress().getProgress() + 3);
        checkForInterruption();

        //make possible adducts persistent without rewriting whole experiment
        inst.getID().setDetectedAdducts(exp.getAnnotationOrNull(DetectedAdducts.class));
        inst.updateCompoundID();
        updateProgress(currentProgress().getProgress() + 2);

//        updateProgress(99);
    }

    //todo remove when detection is always performed at import and this is not necessary anymore
    protected PrecursorIonType[] detectPossibleAdducts(Ms2Experiment experiment) {
        final Ms1Preprocessor pp = ApplicationCore.SIRIUS_PROVIDER.sirius().getMs1Preprocessor();
        Ms2Experiment me = new MutableMs2Experiment(experiment, true);
        if (me.hasAnnotation(DetectedAdducts.class)) {
            //copy DetectedAdducts, so the following preprocess does not already alter this annotation. (not 100% sure if this is needed here)
            DetectedAdducts detectedAdducts = me.getAnnotationOrNull(DetectedAdducts.class);
            DetectedAdducts daWithoutMS1Detect = new DetectedAdducts();
            detectedAdducts.getSourceStrings().stream().forEach(s -> daWithoutMS1Detect.put(s, detectedAdducts.get(s)));
            me.setAnnotation(DetectedAdducts.class, daWithoutMS1Detect);
        }
        ProcessedInput pi = pp.preprocess(me);
        return pi.getAnnotation(PossibleAdducts.class).orElseGet(() -> {
            LoggerFactory.getLogger(SiriusSubToolJob.class).error("Could not detect adducts. Molecular formula candidate list may be affected and incomplete.");
            return experiment.getPossibleAdductsOrFallback();
        }).getAdducts().toArray(l -> new PrecursorIonType[l]);
    }

    @Override
    public String getToolName() {
        return PicoUtils.getCommand(SiriusOptions.class).name();
    }
}
