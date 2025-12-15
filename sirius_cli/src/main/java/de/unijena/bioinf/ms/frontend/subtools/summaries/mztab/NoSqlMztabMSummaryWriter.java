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

package de.unijena.bioinf.ms.frontend.subtools.summaries.mztab;

import de.isas.mztab2.io.MZTabParameter;
import de.isas.mztab2.io.SiriusMZTabParameter;
import de.isas.mztab2.io.SiriusWorkspaceMzTabNonValidatingWriter;
import de.isas.mztab2.io.SiriusWorkspaceMzTabValidatingWriter;
import de.isas.mztab2.model.*;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiStructureMatch;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiStructureSearchResult;
import de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate;
import de.unijena.bioinf.ms.persistence.model.sirius.StructureMatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.ac.ebi.pride.jmztab2.model.MZTabConstants;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

public class NoSqlMztabMSummaryWriter implements AutoCloseable {
    private final Writer writer;
    private int smlID = 0;
    private int smfID = 0;
    private int smeID = 0;
    private final MzTab mztab;

    private boolean fingerID = false;

    private final Map<String, MsRun> pathToRun = new HashMap<>();

    public NoSqlMztabMSummaryWriter(Writer writer) {
        this.writer = writer;
        this.mztab = new MzTab();
        this.mztab.setMetadata(
                buildMTDBlock()
        );

        UUID id = UUID.randomUUID();
        setID("SIRIUS-" + id.toString());
        setTitle("SIRIUS Analysis Report: " + id.toString());
    }

    public NoSqlMztabMSummaryWriter(Writer writer, String title, String id) {
        this.writer = writer;
        this.mztab = new MzTab();
        this.mztab.setMetadata(
                buildMTDBlock()
        );
        setTitle(title);
        setID(id);
    }


    public static void write(final Writer writer, final MzTab mztab) throws IOException {
        write(writer, mztab, false);
    }

    public static void write(final Writer writer, final MzTab mztab, final boolean validate) throws IOException {
        if (validate)
            new SiriusWorkspaceMzTabValidatingWriter().write(writer, mztab);
        else
            new SiriusWorkspaceMzTabNonValidatingWriter().write(writer, mztab);
    }

    public void write(final Writer writer) throws IOException {
        write(writer, false);
    }

    public void write(final Writer writer, final boolean validate) throws IOException {
        write(writer, mztab, validate);
    }


    public synchronized void writeFeatureResult(@NotNull AlignedFeatures feature, @Nullable FormulaCandidate bestHitSource, @Nullable CsiStructureMatch bestHit, @Nullable CsiStructureSearchResult searchResult) throws IOException {
        if (bestHitSource != null) {
            final SmallMoleculeSummary smlItem = buildSMLItem(feature, bestHitSource, bestHit);
            mztab.addSmallMoleculeSummaryItem(smlItem);

            final SmallMoleculeFeature smfItem = buildSMFItem(feature, bestHitSource, smlItem);
            mztab.addSmallMoleculeFeatureItem(smfItem);

            //todo add spec ref again
//            final List<SpectraRef> spectraRefs = extractReferencesAndRuns(feature);

            final SmallMoleculeEvidence smeSiriusItem = buildSiriusFormulaIDSMEItem(feature, bestHitSource, smfItem);
//todo add spec ref again
            //            smeSiriusItem.setSpectraRef(spectraRefs);
            mztab.addSmallMoleculeEvidenceItem(smeSiriusItem);


            if (bestHit != null) {
                final SmallMoleculeEvidence smeFingerIDItem = buildFingerIDSMEItem(feature, bestHitSource, bestHit, searchResult, smfItem);
//todo add spec ref again
                //                smeFingerIDItem.setSpectraRef(spectraRefs);
                mztab.addSmallMoleculeEvidenceItem(smeFingerIDItem);
                smlItem.setReliability("2");

                smlItem.setBestIdConfidenceMeasure(SiriusMZTabParameter.CSI_FINGERID_CONFIDENCE_SCORE);
                smlItem.setBestIdConfidenceValue(smeFingerIDItem.getIdConfidenceMeasure().get(0));


                List<String> ids = bestHit.getCandidate().getLinks().stream()
                        .filter(dbLink -> Objects.equals(dbLink.getName(), DataSource.PUBCHEM.name())).map(DBLink::getId)
                        .toList();

                smlItem.setDatabaseIdentifier(
                        ids.stream().map(dbLink -> "CID:" + dbLink)
                                .collect(Collectors.toList())
                );

                smlItem.setUri(
                        ids.stream().map(DataSource.PUBCHEM::getLink)
                                .collect(Collectors.toList())
                );

                if (!fingerID) {
                    fingerID = true;
                    mztab.getMetadata().addSoftwareItem(new Software().id(2)
                            .parameter(SiriusMZTabParameter.SOFTWARE_FINGER_ID)
                    );

                    mztab.getMetadata().addIdConfidenceMeasureItem(SiriusMZTabParameter.CSI_FINGERID_CONFIDENCE_SCORE);
                }
            } else {
                smlItem.setReliability("4");
            }
            //todo add zodiac spectral library hits at some time
//        final SmallMoleculeEvidence smeSpectralHitItem = buildSpectralLibSMEItem(bestHitSource, bestHit, smfItem);
//        mztab.addSmallMoleculeEvidenceItem(smeSpectralHitItem);
            mztab.getMetadata().setMsRun(new ArrayList<>(pathToRun.values()));
        }
    }

