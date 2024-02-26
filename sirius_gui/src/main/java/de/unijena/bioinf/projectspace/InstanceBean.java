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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.WrapperSpectrum;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchingResult;
import de.unijena.bioinf.ms.nightsky.sdk.NightSkyClient;
import de.unijena.bioinf.ms.nightsky.sdk.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.unijena.bioinf.projectspace.FormulaResultBean.ensureDefaultOptFields;

/**
 * This is the wrapper for the Instance class to interact with the gui
 * elements. It uses a special property change support that executes the events
 * in the EDT. So you can change all fields from any thread, the gui will still
 * be updated in the EDT. Some operations may NOT be Thread save, so you may have
 * to care about Synchronization.
 */
public class InstanceBean implements SiriusPCS {
    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);
    private final String featureId;
    private AlignedFeature sourceFeature;
    private MsData msData;
    private SpectralMatchingResult spectralMatchingResult;

    @NotNull
    private final GuiProjectManager projectManager;

    @Override
    public HiddenChangeSupport pcs() {
        return pcs;
    }

    //Project-space listener
    private final PropertyChangeListener listener;
    private final AtomicBoolean pcsEnabled = new AtomicBoolean(false);


    //todo best hit property change is needed.
    // e.g. if the scoring changes from sirius to zodiac

    //todo make compute state nice
    //todo we may nee background loading tasks for retriving informaion from project space

    //todo som unregister listener stategy

    public InstanceBean(@NotNull AlignedFeature sourceFeature, @NotNull GuiProjectManager projectManager) {
        this(sourceFeature.getAlignedFeatureId(), sourceFeature, projectManager);
    }

    public InstanceBean(@NotNull String featureId, @NotNull GuiProjectManager projectManager) {
        this(featureId, null, projectManager);
    }

    public InstanceBean(@NotNull String featureId, @Nullable AlignedFeature sourceFeature, @NotNull GuiProjectManager projectManager) {
        this.featureId = featureId;
        this.sourceFeature = sourceFeature;
        this.projectManager = projectManager;

        if (sourceFeature != null)
            assert sourceFeature.getAlignedFeatureId().equals(featureId);
        this.listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() != null && evt.getNewValue() instanceof ProjectChangeEvent pce) {
                    if (getFeatureId().equals(pce.getFeaturedId())) {
                        synchronized (InstanceBean.this) { //todo nighsky: check if this makes sense or if this needs to change on selection only
                            InstanceBean.this.spectralMatchingResult = null;
                            InstanceBean.this.sourceFeature = null;
                        }
                        switch (pce.getEventType()) {
                            case FEATURE_UPDATED -> {
                                synchronized (InstanceBean.this) {
                                    InstanceBean.this.msData = null;
                                }
                                if (pcsEnabled.get())
                                    pcs.firePropertyChange("instance.updated", null, pce);
                            }
                            case RESULT_CREATED -> {
                                if (pcsEnabled.get())
                                    pcs.firePropertyChange("instance.createFormulaResult", null, pce);
                            }
                            case RESULT_DELETED -> {
                                if (pcsEnabled.get())
                                    pcs.firePropertyChange("instance.deleteFormulaResult", null, pce);
                            }
                        }
                    } else {
                        LoggerFactory.getLogger(getClass()).warn("Event delegated with wrong feature id! Id is {} instead of {}!", pce.getFeaturedId(), getFeatureId());
                    }
                }
            }
        };
        registerProjectSpaceListener();
    }

    public void registerProjectSpaceListener() {
        projectManager.pcs.addPropertyChangeListener("project.updateInstance." + getFeatureId(), listener);
    }

    public void unregisterProjectSpaceListener() {
        projectManager.pcs.removePropertyChangeListener("project.updateInstance." + getFeatureId(), listener);
    }

    public void enableProjectSpaceListener() {
        pcsEnabled.set(true);
    }

    public void disableProjectSpaceListener() {
        pcsEnabled.set(false);
    }

    public NightSkyClient getClient() {
        return getProjectManager().getClient();
    }

    public GuiProjectManager getProjectManager() {
        return projectManager;
    }

    @NotNull
    private AlignedFeature getSourceFeature(AlignedFeatureOptField... optFields) {
        return getSourceFeature(List.of(optFields));
    }

    @NotNull
    private Optional<AlignedFeature> sourceFeature() {
        return Optional.ofNullable(sourceFeature);
    }

    @NotNull
    public AlignedFeature getSourceFeature(@Nullable List<AlignedFeatureOptField> optFields) {
        //we always load top annotations because it contains mandatory information for the SIRIUS GUI
        List<AlignedFeatureOptField> of = (optFields != null && !optFields.isEmpty() && !optFields.equals(List.of(AlignedFeatureOptField.NONE))
                ? Stream.concat(optFields.stream(), Stream.of(AlignedFeatureOptField.TOPANNOTATIONS)).distinct().toList()
                : List.of(AlignedFeatureOptField.TOPANNOTATIONS));

        // we update every time here since we do not know which optional fields are already loaded.
        if (sourceFeature == null || !of.equals(List.of(AlignedFeatureOptField.TOPANNOTATIONS)))
            sourceFeature = withIds((pid, fid) ->
                    getClient().features().getAlignedFeature(pid, fid, of));

        return sourceFeature;
    }

    public String getFeatureId() {
        return featureId;
    }

    public long getIndex() {
        return getSourceFeature().getIndex();
    }

    public String getName() {
        return getSourceFeature().getName(); //todo nightsky: check if this is the correct name
    }

    public String getGUIName() {
        return getName() + " (" + getFeatureId() + ")";
    }

    public PrecursorIonType getIonType() {
        if (getSourceFeature().getAdduct() == null)
            return null;
        return PrecursorIonType.fromString(getSourceFeature().getAdduct());
    }

    public double getIonMass() {
        return getSourceFeature().getIonMass();
    }


    public boolean isComputing() {
        return getSourceFeature().isComputing();
    }

    public Optional<RetentionTime> getRT() {
        @NotNull AlignedFeature f = getSourceFeature();
        if (f.getRtStartSeconds() != null && f.getRtEndSeconds() != null) {
            if (Double.compare(f.getRtStartSeconds(), f.getRtEndSeconds()) == 0)
                return Optional.of(new RetentionTime(f.getRtStartSeconds()));
            return Optional.of(new RetentionTime(f.getRtStartSeconds(), f.getRtEndSeconds()));
        } else if (f.getRtStartSeconds() != null) {
            return Optional.of(new RetentionTime(f.getRtStartSeconds()));
        } else if (f.getRtEndSeconds() != null) {
            return Optional.of(new RetentionTime(f.getRtEndSeconds()));
        }

        return Optional.empty();
    }

    public Optional<FormulaResultBean> getFormulaAnnotationAsBean() {
        return getFormulaAnnotation().map(fc -> new FormulaResultBean(fc, this));
    }

    public Optional<FormulaCandidate> getFormulaAnnotation() {
        return Optional.ofNullable(getSourceFeature().getTopAnnotations().getFormulaAnnotation());
    }

    public Optional<StructureCandidateScored> getStructureAnnotation() {
        return Optional.ofNullable(getSourceFeature().getTopAnnotations().getStructureAnnotation());
    }

    public Optional<Double> getConfidenceScoreDefault() {
        return getStructureAnnotation().map(StructureCandidateScored::getConfidenceExactMatch);
    }

    public List<FormulaResultBean> getFormulaCandidates() {
        return withIds((pid, fid) -> getClient().features()
                .getFormulaCandidates(pid, fid, 0, Integer.MAX_VALUE, null, null, SearchQueryType.LUCENE, ensureDefaultOptFields(null)))
                .getContent().stream()
                .map(formulaCandidate -> new FormulaResultBean(formulaCandidate, this))
                .toList();

    }

    public List<FingerprintCandidateBean> getStructureCandidates(int topK) {
        PageStructureCandidateFormula page = getStructureCandidatesPage(topK);
        if (page.getContent() == null)
            return null; //this does usually not happen?!
        if (page.getContent().isEmpty())
            return List.of();


        MaskedFingerprintVersion fpVersion = getProjectManager().getFingerIdData(getIonType().getCharge())
                .getFingerprintVersion();

        Map<String, ProbabilityFingerprint> fps = page.getContent().stream()
                .map(StructureCandidateFormula::getFormulaId).distinct()
                .collect(Collectors.toMap(fcid -> fcid, fcid -> new ProbabilityFingerprint(
                        fpVersion,
                        (List<Double>) withIds((pid, fid) -> getClient().features().getFingerprintPrediction(pid, fid, fcid))
                )));

        return page.getContent().stream().map(c -> new FingerprintCandidateBean(c, fps.get(c.getFormulaId()), getSpectralSearchResults())).toList();
    }

    public PageStructureCandidateFormula getStructureCandidatesPage(int topK) {
        return getStructureCandidatesPage(0, topK);
    }

    public PageStructureCandidateFormula getStructureCandidatesPage(int pageNum, int pageSize) {
        return withIds((pid, fid) -> getClient().features()
                .getStructureCandidates(pid, fid, pageNum, pageSize, null, null, SearchQueryType.LUCENE,
                        List.of(StructureCandidateOptField.DBLINKS, StructureCandidateOptField.FINGERPRINT)));
    }


    public synchronized MsData getMsData() {
        if (msData == null) {
            msData = sourceFeature().map(AlignedFeature::getMsData)
                    .orElse(withIds((pid, fid) -> getClient().features().getMsData(pid, getFeatureId())));
        }

        return msData;
    }

    public SpectralMatchingResult getSpectralSearchResults() {
        if (spectralMatchingResult == null) {
            spectralMatchingResult = getSpectralSearchResults(100);
        }
        return spectralMatchingResult;
    }
    public SpectralMatchingResult getSpectralSearchResults(int topK) {
        return Optional.ofNullable(getSpectralSearchResultsPage(topK)).map(PageSpectralLibraryMatch::getContent)
                .map(SpectralMatchingResult::new).orElse(new SpectralMatchingResult(List.of()));
    }

    protected PageSpectralLibraryMatch getSpectralSearchResultsPage(int topK) {
        return getSpectralSearchResultsPage(0, topK);

    }

    protected PageSpectralLibraryMatch getSpectralSearchResultsPage(int pageNum, int pageSize) {
        return withIds((pid, fid) -> getClient().features()
                .getSpectralLibraryMatchesWithResponseSpec(pid, fid, pageNum, pageSize, null, null, SearchQueryType.LUCENE,
                        List.of(SpectralLibraryMatchOptField.REFERENCESPECTRUM)).bodyToMono(PageSpectralLibraryMatch.class).onErrorComplete().block());
    }

    public MutableMs2Experiment asMs2Experiment() {
        MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setIonMass(getIonMass());
        exp.setFeatureId(getFeatureId());
        exp.setPrecursorIonType(getIonType());
        exp.setMs1Spectra(getMsData().getMs1Spectra().stream()
                .map(s -> new SimpleSpectrum(WrapperSpectrum.of(s.getPeaks(), SimplePeak::getMz, SimplePeak::getIntensity)))
                .toList());
        exp.setMs2Spectra(getMsData().getMs2Spectra().stream()
                .map(s -> new MutableMs2Spectrum(WrapperSpectrum.of(s.getPeaks(), SimplePeak::getMz, SimplePeak::getIntensity), s.getPrecursorMz(), CollisionEnergy.fromStringOrNull(s.getCollisionEnergy()), 2))
                .toList());
        Optional.ofNullable(getMsData().getMergedMs1())
                .map(s -> new SimpleSpectrum(WrapperSpectrum.of(s.getPeaks(), SimplePeak::getMz, SimplePeak::getIntensity)))
                .ifPresent(exp::setMergedMs1Spectrum);
        return exp;
    }


    //todo nightsky reenable setter

    public synchronized <R> R withIds(BiFunction<String, String, R> doWithClient) {
        return doWithClient.apply(projectManager.getProjectId(), getFeatureId());
    }

    public Setter set() {
        throw new UnsupportedOperationException("Implement modification features in nightsky api");
    }

    public class Setter {
        private List<Consumer<MutableMs2Experiment>> mods = new ArrayList<>();

        private Setter() {
        }

        // this is all MSExperiment update stuff. We listen to experiment changes on the project-space.
        // so calling updateExperiemnt will result in a EDT property change event if it was successful
        public Setter setName(final String name) {
            //todo nightsky
            throw new UnsupportedOperationException("Implement modification features in nightsky api");
            /*
            mods.add((exp) -> {
                if (projectSpace().renameCompound(getID(), name, (idx) -> spaceManager.namingScheme.apply(idx, name)))
                    exp.setName(name);
            });
            return this;*/
        }

        public Setter setIonType(final PrecursorIonType ionType) {

            mods.add((exp) -> exp.setPrecursorIonType(ionType));
            return this;
        }

        public Setter setIonMass(final double ionMass) {
            mods.add((exp) -> exp.setIonMass(ionMass));
            return this;
        }

        public Setter setMolecularFormula(final MolecularFormula formula) {
            mods.add((exp) -> exp.setMolecularFormula(formula));
            return this;
        }

        public void apply() {
            throw new UnsupportedOperationException("Implement modification features in nightsky api");
            //todo nightsky
            /*final MutableMs2Experiment exp = getMutableExperiment();
            for (Consumer<MutableMs2Experiment> mod : mods)
                mod.accept(exp);
            updateExperiment();*/
        }
    }
}