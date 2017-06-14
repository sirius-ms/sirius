package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.awt.*;

public abstract class RGBNodeColorManager extends AbstractNodeColorManager {
	
	private double p0,p1,p2,p3,p4;
	private double posM, negM;
	private double redN, green1N, green2N, blueN;
	
	public RGBNodeColorManager(TreeNode root){
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
		green1N = 0;
		green2N = 4;
		blueN = -2;
		
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
		if(value <p1){
			return posM*value + green1N;
		}else if(value >= p1 && value <= p3){
			return 1;
		}else{
			return negM*value + green2N;
		}
	}
	
	private double getBlueValue(double value){
		if(value<=p2){
			return 0;
		}else if(value>p2 && value<p3){
			return posM*value + blueN;
		}else{
			return 1;
		}
	}

	@Override
	public Color getColor(double value) {
		value = value-minValue;
		double rTemp = getRedValue(value);
		double gTemp = getGreenValue(value);
		double bTemp = getBlueValue(value);
		
//		System.out.println(scoreVal+" "+rTemp+" "+gTemp+" "+bTemp+" "+posM);
		
		if(rTemp>1 || rTemp<0 || gTemp>1 || gTemp<0 || bTemp>1 || bTemp<0) throw new RuntimeException("v "+value+" p0 "+p0+" p1 "+p1+" p2 "+p2+" p3 "+p3+" p4 "+p4+" rT "+rTemp+" gT "+gTemp+" bT "+bTemp);
		
		double maxValue = 255;
		double minValue = 175;
		double maxMinDiff = maxValue-minValue;
		
		int rVal = (int) (minValue+rTemp*maxMinDiff);
		int gVal = (int) (minValue+gTemp*maxMinDiff);
		int bVal = (int) (minValue+bTemp*maxMinDiff);
		
		return new Color(rVal,gVal,bVal);
	}

}
