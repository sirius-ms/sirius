package de.unijena.bioinf.io;

import de.isas.mztab2.io.MZTabParameter;
import de.isas.mztab2.io.SiriusMZTabParameter;
import de.isas.mztab2.io.SiriusWorkspaceMzTabNonValidatingWriter;
import de.isas.mztab2.io.SiriusWorkspaceMzTabValidatingWriter;
import de.isas.mztab2.model.*;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.AdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.FingerIdResultWriter;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.ac.ebi.pride.jmztab2.model.MZTabConstants;
import uk.ac.ebi.pride.jmztab2.model.MZTabUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static de.unijena.bioinf.io.JenaMSAdditionalKeys.*;

public class MztabMExporter {
    private int smlID = 0;
    private int smfID = 0;
    private int smeID = 0;
    private final MzTab mztab;

    private boolean fingerID = false;

    private final FingerIdResultWriter.Locations locations;
    private final Map<String, MsRun> pathToRun = new HashMap<>();
    private final List<Parameter> confidenceMeasures = new ArrayList<>();


    public MztabMExporter(@NotNull FingerIdResultWriter.Locations locations) {
        this.locations = locations;
        mztab = new MzTab();
        mztab.setMetadata(
                buildMTDBlock()
        );
    }

    public MztabMExporter(@NotNull FingerIdResultWriter.Locations locations, String title, String id) {
        this(locations);
        setTitle(title);
        setID(id);
    }

//    public MzTab getMztab() {
//        return mztab;
//    }


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

    public void addExperiment(@NotNull final ExperimentResult er, @NotNull final List<IdentificationResult> results) {
        Scored<FingerprintCandidate> bestHit = null;
        IdentificationResult bestHitSource = null;


        for (IdentificationResult result : results) {
            final FingerIdResult r = result.getAnnotationOrNull(FingerIdResult.class);

            if (r != null && r.getCandidates() != null) {
                final Scored<FingerprintCandidate> localBest = r.getCandidates().stream().min(Scored.desc()).orElse(null);

                if (localBest != null) {
                    if (bestHit == null || localBest.getScore() > bestHit.getScore()) {
                        bestHit = localBest;
                        bestHitSource = result;
                    }
                }

            }
        }

        if (bestHitSource == null)
            bestHitSource = results.stream().min(IdentificationResult::compareTo).orElse(null);


        if (bestHitSource != null) {
            final SmallMoleculeSummary smlItem = buildSMLItem(er, bestHitSource, bestHit);
            mztab.addSmallMoleculeSummaryItem(smlItem);


            final SmallMoleculeFeature smfItem = buildSMFItem(er, bestHitSource, smlItem);
            mztab.addSmallMoleculeFeatureItem(smfItem);


            final List<SpectraRef> spectraRefs = extractReferencesAnRuns(er);

            final SmallMoleculeEvidence smeSiriusItem = buildSiriusFormulaIDSMEItem(er, bestHitSource, smfItem);
            smeSiriusItem.setSpectraRef(spectraRefs);
            mztab.addSmallMoleculeEvidenceItem(smeSiriusItem);


            if (bestHit != null) {
                final SmallMoleculeEvidence smeFingerIDItem = buildFingerIDSMEItem(er, bestHitSource, bestHit, smfItem);
                smeFingerIDItem.setSpectraRef(spectraRefs);
                mztab.addSmallMoleculeEvidenceItem(smeFingerIDItem);
                smlItem.setReliability("2");

                smlItem.setBestIdConfidenceMeasure(SiriusMZTabParameter.CSI_FINGERID_CONFIDENCE_SCORE);
                smlItem.setBestIdConfidenceValue(smeFingerIDItem.getIdConfidenceMeasure().get(0));


                List<String> ids = Arrays.stream(bestHit.getCandidate().getLinks())
                        .filter(dbLink -> dbLink.name.equals(DatasourceService.Sources.PUBCHEM.name)).map(dbLink -> dbLink.id).collect(Collectors.toList());

                smlItem.setDatabaseIdentifier(
                        ids.stream().map(dbLink -> "CID:" + dbLink)
                                .collect(Collectors.toList())
                );

                smlItem.setUri(
                        ids.stream().map(DatasourceService.Sources.PUBCHEM::getLink)
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


    private SmallMoleculeEvidence buildSiriusSMEItem(@NotNull final ExperimentResult er, @NotNull final IdentificationResult bestHitSource, @NotNull final SmallMoleculeFeature smfItem) {
        SmallMoleculeEvidence smeItem = buildSMEItem(smfItem);

        smeItem.setMsLevel(MZTabParameter.newInstance(MZTabParameter.MS_LEVEL).value("2"));
        smeItem.setCharge(bestHitSource.getPrecursorIonType().getCharge());
        smeItem.setAdductIon(bestHitSource.getPrecursorIonType().toString());
        smeItem.setChemicalFormula(bestHitSource.getMolecularFormula().toString());
        smeItem.setTheoreticalMassToCharge(bestHitSource.getPrecursorIonType().addIonAndAdduct(bestHitSource.getMolecularFormula().getMass()));
        smeItem.setExpMassToCharge(er.getExperiment().getIonMass());

        return smeItem;
    }

    private SmallMoleculeEvidence buildSiriusFormulaIDSMEItem(@NotNull final ExperimentResult er, @NotNull final IdentificationResult bestHitSource, @NotNull final SmallMoleculeFeature smfItem) {
        SmallMoleculeEvidence smeItem = buildSiriusSMEItem(er, bestHitSource, smfItem);

        smeItem.setIdentificationMethod(SiriusMZTabParameter.SOFTWARE_SIRIUS);
        smeItem.setRank(bestHitSource.getRank());
        smeItem.setEvidenceInputId(locations.makeMassIdentifier(er, bestHitSource));

        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_SCORE, String.valueOf(bestHitSource.getScore())));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_ISOTOPE_SCORE, String.valueOf(bestHitSource.getIsotopeScore())));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_TREE_SCORE, String.valueOf(bestHitSource.getTreeScore())));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_EXPL_INTENSITY_RATIO, String.valueOf(bestHitSource.getExplainedIntensityRatio())));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_EXPL_PEAKS, String.valueOf(bestHitSource.getNumOfExplainedPeaks())));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_EXPL_PEAKS, String.valueOf(bestHitSource.getExplainedIntensityRatio())));
