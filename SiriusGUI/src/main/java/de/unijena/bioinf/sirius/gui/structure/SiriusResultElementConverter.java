package de.unijena.bioinf.sirius.gui.structure;

import java.util.ArrayList;
import java.util.List;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.ChemistryBase.ms.ft.LossAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Score;
import de.unijena.bioinf.myxo.gui.tree.structure.DefaultTreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.DefaultTreeNode;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.sirius.IdentificationResult;

public class SiriusResultElementConverter {
	
	public static TreeNode convertTree(FTree ft){
		FragmentAnnotation<Peak> peakAno = ft.getFragmentAnnotationOrThrow(Peak.class);
		LossAnnotation<Score> lscore = ft.getLossAnnotationOrNull(Score.class);
		FragmentAnnotation<Score> fscore = ft.getFragmentAnnotationOrNull(Score.class);
		
		
		double maxInt = Double.NEGATIVE_INFINITY;
		for(Fragment fragment : ft.getFragments()){
			double fragInt = peakAno.get(fragment).getIntensity();
			if(fragInt>maxInt) maxInt = fragInt;
		}
		
		TreeNode root = initConvertNode(ft, peakAno, lscore, fscore, maxInt);
		return root;
	}
	
	public static SiriusResultElement convertResult(IdentificationResult res){
		
		SiriusResultElement out = new SiriusResultElement();
		out.setMolecularFormula(res.getMolecularFormula());
		out.setRank(res.getRank());
		out.setScore(res.getScore());
		
		FTree ft = res.getTree();
		out.setRawTree(ft);
		
//		FragmentAnnotation<Peak> peakAno = ft.getFragmentAnnotationOrThrow(Peak.class);
//		LossAnnotation<Score> lscore = ft.getLossAnnotationOrNull(Score.class);
//		FragmentAnnotation<Score> fscore = ft.getFragmentAnnotationOrNull(Score.class);
//		
//		double maxInt = Double.NEGATIVE_INFINITY;
//		for(Fragment fragment : ft.getFragments()){
//			double fragInt = peakAno.get(fragment).getIntensity();
//			if(fragInt>maxInt) maxInt = fragInt;
//		}
//		
//		TreeNode root = initConvertNode(ft, peakAno, lscore, fscore, maxInt);
//		out.setTree(root);	
		
		out.setTree(convertTree(ft));
		
		return out;
	}

	public static List<SiriusResultElement> convertResults(List<IdentificationResult> in){
		List<SiriusResultElement> outs = new ArrayList<>();
		for(IdentificationResult res : in){
			outs.add(convertResult(res));
		}
		return outs;
	}
	
//	public static List<SiriusResultElement> convertResults(List<IdentificationResult> in){
//		List<SiriusResultElement> outs = new ArrayList<>();
//		for(IdentificationResult res : in){
//			SiriusResultElement out = new SiriusResultElement();
//			out.setMolecularFormula(res.getMolecularFormula());
//			out.setRank(res.getRank());
//			out.setScore(res.getScore());
//			
//			FTree ft = res.getTree();
//			out.setRawTree(ft);
//			
//			FragmentAnnotation<Peak> peakAno = ft.getFragmentAnnotationOrThrow(Peak.class);
//			LossAnnotation<Score> lscore = ft.getLossAnnotationOrNull(Score.class);
//			FragmentAnnotation<Score> fscore = ft.getFragmentAnnotationOrNull(Score.class);
//			
//			double maxInt = Double.NEGATIVE_INFINITY;
//			for(Fragment fragment : ft.getFragments()){
//				double fragInt = peakAno.get(fragment).getIntensity();
//				if(fragInt>maxInt) maxInt = fragInt;
//			}
//			
//			TreeNode root = initConvertNode(ft, peakAno, lscore, fscore, maxInt);
//			out.setTree(root);	
//			outs.add(out);
//		}
//		return outs;
//	}
	
	private static TreeNode initConvertNode(FTree ft,FragmentAnnotation<Peak> peakAno, LossAnnotation<Score> lscore, FragmentAnnotation<Score> fscore, double maxInt){
		Fragment rootK = ft.getRoot();
		TreeNode rootM = new DefaultTreeNode();
		rootM.setMolecularFormula(rootK.getFormula().toString());
		rootM.setMolecularFormulaMass(rootK.getFormula().getMass());
		rootM.setPeakMass(peakAno.get(rootK).getMass());
		rootM.setPeakAbsoluteIntenstiy(peakAno.get(rootK).getIntensity());
		rootM.setPeakRelativeIntensity(peakAno.get(rootK).getIntensity()/maxInt);
		double tempScore = fscore.get(rootK).sum();
		rootM.setScore(tempScore);
		
		convertNode(ft, rootK, rootM, peakAno, lscore, fscore, maxInt);
		
		return rootM;
	}
	
	private static void convertNode(FTree ft, Fragment sourceK, TreeNode sourceM, FragmentAnnotation<Peak> peakAno, LossAnnotation<Score> lscore, FragmentAnnotation<Score> fscore, double maxInt){
		for(Loss edgeK : sourceK.getOutgoingEdges()){
			Fragment targetK = edgeK.getTarget();
			
			DefaultTreeNode targetM = new DefaultTreeNode();
			targetM.setMolecularFormula(targetK.getFormula().toString());
			targetM.setMolecularFormulaMass(targetK.getFormula().getMass());
			targetM.setPeakMass(peakAno.get(targetK).getMass());
			targetM.setPeakAbsoluteIntenstiy(peakAno.get(targetK).getIntensity());
			targetM.setPeakRelativeIntensity(peakAno.get(targetK).getIntensity()/maxInt);
			double tempScore = fscore.get(targetK).sum();
			tempScore += lscore.get(edgeK).sum();
			targetM.setScore(tempScore);
			
			DefaultTreeEdge edgeM = new DefaultTreeEdge();
			edgeM.setSource(sourceM);
			edgeM.setTarget(targetM);
			edgeM.setScore(lscore.get(edgeK).sum()); //TODO korrekt???
			MolecularFormula mfSource = sourceK.getFormula();
			MolecularFormula mfTarget = targetK.getFormula();
			MolecularFormula mfLoss = mfSource.subtract(mfTarget);
			edgeM.setLossFormula(mfLoss.toString());
			edgeM.setLossMass(sourceM.getPeakMass()-targetM.getPeakMass());
			
			sourceM.addOutEdge(edgeM);
			targetM.setInEdge(edgeM);
			
			convertNode(ft,targetK,targetM,peakAno,lscore,fscore,maxInt);
			
		}
	}

}
