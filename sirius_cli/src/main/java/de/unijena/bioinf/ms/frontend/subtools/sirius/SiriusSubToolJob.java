/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.sirius;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.chemdb.annotations.FormulaSearchDB;
import de.unijena.bioinf.fingerid.FormulaWhiteListJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResultRankingScore;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SiriusSubToolJob extends InstanceJob {
    //todo this is only a temprary solution. parameters should be annotated to the exp
    // we do not want to have oure hol tool management to be dependent on a cli parsing library
    protected final SiriusOptions cliOptions;

    public SiriusSubToolJob(SiriusOptions cliOptions, JobSubmitter jobSubmitter) {
        super(jobSubmitter);
        this.cliOptions = cliOptions;
    }


    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return !inst.loadCompoundContainer().getResults().isEmpty();
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        final Ms2Experiment exp = inst.getExperiment();
        // set whiteSet or merge with whiteSet from db search if available
        Whiteset wSet = null;

        // create WhiteSet from DB if necessary
        //todo do we really want to restrict to organic even if the db is user selected
        final Optional<FormulaSearchDB> searchDB = exp.getAnnotation(FormulaSearchDB.class);
        if (searchDB.isPresent() && searchDB.get().containsDBs()) {
            FormulaWhiteListJob wsJob = new FormulaWhiteListJob(ApplicationCore.WEB_API.getChemDB(), searchDB.get().searchDBs, exp, true, false);
            wSet = SiriusJobs.getGlobalJobManager().submitJob(wsJob).awaitResult();
        }

        // todo this should be moved to annotations at some point.
        // so that the cli parser dependency can be removed
        if (cliOptions.formulaWhiteSet != null) {
            if (wSet != null)
                wSet = wSet.add(cliOptions.formulaWhiteSet);
            else
                wSet = cliOptions.formulaWhiteSet;
        }

        exp.setAnnotation(Whiteset.class, wSet);

        final Sirius sirius = ApplicationCore.SIRIUS_PROVIDER.sirius(exp.getAnnotationOrThrow(FinalConfig.class).config.getConfigValue("AlgorithmProfile"));
        List<IdentificationResult<SiriusScore>> results = SiriusJobs.getGlobalJobManager().submitJob(sirius.makeIdentificationJob(exp)).awaitResult();

        //write results to project space
        for (IdentificationResult<SiriusScore> result : results)
            inst.newFormulaResultWithUniqueId(result.getTree());

        // set sirius to ranking score
        if (exp.getAnnotation(FormulaResultRankingScore.class).orElse(FormulaResultRankingScore.AUTO).isAuto())
            inst.getID().setRankingScoreTypes(new ArrayList<>(List.of(SiriusScore.class)));

        //make possible adducts persistent without rewriting whole experiment
        inst.getID().setDetectedAdducts(exp.getAnnotationOrNull(DetectedAdducts.class));
        inst.updateCompoundID();
    }

    @Override
    public String getToolName() {
        return PicoUtils.getCommand(SiriusOptions.class).name();
    }
}