//        smeItem.addOptItem(SiriusMZTabParameter.newOptColumnParameter(SiriusMZTabParameter.SIRIUS_MED_ABS_MASS_DEVIATION,));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_ANNOTATED_SPECTRA_LOCATION, locations.SIRIUS_ANNOTATED_SPECTRA.path(er, bestHitSource)));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_TREE_LOCATION, locations.SIRIUS_TREES_JSON.path(er, bestHitSource)));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.SIRIUS_CANDIDATE_LOCATION, locations.SIRIUS_SUMMARY.path(er, bestHitSource)));

        return smeItem;
    }

    private SmallMoleculeEvidence buildFingerIDSMEItem(@NotNull final ExperimentResult er, @NotNull final IdentificationResult bestHitSource, @NotNull final Scored<FingerprintCandidate> bestHit, @NotNull final SmallMoleculeFeature smfItem) {
        SmallMoleculeEvidence smeItem = buildSiriusSMEItem(er, bestHitSource, smfItem);
        smeItem.setIdentificationMethod(SiriusMZTabParameter.SOFTWARE_FINGER_ID);
        smeItem.setRank(1); //todo make exported result user definable in gui
        smeItem.setEvidenceInputId(locations.makeFormulaIdentifier(er, bestHitSource));

        smeItem.setChemicalName(bestHit.getCandidate().getName());
        smeItem.setInchi(bestHit.getCandidate().getInchi().in2D);
        smeItem.setSmiles(bestHit.getCandidate().getSmiles());

        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.FINGERID_SCORE, String.valueOf(bestHit.getScore())));
//        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.FINGERID_CONFIDENCE, null));

        smeItem.addIdConfidenceMeasureItem(null);//todo add configdence score