    private SmallMoleculeEvidence buildSiriusSMEItem(@NotNull final AlignedFeatures er, @NotNull final FormulaCandidate bestHitSource, @NotNull final SmallMoleculeFeature smfItem) {
        SmallMoleculeEvidence smeItem = buildSMEItem(smfItem);

        smeItem.setMsLevel(MZTabParameter.newInstance(MZTabParameter.MS_LEVEL).value("2"));
        smeItem.setCharge(bestHitSource.getAdduct().getCharge());
        smeItem.setAdductIon(bestHitSource.getAdduct().toString());
        smeItem.setChemicalFormula(bestHitSource.getMolecularFormula().toString());
        smeItem.setTheoreticalMassToCharge(bestHitSource.getPrecursorFormula().getMass()); //todo check if this is correct.
        smeItem.setExpMassToCharge(er.getAverageMass());

        return smeItem;
    }

    private SmallMoleculeEvidence buildSiriusFormulaIDSMEItem(@NotNull final AlignedFeatures feature, @NotNull final FormulaCandidate bestHitSource, @NotNull final SmallMoleculeFeature smfItem) {
        SmallMoleculeEvidence smeItem = buildSiriusSMEItem(feature, bestHitSource, smfItem);

        smeItem.setIdentificationMethod(SiriusMZTabParameter.SOFTWARE_SIRIUS);
        smeItem.setRank(bestHitSource.getFormulaRank());
        //todo add evidence identifier
//        smeItem.setEvidenceInputId(makeMassIdentifier(feature, bestHitSource));

//        @NotNull final FormulaScoring scoring = bestHitSource.getAnnotationOrThrow(FormulaScoring.class);
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_SCORE_NORM, String.valueOf(bestHitSource.getSiriusScoreNormalized())));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_SCORE, String.valueOf(bestHitSource.getSiriusScore())));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_ISOTOPE_SCORE, String.valueOf(bestHitSource.getIsotopeScore())));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_TREE_SCORE, String.valueOf(bestHitSource.getTreeScore())));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.ZODIAC_SCORE, String.valueOf(bestHitSource.getZodiacScore())));

//        todo add tree Stats
//        @NotNull final FTree tree = null;
//        @NotNull final TreeStatistics treeStats = tree.getAnnotationOrThrow(TreeStatistics.class);
//        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_EXPL_INTENSITY_OF_TOTAL_INTENSITY, String.valueOf(treeStats.getExplainedIntensity())));
//        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_EXPL_INTENSITY_OF_EXPLAINABLE_INTENSITY, String.valueOf(treeStats.getExplainedIntensityOfExplainablePeaks())));
//        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_NUM_EXPL_PEAKS_RATIO, String.valueOf(treeStats.getRatioOfExplainedPeaks())));

        //todo add api links to sources instead!
//        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_ANNOTATED_SPECTRA_LOCATION, SPECTRA.relFilePath(bestHitSource.getId())));
//        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_TREE_LOCATION, TREES.relFilePath(bestHitSource.getId())));
//        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_SUMMARY_LOCATION, bestHitSource.getId().getParentId().getDirectoryName() + "/" + SummaryLocations.FORMULA_CANDIDATES));

        return smeItem;
    }

    private SmallMoleculeEvidence buildFingerIDSMEItem(@NotNull final AlignedFeatures feature, @NotNull final FormulaCandidate bestHitSource, @NotNull final StructureMatch bestHit, @NotNull final CsiStructureSearchResult bestHitSearchResult, @NotNull final SmallMoleculeFeature smfItem) {
        SmallMoleculeEvidence smeItem = buildSiriusSMEItem(feature, bestHitSource, smfItem);
        smeItem.setIdentificationMethod(SiriusMZTabParameter.SOFTWARE_FINGER_ID);
        smeItem.setRank(1); //todo make exported result user definable in gui
        //todo add evidence identifier.
//        smeItem.setEvidenceInputId(makeFormulaIdentifier(feature, bestHitSource));

        smeItem.setChemicalName(bestHit.getCandidate().getName());
        smeItem.setInchi(bestHit.getCandidate().getInchi().in2D);
        smeItem.setSmiles(bestHit.getCandidate().getSmiles());

        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.FINGERID_SCORE, String.valueOf(bestHit.getCsiScore())));
