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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.CandidateFormulas;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.chemdb.annotations.FormulaSearchDB;
import de.unijena.bioinf.fingerid.FormulaWhiteListJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.spectraldb.InjectSpectralLibraryMatchFormulas;
import de.unijena.bioinf.spectraldb.SpectraMatchingJJob;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import de.unijena.bioinf.spectraldb.SpectralSearchResults;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SiriusSubToolJob extends InstanceJob {
    public SiriusSubToolJob(JobSubmitter jobSubmitter) {
        super(jobSubmitter);
    }

    @Override
    protected boolean needsMs2() {
        return false;
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return inst.hasSiriusResult();
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        MutableMs2Experiment mut = inst.getExperiment().mutate();
        List<SpectralSearchResult.SearchResult> searchResults = inst.getSpectraMatches();
        // set whiteSet or merge with whiteSet from db search if available
        CandidateFormulas wSet = null;
        final Ms2Experiment exp;

        {
            checkForInterruption();

            // If Ms1 only, IonMass is NaN here. That is getting fixed later in Ms2Validator, but we already need it for DB whitelists
            if(mut.getMs2Spectra().isEmpty() && !mut.getMergedMs1Spectrum().isEmpty()){
                final Spectrum<Peak> ms1 = mut.getMergedMs1Spectrum();
                int index = Spectrums.getIndexOfPeakWithMaximalIntensity(ms1);
                // move backward, maybe you are in the middle of an isotope pattern
                while (index > 0) {
                    if (Math.abs(ms1.getMzAt(index) - ms1.getMzAt(index - 1)) > 1.1d) break;
                    --index;
                }
                mut.setIonMass(ms1.getMzAt(index));
            }
            exp = mut;

            // extract additional candidates from library matches
            if (searchResults != null && !searchResults.isEmpty()){
                //add adduct and formula from high-scoring library hits to detected adducts and formula candiadates list
                InjectSpectralLibraryMatchFormulas injectFormulas = exp.getAnnotationOrDefault(InjectSpectralLibraryMatchFormulas.class);
                //todo Adducts from spectral library search are not stored persistently, when implementing library search 2.0 we have to evaluate whether we want to do this.
                addAdductsAndFormulasFromHighScoringLibraryMatches(exp, searchResults, injectFormulas.getMinScoreToInject(), injectFormulas.getMinPeakMatchesToInject());
            }

            // create WhiteSet from DB if necessary
            final Optional<FormulaSearchDB> searchDB = exp.getAnnotation(FormulaSearchDB.class);
            if (searchDB.isPresent() && !searchDB.get().isEmpty())
                wSet = submitSubJob(FormulaWhiteListJob
                        .create(ApplicationCore.WEB_API.getChemDB(), searchDB.get().searchDBs, exp))
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

//        if (adductsOnlyMulti(exp)) {
//            logError("Skipping instance " + inst.getId() +": SIRIUS does not support multimere or multiple charged adducts.");
//            return;
//        }

        //todo improve progress with progress merger
        final Sirius sirius = ApplicationCore.SIRIUS_PROVIDER.sirius(inst.loadProjectConfig()
                .map(c -> c.getConfigValue("AlgorithmProfile")).orElse(null));

        Sirius.SiriusIdentificationJob idjob = sirius.makeIdentificationJob(exp);
        idjob.addJobProgressListener(evt -> updateProgress(evt.getMinValue() + 5, evt.getMaxValue() + 10, evt.getProgress() + 5));
        List<IdentificationResult> results = submitSubJob(idjob).awaitResult();

        checkForInterruption();

        //write results to project space
        inst.saveSiriusResult(results);

        updateProgress(currentProgress().getProgress() + 3);
        checkForInterruption();

        //make possible adducts persistent without rewriting whole experiment
        exp.getAnnotation(DetectedAdducts.class).ifPresent(detectedAdducts -> {
            Map<DetectedAdducts.Source, Iterable<PrecursorIonType>> subsetToAdd = new HashMap<>(detectedAdducts);
            subsetToAdd.entrySet().removeIf(e -> e.getKey() != DetectedAdducts.Source.MS1_PREPROCESSOR && e.getKey() != DetectedAdducts.Source.SPECTRAL_LIBRARY_SEARCH);
            if (!subsetToAdd.isEmpty())
                inst.addAndSaveAdductsBySource(subsetToAdd);
        });
        updateProgress(currentProgress().getProgress() + 2);
    }

    private void addAdductsAndFormulasFromHighScoringLibraryMatches(Ms2Experiment exp, List<SpectralSearchResult.SearchResult> searchResults, double minSimilarity, int minSharedPeaks) {
        final DetectedAdducts detAdds = exp.computeAnnotationIfAbsent(DetectedAdducts.class, DetectedAdducts::new);
        Set<PrecursorIonType> adducts = SpectralSearchResults.deriveDistinctAdductsSetWithThreshold(searchResults, exp.getIonMass(), minSimilarity, minSharedPeaks);
        if (adducts.isEmpty()) return;

        PossibleAdducts possibleAdducts = new PossibleAdducts(adducts);
        //overrides any detected addcuts from previous spectral library searches for consistency reasons. alternatively, we could use union.
        detAdds.put(DetectedAdducts.Source.SPECTRAL_LIBRARY_SEARCH, possibleAdducts);

        //set high-scoring formulas
        Set<MolecularFormula> formulas = SpectralSearchResults.deriveDistinctFormulaSetWithThreshold(searchResults, exp.getIonMass(), minSimilarity, minSharedPeaks);
        if (formulas.isEmpty()) return;

        CandidateFormulas candidateFormulas = exp.computeAnnotationIfAbsent(CandidateFormulas.class);
        candidateFormulas.addAndMergeSpectralLibrarySearchFormulas(formulas, SpectraMatchingJJob.class);
    }

    private boolean adductsOnlyMulti(Ms2Experiment exp) {
        // TODO what about fallback/enforced?
        final DetectedAdducts detAdds = exp.computeAnnotationIfAbsent(DetectedAdducts.class, DetectedAdducts::new);
        Set<PrecursorIonType> adducts = detAdds.getAllAdducts().getAdducts();
        final AdductSettings settings = exp.getAnnotationOrDefault(AdductSettings.class);
        return adducts.stream().filter(ion -> !ion.isIonizationUnknown()).allMatch(ion -> ion.isMultimere() || ion.isMultipleCharged());
    }

    @Override
    public String getToolName() {
        return PicoUtils.getCommand(SiriusOptions.class).name();
    }
}