//        smeItem.addOptItem(SiriusMZTabParameter.newOptColumnParameter(SiriusMZTabParameter.FINGERID_TANIMOTO_SIMILARITY, bestHit.getCandidate()));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.FINGERID_FINGERPRINT_LOCATION, locations.FINGERID_FINGERPRINT.path(er, bestHitSource)));
        smeItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.FINGERID_CANDIDATE_LOCATION, locations.FINGERID_SUMMARY.path(er, bestHitSource)));


        return smeItem;
    }

    private SmallMoleculeEvidence buildSpectralLibSMEItem(@NotNull ExperimentResult er, final IdentificationResult bestHitSource, final Scored<FingerprintCandidate> bestHit, final SmallMoleculeFeature smfItem) {
        SmallMoleculeEvidence smeItem = buildSMEItem(smfItem);
        //todo implement if available through zodiac
        return smeItem;
    }

    private SmallMoleculeEvidence buildSMEItem(final SmallMoleculeFeature smfItem) {
        SmallMoleculeEvidence smeItem = new SmallMoleculeEvidence();
        smeItem.setSmeId(++smeID);
        smfItem.addSmeIdRefsItem(smeItem.getSmeId());
//        smeItem.setEvidenceInputId(); //todo maybe we can use this for openms mapping stuff
        return smeItem;
    }

    private SmallMoleculeFeature buildSMFItem(@NotNull ExperimentResult er, @NotNull final IdentificationResult bestHitSource, @NotNull final SmallMoleculeSummary smlItem) {
        final SmallMoleculeFeature smfItem = new SmallMoleculeFeature();
        smfItem.setSmfId(++smfID);
        smfItem.smeIdRefAmbiguityCode(2); //todo 3 is needed if we also want to add multiple candidates
        smlItem.addSmfIdRefsItem(smfItem.getSmfId());

        smfItem.setAdductIon(bestHitSource.getPrecursorIonType().toString());
        smfItem.setCharge(bestHitSource.getPrecursorIonType().getCharge());
        smfItem.setExpMassToCharge(er.getExperiment().getIonMass());

        //add retention time if available
        RetentionTime rt = er.getExperiment().getAnnotation(RetentionTime.class);
        if (rt != null) {
            smfItem.setRetentionTimeInSeconds(rt.getRetentionTimeInSeconds());
            if (!Double.isNaN(rt.getStartTime()))
                smfItem.setRetentionTimeInSecondsStart(rt.getStartTime());
            if (!Double.isNaN(rt.getEndTime()))
                smfItem.setRetentionTimeInSecondsEnd(rt.getEndTime());
        }

        return smfItem;
    }

    public void setTitle(String title) {
        mztab.getMetadata().title("SIRIUS Workspace Summary: " + title);

    }

    public void setID(String ID) {
        mztab.getMetadata().mzTabID(ID); //todo add workspace file parameterName here
    }

    private SmallMoleculeSummary buildSMLItem(@NotNull ExperimentResult er, @NotNull IdentificationResult bestHitSource, @Nullable Scored<FingerprintCandidate> bestHit) {
        final SmallMoleculeSummary smlItem = new SmallMoleculeSummary();
        smlItem.setSmlId(++smlID);
        smlItem.adductIons(Collections.singletonList(bestHitSource.getPrecursorIonType().toString()));
        smlItem.addChemicalFormulaItem(bestHitSource.getMolecularFormula().toString());
        smlItem.addTheoreticalNeutralMassItem(bestHitSource.getMolecularFormula().getMass());

        if (bestHit != null) {
            smlItem.addChemicalNameItem(bestHit.getCandidate().getName());
            smlItem.addInchiItem(bestHit.getCandidate().getInchi().in2D);
            smlItem.addSmilesItem(bestHit.getCandidate().getSmiles());
        }

        AdditionalFields fields = er.getExperiment().getAnnotation(AdditionalFields.class);
        if (fields != null) {
            if (fields.containsKey(FEATURE_ID)) {
                smlItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.OPENMS_FEATURE_ID, fields.get(FEATURE_ID)));
            }
            if (fields.containsKey(CONSENSUS_ID)) {
                smlItem.addOptItem(SiriusMZTabParameter.newOptColumn(SiriusMZTabParameter.OPENMS_CONSENSUS_ID, fields.get(CONSENSUS_ID)));
            }
        }

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

    public List<SpectraRef> extractReferencesAnRuns(@NotNull ExperimentResult re) {
        List<Spectrum> specs = new ArrayList<>(re.getExperiment().getMs2Spectra().size() + re.getExperiment().getMs1Spectra().size());
        specs.addAll(re.getExperiment().getMs1Spectra());
        specs.addAll(re.getExperiment().getMs2Spectra());


        return specs.stream().map((it) -> {
            if (it instanceof AnnotatedSpectrum)
                return (AdditionalFields) ((AnnotatedSpectrum) it).getAnnotation(AdditionalFields.class);

            return null;
        }).filter(Objects::nonNull).map((it) -> {
            SpectraRef ref = new SpectraRef();
            String specref = it.get(SPECTRUM_ID);
            if (specref != null) {
                if (specref.startsWith("ms_run[") && specref.contains("]:")) //todo pattern matcher
                    specref = specref.split(":", 2)[1];
                ref.setReference(specref);
            }

            String source = it.get(SOURCE_FILE);
            if (source != null) {
                MsRun run = pathToRun.get(source);
                if (run == null) {
                    run = new MsRun().id(pathToRun.size() + 1).location(source);
                    pathToRun.put(source, run);
                }

                if (run.getFormat() == null && it.containsKey(SOURCE_FILE_FORMAT))
                    run.setFormat(MZTabUtils.parseParam(it.get(SOURCE_FILE_FORMAT)));
                if (run.getIdFormat() == null && it.containsKey(SPECTRUM_ID_FORMAT))
                    run.setIdFormat(MZTabUtils.parseParam(it.get(SPECTRUM_ID_FORMAT)));

                ref.setMsRun(run);
            }

            return ref;
        }).collect(Collectors.toCollection(ArrayList::new));
    }
}
