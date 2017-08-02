package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.awt.*;

public abstract class RBGNodeColorManager extends AbstractNodeColorManager {
	
	@SuppressWarnings("unused")
	private double p0,p1,p2,p3,p4;
	private double posM, negM;
	private double redN, blue1N, blue2N, greenN;
	
	public RBGNodeColorManager(TreeNode root){
		super(root);
		
		p0 = 0;
		p1 = diff/4;
		p2 = diff/2;
		p3 = 3*diff/4;
		p4 = diff;
		
		posM = 4/diff;
		negM = -posM;
		
//		System.out.println("minScore: "+minScore+" maxScore: "+maxScore);
//		System.out.println("posM: "+posM+" negM: "+negM);
//		if(true)throw new RuntimeException();
		
		redN = 2;
		blue1N = 0;
		blue2N = 4;
		greenN = -2;
		
	}
	
	private double getRedValue(double value){
		if(value<=p1){
			return 1;
		}else if(value>p1 && value<p2){
			return negM*value + redN;
		}else{
			return 0;
		}
	}
	
	private double getGreenValue(double value){
		if(value<=p2){
			return 0;
		}else if(value>p2 && value<p3){
			return posM*value + greenN;
		}else{
			return 1;
		}
	}
	
	private double getBlueValue(double value){
		if(value <p1){
//			System.out.println("Fall1");
			return posM*value + blue1N;
		}else if(value >= p1 && value <= p3){
//			System.out.println("Fall2");
			return 1;
		}else{
//			System.out.println("Fall2 "+negM+" "+score+" "+blue2N);
			return negM*value + blue2N;
		}
	}

	@Override
	public Color getColor(double value) {
		
		value = value - minValue;
		
		double rTemp = getRedValue(value);
		double gTemp = getGreenValue(value);
		double bTemp = getBlueValue(value);
		
		if(rTemp>1 || rTemp<0 || gTemp>1 || gTemp<0 || bTemp>1 || bTemp<0) throw new RuntimeException(rTemp+" "+gTemp+" "+bTemp);
		
		double maxValue = 255;
		double minValue = 175;
		double maxMinDiff = maxValue-minValue;
		
		int rVal = (int) (minValue+rTemp*maxMinDiff);
		int gVal = (int) (minValue+gTemp*maxMinDiff);
		int bVal = (int) (minValue+bTemp*maxMinDiff);
		
		return new Color(rVal,gVal,bVal);
	}

}
