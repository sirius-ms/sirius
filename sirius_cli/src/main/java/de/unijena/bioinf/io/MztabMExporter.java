package de.unijena.bioinf.io;

import de.isas.mztab2.io.SiriusWorkspaceMzTabNonValidatingWriter;
import de.isas.mztab2.io.SiriusWorkspaceMzTabValidatingWriter;
import de.isas.mztab2.model.*;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.core.ApplicationCore;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MztabMExporter {

    public void writeWorkspaceSummary(final Writer writer, List<IdentificationResult> results) throws IOException {
        writeWorkspaceSummary(writer, results, false);
    }

    public void writeWorkspaceSummary(final Writer writer, List<IdentificationResult> results, boolean validate) throws IOException {
        MzTab mztab = new MzTab();
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

        if (validate)
            new SiriusWorkspaceMzTabValidatingWriter().write(writer, mztab);
        else
            new SiriusWorkspaceMzTabNonValidatingWriter().write(writer, mztab);
    }

    private SmallMoleculeEvidence buildSMEBlock() {
        return null;
    }

    private SmallMoleculeFeature buildSMFBlock() {
        return null;

    }

    private SmallMoleculeSummary buildSMLItem(IdentificationResult result) {
        final SmallMoleculeSummary smlItem = new SmallMoleculeSummary();
//        SmallMoleculeFeature smfItem =  new SmallMoleculeFeature();
//        SmallMoleculeEvidence smeItem = new SmallMoleculeEvidence();
        FingerIdResult r = result.getAnnotationOrNull(FingerIdResult.class);
        if (r != null) {
            final ArrayList<Scored<FingerprintCandidate>> candidates = r.getCandidates().stream().sorted(Scored.desc()).collect(Collectors.toCollection(ArrayList::new));
            final Scored<FingerprintCandidate> bestHit = candidates.get(0);


            smlItem.addChemicalNameItem(bestHit.getCandidate().getName());
            smlItem.adductIons(Collections.singletonList(result.getPrecursorIonType().toString()));
            smlItem.addChemicalFormulaItem(result.getMolecularFormula().toString());
            smlItem.addTheoreticalNeutralMassItem(result.getMolecularFormula().getMass());
            smlItem.addInchiItem(bestHit.getCandidate().getInchi().in2D);
            smlItem.addSmilesItem(bestHit.getCandidate().getSmiles());
        } else {
            //todo empty identification or skip???
        }

        return smlItem;

    }

    private Metadata buildMTDBlock() {
        Metadata mtd = new Metadata();
        mtd.mzTabVersion("2.0.0-M"); //this is the format not the library version
        mtd.mzTabID("sirius-"); //todo add workspace file name here

        mtd.addSoftwareItem(new Software().id(1).
                parameter(new Parameter().id(1).
                        name(PropertyManager.getProperty("de.unijena.bioinf.utils.errorReport.softwareName", "SIRIUS")).
                        value(ApplicationCore.VERSION_STRING))
        );

        return mtd;
    }
}
