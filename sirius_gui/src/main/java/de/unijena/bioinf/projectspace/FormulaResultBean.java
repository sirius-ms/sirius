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
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import io.sirius.ms.sdk.SiriusClient;
import io.sirius.ms.sdk.model.*;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * this is the view for SiriusResultElement.class
 */
public class FormulaResultBean implements SiriusPCS, Comparable<FormulaResultBean>, DataAnnotation {
    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);
    private final PropertyChangeListener listener;

    private final InstanceBean parentInstance;

    private final String formulaId;

    private FormulaCandidate sourceCandidate;

    private Optional<ProbabilityFingerprint> fp;
    private Optional<LipidAnnotation> lipidAnnotation;
    private Optional<IsotopePatternAnnotation> isotopePatterAnnotation;
    private Optional<FragmentationTree> fragmentationTree;

    private Pair<CompoundClasses, CanopusPrediction> canopusResults;
    private Optional<String> ftreeJson;


    @Override
    public HiddenChangeSupport pcs() {
        return pcs;
    }

    protected FormulaResultBean() {
        this.formulaId = null;
        this.sourceCandidate = null;
        this.parentInstance = null;
        listener = null;
        //dummy constructor
    }

    public FormulaResultBean(@NotNull FormulaCandidate sourceCandidate, @NotNull InstanceBean parentInstance) {
        this(sourceCandidate.getFormulaId(), sourceCandidate, parentInstance);
    }

    public FormulaResultBean(@NotNull String formulaId, @NotNull InstanceBean parentInstance) {
        this(formulaId, null, parentInstance);
    }

    public FormulaResultBean(@NotNull String formulaId, @Nullable FormulaCandidate sourceCandidate, @NotNull InstanceBean parentInstance) {
        this.formulaId = formulaId;
        this.sourceCandidate = sourceCandidate;
        this.parentInstance = parentInstance;

        if (sourceCandidate != null)
            assert sourceCandidate.getFormulaId().equals(formulaId);
        this.listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() != null && evt.getNewValue() instanceof ProjectChangeEvent pce) {
                    if (sourceCandidate.getFormulaId().equals(pce.getFormulaId())) {
                        if (pce.getEventType() == ProjectChangeEvent.EventTypeEnum.RESULT_UPDATED) {
                            synchronized (FormulaResultBean.this) {
                                FormulaResultBean.this.sourceCandidate = null;
                                FormulaResultBean.this.canopusResults = null;
                                FormulaResultBean.this.fp = null;
                                FormulaResultBean.this.lipidAnnotation = null;
                                FormulaResultBean.this.isotopePatterAnnotation = null;
                                FormulaResultBean.this.fragmentationTree = null;
                                FormulaResultBean.this.ftreeJson = null;
                            }
                            pcs.firePropertyChange("formulaResult.update", null, pce);
                        }
                    } else {
                        LoggerFactory.getLogger(getClass()).warn("Event delegated with wrong formula id! Id is {} instead of {}!", pce.getFormulaId(), getFormulaId());
                    }
                }
            }
        };
        registerProjectSpaceListeners();
    }

    public SiriusClient getClient() {
        return getParentInstance().getClient();
    }

    public InstanceBean getParentInstance() {
        return parentInstance;
    }

    @NotNull
    private FormulaCandidate getFormulaCandidate(FormulaCandidateOptField... optFields) {
        return getFormulaCandidate(List.of(optFields));
    }

    public static List<FormulaCandidateOptField> ensureDefaultOptFields(@Nullable List<FormulaCandidateOptField> optFields) {
        //we always load top annotations because it contains mandatory information for the SIRIUS GUI
        return (optFields != null && !optFields.isEmpty() && !optFields.equals(List.of(FormulaCandidateOptField.NONE))
                ? Stream.concat(optFields.stream(), Stream.of(FormulaCandidateOptField.STATISTICS)).distinct().toList()
                : List.of(FormulaCandidateOptField.STATISTICS));
    }

    @NotNull
    private synchronized FormulaCandidate getFormulaCandidate(@Nullable List<FormulaCandidateOptField> optFields) {
        final List<FormulaCandidateOptField> of = ensureDefaultOptFields(optFields);

        // we update every time here since we do not know which optional fields are already loaded.
        if (sourceCandidate == null || !of.equals(List.of(FormulaCandidateOptField.STATISTICS)))
            sourceCandidate = withIds((pid, featId, formId) ->
                    getClient().features().getFormulaCandidate(pid, featId, formId, of));

        return sourceCandidate;
    }

    private Optional<FormulaCandidate> sourceCandidate() {
        return Optional.ofNullable(sourceCandidate);
    }


    @NotNull
    public String getFormulaId() {
        return formulaId;
    }

    public void registerProjectSpaceListeners() {
        parentInstance.addPropertyChangeListener("instance.updateFormulaResult." + getFormulaId(), listener);
    }

    public void unregisterProjectSpaceListeners() {
        parentInstance.removePropertyChangeListener("instance.updateFormulaResult." + getFormulaId(), listener);
    }

    private synchronized <R> R withIds(TriFunction<String, String, String, R> doWithClient) {
        synchronized (parentInstance) {
            return doWithClient.apply(parentInstance.getProjectManager().getProjectId(), parentInstance.getFeatureId(), getFormulaId());
        }
    }

    //id based info
    @NotNull
    public String getAdduct() {
        return getFormulaCandidate().getAdduct();
    }

    @NotNull
    public PrecursorIonType getAdductObj() {
        return PrecursorIonType.fromString(getFormulaCandidate().getAdduct());
    }

    @NotNull
    public int getCharge() {
        return getAdductObj().getCharge();
    }

    @NotNull
    public String getPrecursorFormulaWithCharge() {
        return getPrecursorFormula() + (getAdductObj().isPositive() ? "+" : "-");
    }

    @NotNull
    public MolecularFormula getPrecursorFormula() {
        return getAdductObj().neutralMoleculeToPrecursorIon(getMolecularFormulaObj());
    }

    @NotNull
    public String getMolecularFormula() {
        return getFormulaCandidate().getMolecularFormula();
    }

    @NotNull
    public MolecularFormula getMolecularFormulaObj() {
        return MolecularFormula.parseOrThrow(getMolecularFormula());
    }

    public boolean isLipid() {
        return getLipidAnnotation().map(l -> l.getLipidSpecies() != null).orElse(false);
    }


    private final static Pattern pat = Pattern.compile("^\\s*\\[\\s*M\\s*|\\s*\\]\\s*\\d*\\s*[\\+\\-]\\s*$");

    public String getFormulaAndIonText() {
        final String ionType = getAdduct();
        final String mf = getMolecularFormula();
        String niceName = ionType;
        niceName = pat.matcher(niceName).replaceAll("");
        if (PrecursorIonType.fromString(ionType).isIonizationUnknown()) {
            return mf.toString();
        } else {
            return mf.toString() + " " + niceName;
        }
    }


    public Optional<Integer> getRank() {
        return Optional.ofNullable(getFormulaCandidate().getRank());
    }

    public Optional<Double> getSiriusScore() {
        return Optional.ofNullable(getFormulaCandidate().getSiriusScore());
    }

    public Optional<Double> getIsotopeScore() {
        return Optional.ofNullable(getFormulaCandidate().getIsotopeScore());
    }

    public Optional<Double> getTreeScore() {
        return Optional.ofNullable(getFormulaCandidate().getTreeScore());
    }

    public Optional<Double> getZodiacScore() {
        return Optional.ofNullable(getFormulaCandidate().getZodiacScore());
    }

    public Optional<Integer> getNumOfExplainedPeaks() {
        return Optional.ofNullable(getFormulaCandidate().getNumOfExplainedPeaks());
    }

    public Optional<Integer> getNumOfExplainablePeaks() {
        return Optional.ofNullable(getFormulaCandidate().getNumOfExplainablePeaks());
    }

    public Optional<Double> getTotalExplainedIntensity() {
        return Optional.ofNullable(getFormulaCandidate().getTotalExplainedIntensity());
    }

    public Optional<Deviation> getMedianMassDeviation() {
        return Optional.ofNullable(getFormulaCandidate().getMedianMassDeviation());
    }

    public synchronized Optional<FragmentationTree> getFragmentationTree() {
        if (this.fragmentationTree == null)
            this.fragmentationTree = Optional.ofNullable(
                    sourceCandidate().map(FormulaCandidate::getFragmentationTree)
                            .orElse(withIds((pid, fid, foid) -> getClient().features()
                                    .getFragTreeWithResponseSpec(pid, fid, foid)
                                    .bodyToMono(FragmentationTree.class).onErrorComplete().block()
                            )));

        return this.fragmentationTree;
    }

    public synchronized Optional<String> getFTreeJson() {
        if (this.ftreeJson == null)
            this.ftreeJson = Optional.ofNullable(withIds((pid, fid, foid) ->
                    getClient().features().getSiriusFragTreeWithResponseSpec(pid, fid, foid)
                            .bodyToMono(String.class).onErrorComplete().block()
            ));

        return this.ftreeJson;
    }

    public synchronized Optional<FTree> getFTree() {
        return getFTreeJson().map(s -> {
            try {
                return new FTJsonReader().treeFromJsonString(s, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    public synchronized Optional<IsotopePatternAnnotation> getIsotopePatterAnnotation() {
        if (this.isotopePatterAnnotation == null)
            this.isotopePatterAnnotation = Optional.ofNullable(
                    sourceCandidate().map(FormulaCandidate::getIsotopePatternAnnotation)
                            .orElse(withIds((pid, fid, foid) -> getClient().features()
                                    .getIsotopePatternAnnotationWithResponseSpec(pid, fid, foid)
                                    .bodyToMono(IsotopePatternAnnotation.class).onErrorComplete().block()
                            )));


        return this.isotopePatterAnnotation;
    }

    public synchronized Optional<LipidAnnotation> getLipidAnnotation() {
        if (this.lipidAnnotation == null) {
            this.lipidAnnotation = Optional.ofNullable(
                    sourceCandidate().map(FormulaCandidate::getLipidAnnotation)
                            .orElse(withIds((pid, fid, foid) -> getClient().features()
                                    .getLipidAnnotationWithResponseSpec(pid, fid, foid)
                                    .bodyToMono(LipidAnnotation.class).onErrorComplete().block()
                            )));
        }
        return this.lipidAnnotation;
    }

    public synchronized Optional<ProbabilityFingerprint> getPredictedFingerprint() {
        if (this.fp == null) {
            List<Double> fpTmp = sourceCandidate().map(FormulaCandidate::getPredictedFingerprint)
                    .orElse(withIds((pid, fid, foid) -> getClient().features()
                            .getFingerprintPredictionWithResponseSpec(pid, fid, foid))
                            .bodyToFlux(Double.class)
                            .collect(Collectors.toCollection(DoubleArrayList::new))
                            .onErrorComplete().block()
                    );


            this.fp = Optional.ofNullable(fpTmp)
                    .map(fpRaw -> new ProbabilityFingerprint(parentInstance.
                            getProjectManager().getFingerIdData(getCharge()).getFingerprintVersion(), fpRaw));
        }
        return this.fp;
    }

    @NotNull
    private synchronized Pair<CompoundClasses, CanopusPrediction> getCanopusResults() {
        if (canopusResults == null) {
            @NotNull FormulaCandidate f = sourceCandidate().filter(fc -> fc.getCanopusPrediction() != null && fc.getCompoundClasses() != null)
                    .orElse(getFormulaCandidate(FormulaCandidateOptField.CANOPUSPREDICTIONS, FormulaCandidateOptField.COMPOUNDCLASSES));
            canopusResults = Pair.of(f.getCompoundClasses(), f.getCanopusPrediction());
        }
        return canopusResults;
    }

    public Optional<CompoundClasses> getCompoundClasses() {
        return Optional.ofNullable(getCanopusResults().first());
    }

    public Optional<CanopusPrediction> getCanopusPrediction() {
        return Optional.ofNullable(getCanopusResults().second());
    }

    @Override
    public int compareTo(FormulaResultBean o) {
        return Double.compare(
                o.getSiriusScore().orElse(Double.NaN),
                getSiriusScore().orElse(Double.NaN)
        );
    }
}
