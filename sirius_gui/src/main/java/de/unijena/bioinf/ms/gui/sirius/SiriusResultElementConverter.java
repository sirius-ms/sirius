package de.unijena.bioinf.ms.gui.sirius;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.ms.frontend.io.projectspace.FormulaResultBean;
import de.unijena.bioinf.myxo.gui.tree.structure.DefaultTreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.DefaultTreeNode;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@Deprecated //todo may not be needed anymore when using cli/api computation
public class SiriusResultElementConverter {

    public static FormulaResultBean convertResult(IdentificationResult res) {
        FormulaResultBean out = new FormulaResultBean(res);
        out.buildTreeVisualization(input -> convertTree(input));
        return out;
    }

    public static List<FormulaResultBean> convertResults(Iterable<IdentificationResult> in) {
        if (in == null) return Collections.emptyList();
        List<FormulaResultBean> outs = new ArrayList<>();
        for (IdentificationResult res : in) {
            outs.add(convertResult(res));
        }
        return outs;
    }

    public static TreeNode convertTree(FTree ft) {
        FragmentAnnotation<Peak> peakAno = ft.getOrCreateFragmentAnnotation(Peak.class);
        FragmentAnnotation<AnnotatedPeak> annoPeakAnno = ft.getFragmentAnnotationOrNull(AnnotatedPeak.class);
        LossAnnotation<Score> lscore = ft.getOrCreateLossAnnotation(Score.class);
        FragmentAnnotation<Score> fscore = ft.getOrCreateFragmentAnnotation(Score.class);
        ProcessedInput processedInput = ft.getAnnotationOrNull(ProcessedInput.class);
        ArrayList<Double> massDeviations = new ArrayList<Double>() ;

        Fragment rootK = ft.getRoot();
        TreeNode rootM = new DefaultTreeNode();
        double maxIntensity = Double.NEGATIVE_INFINITY;
        double maxRelIntensity = Double.NEGATIVE_INFINITY;

        for (Fragment fragment : ft.getFragments()) {
            if (peakAno.get(fragment) == null) continue;
            double fragIntensity = peakAno.get(fragment).getIntensity();
            if (fragIntensity > maxIntensity) maxIntensity = fragIntensity;
        }
        if (annoPeakAnno!=null){
            for (Fragment fragment : ft.getFragments()) {
                if (annoPeakAnno.get(fragment) == null) continue;
                double fragIntensity = annoPeakAnno.get(fragment).getRelativeIntensity();
                if (fragIntensity > maxRelIntensity) maxRelIntensity = fragIntensity;
            }
        }

        rootM.setMolecularFormula(rootK.getFormula().toString());
        rootM.setMolecularFormulaMass(rootK.getFormula().getMass());
        rootM.setIonization(rootK.getIonization().getName());

        if (peakAno.get(rootK) == null) {
            rootM.setPeakMass(ft.getAnnotationOrThrow(PrecursorIonType.class).getIonization().addToMass(rootK.getFormula().getMass()));
            rootM.setPeakRelativeIntensity(0d);
            rootM.setPeakAbsoluteIntenstiy(0d);
            rootM.setScore(0d);
        } else {
            if (annoPeakAnno!=null){
                rootM.setPeakMass(annoPeakAnno.get(rootK).getMass());
                rootM.setPeakRelativeIntensity(annoPeakAnno.get(rootK).getRelativeIntensity()/maxRelIntensity);
                rootM.setPeakAbsoluteIntenstiy(annoPeakAnno.get(rootK).getSumedIntensity());
            } else {
                rootM.setPeakMass(peakAno.get(rootK).getMass());
                rootM.setPeakRelativeIntensity(peakAno.get(rootK).getIntensity() / maxIntensity);
                rootM.setPeakAbsoluteIntenstiy(peakAno.get(rootK).getIntensity());
            }
            rootM.setScore(fscore.get(rootK).sum());
        }

        calculateDeviatonMassInPpm(rootM, rootK.getIonization(), massDeviations);

        convertNode(ft, rootK, rootM, peakAno, annoPeakAnno, lscore, fscore, maxIntensity, maxRelIntensity, massDeviations);

        Collections.sort(massDeviations);
        rootM.setMedianMassDeviation(massDeviations.get(massDeviations.size()/2));
        return rootM;
    }

