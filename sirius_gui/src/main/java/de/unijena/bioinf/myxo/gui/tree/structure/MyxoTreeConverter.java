package de.unijena.bioinf.myxo.gui.tree.structure;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import org.jetbrains.annotations.NotNull;

public class MyxoTreeConverter {

    public static TreeNode convertTree(FTree ft) {
        FragmentAnnotation<Peak> peakAno = ft.getFragmentAnnotationOrThrow(Peak.class);
        LossAnnotation<Score> lscore = ft.getLossAnnotationOrNull(Score.class);
        FragmentAnnotation<Score> fscore = ft.getFragmentAnnotationOrNull(Score.class);


        double maxInt = Double.NEGATIVE_INFINITY;
        for (Fragment fragment : ft.getFragments()) {
            if (peakAno.get(fragment) == null) continue;
            double fragInt = peakAno.get(fragment).getIntensity();
            if (fragInt > maxInt) maxInt = fragInt;
        }

        TreeNode root = initConvertNode(ft, peakAno, lscore, fscore, maxInt);
        return root;
    }

    private static TreeNode initConvertNode(FTree ft, FragmentAnnotation<Peak> peakAno, LossAnnotation<Score> lscore, FragmentAnnotation<Score> fscore, double maxInt) {
        Fragment rootK = ft.getRoot();
        TreeNode rootM = new DefaultTreeNode();
        rootM.setMolecularFormula(rootK.getFormula().toString());
        rootM.setMolecularFormulaMass(rootK.getFormula().getMass());

        final @NotNull PrecursorIonType iotype = ft.getAnnotationOrThrow(PrecursorIonType.class);
        rootM.setIonization(iotype.toString());

        if (peakAno.get(rootK) == null) {
            rootM.setPeakMass(iotype.getIonization().addToMass(rootK.getFormula().getMass()));
            rootM.setPeakRelativeIntensity(0d);
            rootM.setPeakAbsoluteIntenstiy(0d);
        } else {
            rootM.setPeakMass(peakAno.get(rootK).getMass());
            rootM.setPeakAbsoluteIntenstiy(peakAno.get(rootK).getIntensity());
            rootM.setPeakRelativeIntensity(peakAno.get(rootK).getIntensity() / maxInt);
        }
        double tempScore = fscore.get(rootK) == null ? 0d : fscore.get(rootK).sum();
        rootM.setScore(tempScore);

        convertNode(ft, rootK, rootM, peakAno, lscore, fscore, maxInt);

        return rootM;
    }

    private static void convertNode(FTree ft, Fragment sourceK, TreeNode sourceM, FragmentAnnotation<Peak> peakAno, LossAnnotation<Score> lscore, FragmentAnnotation<Score> fscore, double maxInt) {
        for (Loss edgeK : sourceK.getOutgoingEdges()) {
            Fragment targetK = edgeK.getTarget();

            DefaultTreeNode targetM = new DefaultTreeNode();
            targetM.setMolecularFormula(targetK.getFormula().toString());
            targetM.setMolecularFormulaMass(targetK.getFormula().getMass());

            final @NotNull PrecursorIonType iotype = ft.getAnnotationOrThrow(PrecursorIonType.class);
            targetM.setIonization(iotype.toString());

            if (peakAno.get(targetK) == null) {
                targetM.setPeakMass(iotype.getIonization().addToMass(targetK.getFormula().getMass()));
                targetM.setPeakRelativeIntensity(0d);
                targetM.setPeakAbsoluteIntenstiy(0d);
            } else {
                targetM.setPeakMass(peakAno.get(targetK).getMass());
                targetM.setPeakAbsoluteIntenstiy(peakAno.get(targetK).getIntensity());
                targetM.setPeakRelativeIntensity(peakAno.get(targetK).getIntensity() / maxInt);
            }
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

            convertNode(ft, targetK, targetM, peakAno, lscore, fscore, maxInt);
        }
    }
}