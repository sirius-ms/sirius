package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.awt.*;

public abstract class RGNodeColorManager extends AbstractNodeColorManager {
	
	@SuppressWarnings("unused")
	private double p0,p1,p2;
	private double posM, negM;
	private double redN, greenN;
	
	public RGNodeColorManager(TreeNode root){
		super(root);
		
		p0 = 0;
		p1 = diff/2;
		p2 = diff;
		
		posM = 2/diff;
		negM = -posM;
		
		redN = 2;
		greenN = 0;
		
//		System.out.println("posM: "+posM+" negM: "+negM);
//		System.out.println("redN: "+redN+" greenN: "+greenN);
//		System.out.println("p0: "+p0+" p1: "+p1+" p2: "+p2);
		
	}
	
	private double getRedValue(double value){
		if(value<=p1){
			return 1;
		}else{
			return negM*value + redN;
		}
	}
	
	private double getGreenValue(double value){
		if(value <=p1){
			return posM*value + greenN;
		}else{
			return 1;
		}
	}

	@Override
	public Color getColor(double value) {
		value = value-minValue;
		double rTemp = getRedValue(value);
		double gTemp = getGreenValue(value);
		
//		System.out.println(scoreVal+" "+rTemp+" "+gTemp+" "+bTemp+" "+posM);
		
		if(rTemp>1 || rTemp<0 || gTemp>1 || gTemp<0) throw new RuntimeException(rTemp+" "+gTemp);
		
		double maxValue = 255;
		double minValue = 175;
		double maxMinDiff = maxValue-minValue;
		
		int rVal = (int) (minValue+rTemp*maxMinDiff);
		int gVal = (int) (minValue+gTemp*maxMinDiff);
		int bVal = (int) (minValue);
		
		return new Color(rVal,gVal,bVal);
	}


}