    private static void convertNode(FTree ft, Fragment sourceK, TreeNode sourceM, FragmentAnnotation<Peak> peakAno, FragmentAnnotation<AnnotatedPeak> annoPeakAnno, LossAnnotation<Score> lscore, FragmentAnnotation<Score> fscore, double maxIntensity, double maxRelIntensityOfAnnoPeakAnno, ArrayList<Double> massDeviations ) {
        for (Loss edgeK : sourceK.getOutgoingEdges()) {
            Fragment targetK = edgeK.getTarget();

            DefaultTreeNode targetM = new DefaultTreeNode();
            targetM.setMolecularFormula(targetK.getFormula().toString());
            targetM.setMolecularFormulaMass(targetK.getFormula().getMass());
            targetM.setIonization(targetK.getIonization().getName());

            if (peakAno.get(targetK) == null) {
                targetM.setPeakMass(ft.getAnnotationOrThrow(PrecursorIonType.class).getIonization().addToMass(targetK.getFormula().getMass()));
                targetM.setPeakRelativeIntensity(0d);
                targetM.setPeakAbsoluteIntenstiy(0d);
            } else {
                targetM.setPeakMass(peakAno.get(targetK).getMass());
                targetM.setPeakAbsoluteIntenstiy(peakAno.get(targetK).getIntensity());
                targetM.setPeakRelativeIntensity(peakAno.get(targetK).getIntensity() / maxIntensity);

                if (annoPeakAnno!=null){
                    targetM.setPeakMass(annoPeakAnno.get(targetK).getMass());
                    targetM.setPeakRelativeIntensity(annoPeakAnno.get(targetK).getRelativeIntensity()/maxRelIntensityOfAnnoPeakAnno);
                    targetM.setPeakAbsoluteIntenstiy(annoPeakAnno.get(targetK).getSumedIntensity());
                } else {
                    targetM.setPeakMass(peakAno.get(targetK).getMass());
                    targetM.setPeakRelativeIntensity(peakAno.get(targetK).getIntensity() / maxIntensity);
                    targetM.setPeakAbsoluteIntenstiy(peakAno.get(targetK).getIntensity());
                }
            }
            calculateDeviatonMassInPpm(targetM, targetK.getIonization(), massDeviations );

            double tempScore = fscore.get(targetK) == null ? 0d : fscore.get(targetK).sum();
            tempScore += lscore.get(edgeK) == null ? edgeK.getWeight() : lscore.get(edgeK).sum();
            targetM.setScore(tempScore);

            DefaultTreeEdge edgeM = new DefaultTreeEdge();
            edgeM.setSource(sourceM);
            edgeM.setTarget(targetM);
            edgeM.setScore(lscore.get(edgeK) == null ? edgeK.getWeight() : lscore.get(edgeK).sum());
            MolecularFormula mfSource = sourceK.getFormula();
            MolecularFormula mfTarget = targetK.getFormula();
            MolecularFormula mfLoss = mfSource.subtract(mfTarget);
            edgeM.setLossFormula(mfLoss.toString());
            edgeM.setLossMass(sourceM.getPeakMass() - targetM.getPeakMass());

            sourceM.addOutEdge(edgeM);
            targetM.setInEdge(edgeM);

            convertNode(ft, targetK, targetM, peakAno, annoPeakAnno, lscore, fscore, maxIntensity, maxRelIntensityOfAnnoPeakAnno, massDeviations );
        }
    }

    private static void calculateDeviatonMassInPpm(TreeNode treeNode, Ionization ionization, ArrayList<Double> massDeviations ) {
        final double relativToPpm = 1000 * 1000;

        if (ionization != null && treeNode != null) {
            Double massDeviation = ((treeNode.getMolecularFormulaMass() - treeNode.getPeakMass() + ionization.getMass()) / treeNode.getMolecularFormulaMass()) * relativToPpm;
            treeNode.setDeviatonMass(massDeviation);
            massDeviations.add(massDeviation);
        }
    }

}
