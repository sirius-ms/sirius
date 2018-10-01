package de.unijena.bioinf.io;

import de.isas.mztab2.io.SiriusWorkspaceMzTabNonValidatingWriter;
import de.isas.mztab2.io.SiriusWorkspaceMzTabValidatingWriter;
import de.isas.mztab2.model.*;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import gnu.trove.map.TObjectIntMap;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MztabMExporter {
    private int smlID = 0;
    private int smfID = 0;
    private int smeID = 0;
    private final MzTab mztab;


//    List<SmallMoleculeSummary> summaries = new ArrayList<>();

//    List<Set<SmallMoleculeSummary>> adductIons = new ArrayList<>();
//    List<Set<Integer>> smfIDMapping = new ArrayList<>();

//    TObjectIntMap<MolecularFormula> formulaToSummary;
//    TObjectIntMap<String> inchiToSummary;



    public MztabMExporter() {
        mztab = new MzTab();
        mztab.setMetadata(
                buildMTDBlock()
        );
    }

    public MzTab getMztab() {
        return mztab;
    }


    /*public static void write(final Writer writer, List<IdentificationResult> results) throws IOException {
        write(writer, results, false);
    }

    public static void write(final Writer writer, List<IdentificationResult> results, boolean validate) throws IOException {
        final MzTab mztab = new MzTab();
        //do the meta information
        mztab.setMetadata(
                buildMTDBlock()
        );

        int sflID = 0;
        for (IdentificationResult result : results) {
            //build identification list
            final SmallMoleculeSummary smlItem = buildSMLItem(result);
            smlItem.setSmlId(sflID++);

            mztab.addSmallMoleculeSummaryItem(smlItem);

            //build best hits or each CSI:FingerID search list
//            buildSMFBlock(smlItem,result);
            //collect best k hits for each SME
//            buildSMEBlock();
        }

        write(writer, mztab, validate);
    }*/

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

    public void addExperiment(final ExperimentResult er, final List<IdentificationResult> results, final List<FingerIdResult> frs) {
        final SmallMoleculeSummary smlItem = buildSMLItem(results);
        smlItem.setSmlId(smlID++);

        mztab.addSmallMoleculeSummaryItem(smlItem);
    }


    private static SmallMoleculeEvidence buildSMEBlock() {
        return null;
    }

    private static SmallMoleculeFeature buildSMFBlock() {
        return null;

    }

    private static SmallMoleculeSummary buildSMLItem(List<IdentificationResult> results) {
        final SmallMoleculeSummary smlItem = new SmallMoleculeSummary();
        Scored<FingerprintCandidate> bestHit = null;
        IdentificationResult bestHitSource = null;

        for (IdentificationResult result : results) {
            final FingerIdResult r = result.getAnnotationOrNull(FingerIdResult.class);
            if (r == null || r.getCandidates().isEmpty())
                continue;
            final Scored<FingerprintCandidate> localBest = r.getCandidates().stream().sorted(Scored.desc())
                    .collect(Collectors.toCollection(ArrayList::new)).get(0);

            if (bestHit == null || localBest.getScore() > bestHit.getScore()) {
                bestHit = localBest;
                bestHitSource = result;
            }

        }

        smlItem.addChemicalNameItem(bestHit.getCandidate().getName());
        smlItem.adductIons(Collections.singletonList(bestHitSource.getPrecursorIonType().toString()));
        smlItem.addChemicalFormulaItem(bestHitSource.getMolecularFormula().toString());
        smlItem.addTheoreticalNeutralMassItem(bestHitSource.getMolecularFormula().getMass());
        smlItem.addInchiItem(bestHit.getCandidate().getInchi().in2D);
        smlItem.addSmilesItem(bestHit.getCandidate().getSmiles());

        return smlItem;
    }

    private static Metadata buildMTDBlock() {
        Metadata mtd = new Metadata();
        mtd.mzTabVersion("2.0.0-M"); //this is the format not the library version
        mtd.mzTabID("sirius-"); //todo add workspace file name here

        mtd.addSoftwareItem(new Software().id(1).
                parameter(new Parameter().id(1).
                        name(PropertyManager.getProperty("de.unijena.bioinf.utils.errorReport.softwareName", "SIRIUS")).
                        value(PropertyManager.getProperty("de.unijena.bioinf.ms.sirius.version", "SIRIUS"))
                )
        );

        return mtd;
    }
}