//        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.FINGERID_TANIMOTO_SIMILARITY, bestHit.getCandidate().));

        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.FINGERID_CONFIDENCE, String.valueOf(bestHitSearchResult.getConfidenceExact())));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.FINGERID_CONFIDENCE_APPROX, String.valueOf(bestHitSearchResult.getConfidenceApprox())));
        smeItem.addIdConfidenceMeasureItem(bestHitSearchResult.getConfidenceExact());

        //todo add API links to sources
//        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.FINGERID_FINGERPRINT_LOCATION, FingerIdLocations.FINGERPRINTS.relFilePath(bestHitSource.getId())));
//        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.FINGERID_CANDIDATE_LOCATION, FingerIdLocations.FINGERBLAST.relFilePath(bestHitSource.getId())));


        return smeItem;
    }

    private SmallMoleculeEvidence buildSMEItem(final SmallMoleculeFeature smfItem) {
        SmallMoleculeEvidence smeItem = new SmallMoleculeEvidence();
        smeItem.setSmeId(++smeID);
        smfItem.addSmeIdRefsItem(smeItem.getSmeId());
//        smeItem.setEvidenceInputId(); //todo maybe we can use this for openms mapping stuff
        return smeItem;
    }

    private SmallMoleculeFeature buildSMFItem(@NotNull AlignedFeatures feature, @NotNull final FormulaCandidate bestHitSource, @NotNull final SmallMoleculeSummary smlItem) {
        final SmallMoleculeFeature smfItem = new SmallMoleculeFeature();
        smfItem.setSmfId(++smfID);
        smfItem.smeIdRefAmbiguityCode(2); //todo 3 is needed if we also want to add multiple candidates
        smlItem.addSmfIdRefsItem(smfItem.getSmfId());

        smfItem.setAdductIon(bestHitSource.getAdduct().toString());
        smfItem.setCharge(bestHitSource.getAdduct().getCharge());
        smfItem.setExpMassToCharge(feature.getAverageMass());

        //add retention time if available
        if (feature.getRetentionTime() != null){
            RetentionTime rt = feature.getRetentionTime();
            smfItem.setRetentionTimeInSeconds(rt.getRetentionTimeInSeconds());
            if (rt.isInterval()){
                if (!Double.isNaN(rt.getStartTime()))
                    smfItem.setRetentionTimeInSecondsStart(rt.getStartTime());
                if (!Double.isNaN(rt.getEndTime()))
                    smfItem.setRetentionTimeInSecondsEnd(rt.getEndTime());
            }
        }
        return smfItem;
    }

    public void setTitle(String title) {
        mztab.getMetadata().setTitle(title);

    }

    public void setID(String ID) {
        mztab.getMetadata().setMzTabID(ID); //todo add workspace file parameterName here
    }

    private SmallMoleculeSummary buildSMLItem(@NotNull AlignedFeatures feature, @NotNull FormulaCandidate bestHitSource, @Nullable StructureMatch bestHit) {
        final SmallMoleculeSummary smlItem = new SmallMoleculeSummary();
        smlItem.setSmlId(++smlID);
        smlItem.adductIons(Collections.singletonList(bestHitSource.getAdduct().toString()));
        smlItem.addChemicalFormulaItem(bestHitSource.getMolecularFormula().toString());
        smlItem.addTheoreticalNeutralMassItem(bestHitSource.getMolecularFormula().getMass());

        if (bestHit != null) {
            smlItem.addChemicalNameItem(bestHit.getCandidate().getName());
            smlItem.addInchiItem(bestHit.getCandidate().getInchi().in2D);
            smlItem.addSmilesItem(bestHit.getCandidate().getSmiles());
        }

        if (Utils.notNullOrBlank(feature.getExternalFeatureId())){
            smlItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.OPENMS_FEATURE_ID, feature.getExternalFeatureId()));
        }

        //todo what is consensus ID, do we need this?
