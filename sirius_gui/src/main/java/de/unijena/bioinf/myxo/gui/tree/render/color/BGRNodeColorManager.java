package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.awt.*;

public abstract class BGRNodeColorManager extends AbstractNodeColorManager {
	
	@SuppressWarnings("unused")
	private double p0;
	@SuppressWarnings("unused")
	private double p1;
	@SuppressWarnings("unused")
	private double p2;
	@SuppressWarnings("unused")
	private double p3;
	private double posM, negM;
	private double redN, green1N, green2N, blueN;
	
	public BGRNodeColorManager(TreeNode root){
		super(root);
		
		p0 = 0;
		p1 = diff/4;
		p2 = diff/2;
		p3 = 3*diff/4;

		posM = 4/diff;
		negM = -posM;
		
		redN = -2;
		green1N = 0;
		green2N = 4;
		blueN = 2;
		
	}
	
	private double getRedValue(double value){
		if(value<=p2){
			return 0;
		}else if(value>p2 && value<p3){
			return posM*value + redN;
		}else{
			return 1;
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
		if(value<=p1){
			return 1;
		}else if(value>p1 && value<p2){
			return negM*value + blueN;
		}else{
			return 0;
		}
	}

	@Override
	public Color getColor(double value) {
		value = value-minValue;
		double rTemp = getRedValue(value);
		double gTemp = getGreenValue(value);
		double bTemp = getBlueValue(value);
		
//		System.out.println(scoreVal+" "+rTemp+" "+gTemp+" "+bTemp+" "+posM);
		
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
