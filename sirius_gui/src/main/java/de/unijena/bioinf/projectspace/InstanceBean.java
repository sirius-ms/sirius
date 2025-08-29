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
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.ms.gui.fingerid.DatabaseLabel;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchBean;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchingCache;
import io.sirius.ms.sdk.SiriusClient;
import io.sirius.ms.sdk.model.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
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
    private static final Logger log = LoggerFactory.getLogger(InstanceBean.class);
    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);
    @Getter
    private final String featureId;
    private volatile AlignedFeature sourceFeature;
    private final Set<AlignedFeatureOptField> optFields;

    private volatile MsData msData;
    private volatile SpectralMatchingCache spectralMatchingCache;
    private volatile List<FormulaResultBean> formulaAnnotationCache;
    private volatile Optional<FormulaResultBean> topFormulaCache;

    @Getter
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

    public InstanceBean(@NotNull AlignedFeature sourceFeature, @NotNull Collection<AlignedFeatureOptField> sourceOptFields, @NotNull GuiProjectManager projectManager) {
        this(sourceFeature.getAlignedFeatureId(), sourceFeature, sourceOptFields, projectManager);
    }

    public InstanceBean(@NotNull String featureId, @NotNull GuiProjectManager projectManager) {
        this(featureId, null, null, projectManager);
    }

    private InstanceBean(@NotNull String featureId, @Nullable AlignedFeature sourceFeature, @Nullable Collection<AlignedFeatureOptField> sourceOptFields, @NotNull GuiProjectManager projectManager) {
        this.featureId = featureId;
        this.projectManager = projectManager;
        this.sourceFeature = sourceFeature;
        this.optFields = new HashSet<>();
        if (sourceOptFields != null)
            this.optFields.addAll(sourceOptFields);

        if (this.sourceFeature == null) {
            //preload minimal information for the compound list to prevent them from being loaded in EDT.
            this.sourceFeature = projectManager.getFeature(getFeatureId(), DEFAULT_OPT_FEATURE_FIELDS);
            this.optFields.addAll(DEFAULT_OPT_FEATURE_FIELDS);
        }

        this.listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (("project.updateInstance" + getFeatureId()).equals(evt.getPropertyName())) {
                    if (evt.getNewValue() != null) {
                        clearCache(evt.getNewValue());
                    } else {
                        if (pcsEnabled.get()) //fire without invalidation, e.g. if some in memory updates happened.
                            pcs.firePropertyChange("instance.updated", null, null);
                    }
                } else {
                    LoggerFactory.getLogger(getClass()).warn("Event delegated with wrong feature id! Id is {} instead of {}!", evt.getPropertyName(), getFeatureId());
                }
            }
        };
        registerProjectSpaceListener();
    }

    private void clearCache(@NotNull Object evt) {
        if (SwingUtilities.isEventDispatchThread())
            log.warn("Cache update happened in GUI thread. Might cause GUI stutters!");
        synchronized (this) {
            this.optFields.clear();
            try {
                this.sourceFeature = projectManager.getFeature(getFeatureId(), DEFAULT_OPT_FEATURE_FIELDS);
                this.optFields.addAll(DEFAULT_OPT_FEATURE_FIELDS);
            } catch (WebClientResponseException.NotFound e) {  // feature has been deleted on server, will be deleted on client with a following event
                this.sourceFeature = null;
            }

            //todo this should go into  clearResultCache but this seems to have side effects, due to suboptimal caching architecture
            this.spectralMatchingCache = null;
            clearResultCache();
        }
        if (evt instanceof BackgroundComputationsStateEvent pce) {
            if (pcsEnabled.get())
                pcs.firePropertyChange("instance.updated", null, pce);
        } else if (evt instanceof ProjectChangeEvent pce && pce.getEventType() != null) {
            switch (pce.getEventType()) {
                case RESULT_CREATED -> {
                    if (pcsEnabled.get())
                        pcs.firePropertyChange("instance.resultCreated", null, pce);
                }
                case RESULT_UPDATED -> {
                    if (pcsEnabled.get())
                        pcs.firePropertyChange("instance.resultUpdated", null, pce);
                }
                case RESULT_DELETED -> {
                    if (pcsEnabled.get())
                        pcs.firePropertyChange("instance.resultDeleted", null, pce);
                }
            }
        }
    }

    public void registerProjectSpaceListener() {
        projectManager.pcs.addPropertyChangeListener("project.updateInstance" + getFeatureId(), listener);
    }

    public void unregisterProjectSpaceListener() {
        projectManager.pcs.removePropertyChangeListener("project.updateInstance" + getFeatureId(), listener);
    }

    public void enableProjectSpaceListener() {
        pcsEnabled.set(true);
    }

    public void disableProjectSpaceListener() {
        pcsEnabled.set(false);
        clearResultCache();
    }

    private void clearResultCache() {
        synchronized (this) {
            //todo find the side effects happending with removing on deselsection and enable it again.
//            this.spectralMatchingCache = null;
            this.formulaAnnotationCache = null;
            this.topFormulaCache = null;
        }
    }

    public SiriusClient getClient() {
        return getProjectManager().getClient();
    }


    @NotNull
    public AlignedFeature getSourceFeature() {
        return getSourceFeature(List.of());
    }

    @NotNull
    private Optional<AlignedFeature> sourceFeature() {
        return Optional.ofNullable(sourceFeature);
    }

    public static final List<AlignedFeatureOptField> DEFAULT_OPT_FEATURE_FIELDS = List.of(AlignedFeatureOptField.COMPUTED_TOOLS, AlignedFeatureOptField.TOP_ANNOTATIONS_SUMMARY);

    @NotNull
    public AlignedFeature getSourceFeature(@Nullable List<AlignedFeatureOptField> optFields) {
        //we always load top annotations because it contains mandatory information for the SIRIUS GUI
        List<AlignedFeatureOptField> of = (optFields != null && !optFields.isEmpty() && !optFields.equals(List.of(AlignedFeatureOptField.NONE))
                ? Stream.concat(optFields.stream(), DEFAULT_OPT_FEATURE_FIELDS.stream()).distinct().toList()
                : DEFAULT_OPT_FEATURE_FIELDS);

        //double-checked locking source feature must be volatile
        if (sourceFeature == null || !this.optFields.containsAll(of)) {
            synchronized (this) {
                // we update every time here since we do not know which optional fields are already loaded.
                if (sourceFeature == null || !this.optFields.containsAll(of)) {
                    if (SwingUtilities.isEventDispatchThread())
                        log.warn("Reload Featured '{}' with nu [{}] vs current [{}] in Event Thread. Might cause GUI stutters!", sourceFeature.getAlignedFeatureId(), of.stream().sorted().map(AlignedFeatureOptField::toString).collect(Collectors.joining(", ")), this.optFields.stream().sorted().map(AlignedFeatureOptField::toString).collect(Collectors.joining(", ")));

                    sourceFeature = withIds((pid, fid) ->
                            getClient().features().getAlignedFeature(pid, fid, false, of));
                    this.optFields.clear();
                    this.optFields.addAll(of);
                }
                return sourceFeature;
            }
        }
        return sourceFeature;
    }

    public String getName() {
        return getSourceFeature().getName(); //todo nightsky: check if this is the correct name
    }

    public String getGUIName() {
        if (getName() != null && !getFeatureId().equalsIgnoreCase(getName()) && !getFeatureId().toLowerCase().endsWith(getName().toLowerCase()))
            return getName() + " (" + getFeatureId() + ")";
        return getFeatureId();
    }

    public @Nullable DataQuality getQuality() {
        return getSourceFeature().getQuality();
    }

    @Nullable
    public AlignedFeatureQualityExperimental getQualityReport() {
        return withIdsCatched((pid, fid) -> getClient().features().getAlignedFeatureQualityExperimentalWithResponseSpec(pid, fid)
                .bodyToMono(AlignedFeatureQualityExperimental.class).onErrorComplete().block());
    }

    public PrecursorIonType getIonType() {
        Set<PrecursorIonType> adducts = getDetectedAdducts();
        if (adducts.size() == 1)
            return adducts.iterator().next();
        return PrecursorIonType.unknown(getSourceFeature().getCharge());
    }

    public Set<PrecursorIonType> getDetectedAdducts() {
        return getSourceFeature().getDetectedAdducts().stream()
                .map(PrecursorIonType::parsePrecursorIonType)
                .flatMap(Optional::stream)
                .filter(it -> !it.isIonizationUnknown()) //Detected adducts may contain unknown adduct by convention to indicate that they are not very confident
                .collect(Collectors.toSet());
    }

    public Set<PrecursorIonType> getDetectedAdductsOrUnknown() {
        @NotNull AlignedFeature f = getSourceFeature();
        Set<PrecursorIonType> add = f.getDetectedAdducts().stream()
                .map(PrecursorIonType::parsePrecursorIonType)
                .flatMap(Optional::stream)
                .filter(it -> !it.isIonizationUnknown()) //Detected adducts may contain unknown adduct by convention to indicate that they are not very confident
                .collect(Collectors.toSet());
        if (add.isEmpty())
            return Set.of(PrecursorIonType.unknown(f.getCharge()));
        return add;
    }

    public Set<PrecursorIonType> getDetectedAdductsIncludingUnknown() {
        @NotNull AlignedFeature f = getSourceFeature();
        if (f.getDetectedAdducts().isEmpty())
            return Set.of(PrecursorIonType.unknown(f.getCharge()));

        return f.getDetectedAdducts().stream()
                .map(PrecursorIonType::parsePrecursorIonType)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }

    public Set<PrecursorIonType> getDetectedAdductsOrCharge() {
        Set<PrecursorIonType> detected = getDetectedAdducts();
        if (detected.isEmpty()) return Collections.singleton(PrecursorIonType.unknown(getSourceFeature().getCharge()));
        else return detected;
    }

    public double getIonMass() {
        return getSourceFeature().getIonMass();
    }


    public boolean isComputing() {
        return getSourceFeature().isComputing();
    }

    public Optional<RetentionTime> getRT() {
        @NotNull AlignedFeature f = getSourceFeature();
        if (f.getRtStartSeconds() != null && f.getRtEndSeconds() != null && f.getRtApexSeconds() != null) {
            if (Double.compare(f.getRtStartSeconds(), f.getRtEndSeconds()) == 0)
                return Optional.of(new RetentionTime(f.getRtStartSeconds()));
            return Optional.of(new RetentionTime(f.getRtStartSeconds(), f.getRtEndSeconds(), f.getRtApexSeconds()));
        } else if (f.getRtApexSeconds() != null) {
            return Optional.of(new RetentionTime(f.getRtApexSeconds()));
        } else if (f.getRtStartSeconds() != null) {
            return Optional.of(new RetentionTime(f.getRtStartSeconds()));
        } else if (f.getRtEndSeconds() != null) {
            return Optional.of(new RetentionTime(f.getRtEndSeconds()));
        }

        return Optional.empty();
    }

    public RetentionTime getRTOrMissing() {
        return getRT().orElseGet(RetentionTime::NA);
    }

    @NotNull
    public Optional<FormulaResultBean> getFormulaAnnotation() {
        if (topFormulaCache == null) {
            synchronized (this) {
                if (topFormulaCache == null) {
                    List<FormulaResultBean> candidates = getFormulaCandidates();
                    if (Utils.isNullOrEmpty(candidates)) {
                        topFormulaCache = Optional.empty();
                    } else {
                        topFormulaCache = candidates.stream().filter(FormulaResultBean::isTopStructureFormula).findFirst();
                        if (topFormulaCache.isEmpty())
                            topFormulaCache = Optional.of(candidates.getFirst());
                    }
                }
            }
        }
        return topFormulaCache;
    }

    public Optional<Double> getConfidenceScore(ConfidenceDisplayMode viewMode) {
        return viewMode == ConfidenceDisplayMode.APPROXIMATE ?
                Optional.ofNullable(getSourceFeature().getTopAnnotations()).map(FeatureAnnotations::getConfidenceApproxMatch) :
                Optional.ofNullable(getSourceFeature().getTopAnnotations()).map(FeatureAnnotations::getConfidenceExactMatch);
    }

    @NotNull
    public List<FormulaResultBean> getFormulaCandidates() {
        if (!getSourceFeature().getComputedTools().isFormulaSearch())
            return List.of();
        //double-checked locking, msData must be volatile
        if (formulaAnnotationCache == null) {
            synchronized (this) {
                if (formulaAnnotationCache == null) {
                    formulaAnnotationCache = withIdsCatched((pid, fid) -> getClient().features()
                            .getFormulaCandidates(pid, fid, false, ensureDefaultOptFields(null)))
                            .stream()
                            .map(formulaCandidate -> new FormulaResultBean(formulaCandidate, this))
                            .toList();

                    if (Boolean.TRUE.equals(getComputedTools().isStructureSearch())) {
                        List<FingerprintCandidateBean> top = getStructureCandidates(1, false);
                        if (Utils.notNullOrEmpty(top)) {
                            String formulaId = top.getFirst().getCandidate().getFormulaId();
                            formulaAnnotationCache.stream().filter(fa -> fa.getFormulaId().equals(formulaId))
                                    .forEach(fa -> fa.setTopStructureFormula(true));
                        }
                    }


                }
            }
        }
        return formulaAnnotationCache;
    }

    /**
     * retrieves database and de novo structure candidates and merges identical structures
     */
    @NotNull
    public List<FingerprintCandidateBean> getBothStructureCandidates(int topK, boolean fp, boolean loadDatabaseHits, boolean loadDenovo) {
        final List<FingerprintCandidateBean> database = !loadDatabaseHits ? Collections.emptyList() : toFingerprintCandidateBeans(getStructureCandidatesPage(topK, fp), true, false);
        final List<FingerprintCandidateBean> deNovo = !loadDenovo ? Collections.emptyList() : toFingerprintCandidateBeans(getDeNovoStructureCandidatesPage(topK, fp), false, true);
        final List<FingerprintCandidateBean> merged = mergeIdenticalStructures(database, deNovo);
        return addDeNovoDatabaseLabels(merged);
    }

    @NotNull
    private List<FingerprintCandidateBean> mergeIdenticalStructures(@NotNull List<FingerprintCandidateBean> database, @NotNull List<FingerprintCandidateBean> deNovo) {
        if (database.isEmpty()) return deNovo;
        if (deNovo.isEmpty()) return database;

        database = database.stream().sorted(Comparator.comparing(a -> a.getCandidate().getInchiKey())).toList();
        deNovo = deNovo.stream().sorted(Comparator.comparing(a -> a.getCandidate().getInchiKey())).toList();

        List<FingerprintCandidateBean> merged = new ArrayList<>();
        int i = 0, j = 0;
        while ((i < database.size()) && (j < deNovo.size())) {
            FingerprintCandidateBean db = database.get(i);
            FingerprintCandidateBean novo = deNovo.get(j);
            if (db.getInChiKey().equals(novo.getInChiKey())) {
                FingerprintCandidateBean mergedFC = db.withNewDatabaseAndDeNovoFlag(true, true);
                merged.add(mergedFC);
                ++i;
                ++j;
            } else if (db.getInChiKey().compareTo(novo.getInChiKey()) < 0) {
                merged.add(db);
                ++i;
            } else {
                merged.add(novo);
                ++j;
            }
        }
        while (i < database.size()) {
            merged.add(database.get(i++));
        }
        while (j < deNovo.size()) {
            merged.add(deNovo.get(j++));
        }
        //recalculate ranks
        {
            merged.sort(Comparator.comparing(a -> ((FingerprintCandidateBean) a).getCandidate().getCsiScore()).reversed());
            int rank = 1;
            for (FingerprintCandidateBean fc : merged)
                fc.getCandidate().setRank(rank++);
        }
        return merged;
    }

    @NotNull
    private List<FingerprintCandidateBean> addDeNovoDatabaseLabels(@NotNull List<FingerprintCandidateBean> merged) {
        return merged.stream().map(fpc -> fpc.isDeNovo() ? fpc.withAdditionalLabelAtBeginning(new DatabaseLabel[]{new DatabaseLabel("De Novo", null)}) : fpc).toList();
    }

    @NotNull
    public List<FingerprintCandidateBean> getStructureCandidates(int topK, boolean fp) {
        return toFingerprintCandidateBeans(getStructureCandidatesPage(topK, fp), true, false);
    }

    @Nullable
    public PagedModelStructureCandidateFormula getStructureCandidatesPage(int topK, boolean fp) {
        return getStructureCandidatesPage(0, topK, fp);
    }

    @Nullable
    public PagedModelStructureCandidateFormula getStructureCandidatesPage(int pageNum, int pageSize, boolean fp) {
        if (!getSourceFeature().getComputedTools().isStructureSearch())
            return null;
        return withIdsCatched((pid, fid) -> getClient().features()
                .getStructureCandidatesPaged(pid, fid, pageNum, pageSize, null,
                        fp ? List.of(StructureCandidateOptField.DB_LINKS, StructureCandidateOptField.FINGERPRINT) : List.of(StructureCandidateOptField.DB_LINKS)));
    }


    @NotNull
    public List<FingerprintCandidateBean> getDeNovoStructureCandidates(int topK, boolean fp) {
        return toFingerprintCandidateBeans(getDeNovoStructureCandidatesPage(topK, fp), false, true);
    }


    @Nullable
    public PagedModelStructureCandidateFormula getDeNovoStructureCandidatesPage(int topK, boolean fp) {
        return getDeNovoStructureCandidatesPage(0, topK, fp);
    }

    @Nullable
    public PagedModelStructureCandidateFormula getDeNovoStructureCandidatesPage(int pageNum, int pageSize, boolean fp) {
        if (!getSourceFeature().getComputedTools().isDeNovoSearch())
            return null;
        return withIdsCatched((pid, fid) -> getClient().features()
                .getDeNovoStructureCandidatesPaged(pid, fid, pageNum, pageSize, null,
                        fp ? List.of(StructureCandidateOptField.DB_LINKS, StructureCandidateOptField.FINGERPRINT) : List.of(StructureCandidateOptField.DB_LINKS)));
    }

    @NotNull
    private List<FingerprintCandidateBean> toFingerprintCandidateBeans(PagedModelStructureCandidateFormula page, boolean isDatabase, boolean isDeNovo) {
        if (page == null || page.getContent() == null)
            return List.of();
        if (page.getContent().isEmpty())
            return List.of();


        try {
            MaskedFingerprintVersion fpVersion = getProjectManager().getFingerIdData(getIonType().getCharge())
                    .getFingerprintVersion();

            Map<String, ProbabilityFingerprint> fps = page.getContent().stream()
                    .map(StructureCandidateFormula::getFormulaId).distinct()
                    .collect(Collectors.toMap(fcid -> fcid, fcid -> new ProbabilityFingerprint(
                            fpVersion,
                            (List<Double>) withIds((pid, fid) -> getClient().features().getFingerprintPrediction(pid, fid, fcid))
                    )));
            return page.getContent().stream().map(c -> new FingerprintCandidateBean(c, isDatabase, isDeNovo, fps.get(c.getFormulaId()), this)).toList();
        } catch (WebClientResponseException e) {
            return List.of();
        }
    }

    public List<SpectralMatchBean> getTopSpectralMatches() {
        return withSpectralMatchingCache(cache -> cache.getPageFiltered(0));
    }

    public MsData getMsData() {
        return getMsData(false);
    }

    public MsData getMsData(boolean asSearchPreparedMsData) {
        //double-checked locking, msData must be volatile
        if (msData == null) {
            synchronized (this) {
                if (msData == null) {
                    msData = sourceFeature().map(AlignedFeature::getMsData)
                            .orElse(withIdsCatched((pid, fid) -> getClient().features().getMsData(pid, getFeatureId(), asSearchPreparedMsData)));
                }
                return msData;
            }
        }
        return msData;
    }


    public Boolean hasMs1() {
        return getSourceFeature().isHasMs1();
    }

    public Boolean hasMsMs() {
        return getSourceFeature().isHasMsMs();
    }

    private <R> R withSpectralMatchingCache(Function<SpectralMatchingCache, R> doWithCache) {
        //double-checked locking, msData must be volatile
        if (spectralMatchingCache == null) {
            synchronized (this) {
                if (spectralMatchingCache == null) {
                    spectralMatchingCache = new SpectralMatchingCache(this);
                }
            }
        }
        return doWithCache.apply(spectralMatchingCache);
    }

    public int getNumberOfSpectralMatches() {
        return withSpectralMatchingCache(cache -> cache.getSummary().getReferenceSpectraCount());
    }

    public List<SpectralMatchBean> getAllSpectralMatches() {
        return withSpectralMatchingCache(SpectralMatchingCache::getAllFiltered);
    }

    public List<SpectralMatchBean> getSpectralMatchGroupFromTop(long refSpecUUID) {
        return withSpectralMatchingCache(cache -> cache.getGroupOnPage(0, refSpecUUID));
    }

    public List<SpectralMatchBean> getSpectralMatchGroup(long refSpecUUID) {
        return withSpectralMatchingCache(cache -> cache.getGroup(refSpecUUID));
    }

    @NotNull
    public ComputedSubtools getComputedTools() {
        ComputedSubtools tools = getSourceFeature().getComputedTools();
        if (tools == null) { //should not happen
            log.warn("Computed subtools information is null for feature {}.", getFeatureId());
            return new ComputedSubtools();
        }
        return tools;
    }

    public MutableMs2Experiment asMs2Experiment() {
        MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setIonMass(getIonMass());
        exp.setFeatureId(getFeatureId());
        exp.setPrecursorIonType(getIonType());
        MsData msdata = getMsData();
        exp.setMs1Spectra(msdata.getMs1Spectra().stream()
                .map(s -> new SimpleSpectrum(WrapperSpectrum.of(s.getPeaks(), SimplePeak::getMz, SimplePeak::getIntensity)))
                .toList());
        exp.setMs2Spectra(msdata.getMs2Spectra().stream()
                .map(s -> new MutableMs2Spectrum(WrapperSpectrum.of(s.getPeaks(), SimplePeak::getMz, SimplePeak::getIntensity), s.getPrecursorMz(), CollisionEnergy.fromStringOrNull(s.getCollisionEnergy()), 2))
                .toList());
        Optional.ofNullable(msdata.getMergedMs1())
                .map(s -> new SimpleSpectrum(WrapperSpectrum.of(s.getPeaks(), SimplePeak::getMz, SimplePeak::getIntensity)))
                .ifPresent(exp::setMergedMs1Spectrum);
        return exp;
    }


//todo nightsky reenable setter

    public synchronized <R> R withIds(BiFunction<String, String, R> doWithClient) {
        return doWithClient.apply(projectManager.getProjectId(), getFeatureId());
    }


    public synchronized <R> R withIdsCatched(BiFunction<String, String, R> doWithClient) {
        try {
            return withIds(doWithClient);
        } catch (WebClientResponseException e) {
            log.warn("Error during request | {}", e.getMessage());
            log.debug("Error during request!", e);
            return null;
        }
    }

    public Setter set() {
        throw new UnsupportedOperationException("Implement modification features in nightsky api");
    }

    synchronized boolean changeComputeStateOfCache(boolean computeState) {
        if (sourceFeature != null) {
            sourceFeature.setComputing(computeState);
            return true;
        }
        return false;
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