//        if (fields.containsKey(CONSENSUS_ID)) {
//            smlItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.OPENMS_CONSENSUS_ID, fields.get(CONSENSUS_ID)));
//        }

        return smlItem;
    }

    private static Metadata buildMTDBlock() {
        Metadata mtd = new Metadata();
        mtd.mzTabVersion(MZTabConstants.VERSION_MZTAB_M); //this is the format not the library version
        mtd.addCvItem(SiriusMZTabParameter.DEFAULT_CV);
        mtd.setSmallMoleculeIdentificationReliability(SiriusMZTabParameter.SMALL_MOLECULE_IDENTIFICATION_RELIABILITY);


        mtd.addSoftwareItem(new Software().id(1)
                .parameter(SiriusMZTabParameter.SOFTWARE_SIRIUS)
        );

        mtd.addDatabaseItem(SiriusMZTabParameter.NO_DATABASE.id(1));
//        mtd.addDatabaseItem(SiriusMZTabParameter.DE_NOVO);
        mtd.addDatabaseItem(SiriusMZTabParameter.PUBCHEM.id(2));


        return mtd;
    }

    /*public List<SpectraRef> extractReferencesAndRuns(@NotNull AlignedFeatures feature) {
        feature.getFeatures().get().getFirst().getTraceReference().get();
        feature.getMSData();
        List<Spectrum<?>> specs = new ArrayList<>(feature.getMs2Spectra().size() + exp.getMs1Spectra().size() + 1);
        specs.add(exp.getMergedMs1Spectrum());
        specs.addAll(exp.getMs1Spectra());
        specs.addAll(exp.getMs2Spectra());

        final AdditionalFields global = exp.getAnnotation(AdditionalFields.class).orElse(new AdditionalFields());

        final String globalSource = global.getOrDefault(SOURCE_FILE, Optional.ofNullable(exp.getSourceString()).orElse(null));

        return specs.stream().map((it) -> {
            if (it instanceof AnnotatedSpectrum)
                return (AdditionalFields) ((AnnotatedSpectrum) it).getAnnotationOrNull(AdditionalFields.class);
            else if (it instanceof SpectrumWithAdditionalFields<?>)
                return ((SpectrumWithAdditionalFields<?>) it).additionalFields();

            return null;
        }).filter(Objects::nonNull).map((it) -> {
            SpectraRef ref = new SpectraRef();
            String specref = it.get(SPECTRUM_ID);
            Integer runID = null;
//            String scanNumber =  it.get(SCAN_NUMBER);
//            if (scanNumber != null)


            if (specref == null)
                return null;

            if (specref.startsWith("ms_run[") && specref.contains("]:")) { //todo pattern matcher
                final String[] s = specref.split(":", 2);
                specref = s[1];
                try {
                    runID = Integer.parseInt(s[0].substring(s[0].indexOf('[') + 1, s[0].indexOf(']')));
                } catch (NumberFormatException e) {
                    runID = null;
                }
            }
            ref.setReference(specref);

            String source = it.getOrDefault(SOURCE_FILE, globalSource);
            if (source == null)
                return null;

            MsRun run = pathToRun.get(source);
            if (run == null) {
                run = new MsRun()
                        .id(runID != null ? runID : pathToRun.size() + 1)
                        .location(source);
                pathToRun.put(source, run);
            }

            if (run.getFormat() == null && it.containsKey(SOURCE_FILE_FORMAT))
                run.setFormat(MZTabUtils.parseParam(it.get(SOURCE_FILE_FORMAT)));
            if (run.getIdFormat() == null && it.containsKey(SPECTRUM_ID_FORMAT))
                run.setIdFormat(MZTabUtils.parseParam(it.get(SPECTRUM_ID_FORMAT)));

            final Parameter polarity = SiriusMZTabParameter.getScanPolarity(feature.getCharge());
            if (polarity != null && (run.getScanPolarity() == null || !run.getScanPolarity().contains(polarity)))
                run.addScanPolarityItem(polarity);

            ref.setMsRun(run);


            return ref;
        }).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
    }*/

   /* protected static String makeFormulaIdentifier(String projectDir, AlignedFeatures feature, FormulaCandidate result) {
        return projectDir  + ":" + result.getMolecularFormula() + ":" + res;
    }

    protected String makeMassIdentifier(String projectDir, AlignedFeatures feature, FormulaCandidate result) {
        try {
            return  projectDir + ":" + feature.getAverageMass() + ":" + result.getFormulaId();
        } catch (Exception e) {
//            System.out.println("Instance was not written?????????? -> " + ex.getName());
            throw e;
        }
    }*/

    @Override
    public void close() throws Exception {
        try {
            write(writer);
            writer.flush();
        } finally {
            writer.close();
        }
    }
   }
