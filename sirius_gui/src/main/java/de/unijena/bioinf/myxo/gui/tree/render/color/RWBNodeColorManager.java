package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.awt.*;

public abstract class RWBNodeColorManager extends AbstractNodeColorManager {

	private double p0,p1,p2;
	private double posM, negM;
	@SuppressWarnings("unused")
	private double redN, green1N, green2N, blueN;
	
	public RWBNodeColorManager(TreeNode root){
		super(root);
		
		p0 = 0;
		p1 = diff/2;
		p2 = diff;
		
		posM = 2/diff;
		negM = -posM;
		
	}
	
	private double getRedValue(double value){
		if(value<=p1){
			return 1;
		}else{
			return negM*value + 2;
		}
	}
	
	private double getGreenValue(double value){
		if(value <p1){
			return posM*value;
		}else if(value == p1){
			return 1;
		}else{
			return negM*value + 2;
		}
	}
	
	private double getBlueValue(double value){
		if(value<=p1){
			return posM*value;
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
		
		if(rTemp>1 || rTemp<0 || gTemp>1 || gTemp<0 || bTemp>1 || bTemp<0) throw new RuntimeException("v "+value+" p0 "+p0+" p1 "+p1+" p2 "+p2+" rT "+rTemp+" gT "+gTemp+" bT "+bTemp);
		
		double maxValue = 255;
		double minValue = 175;
		double maxMinDiff = maxValue-minValue;
		
		int rVal = (int) (minValue+rTemp*maxMinDiff);
		int gVal = (int) (minValue+gTemp*maxMinDiff);
		int bVal = (int) (minValue+bTemp*maxMinDiff);
		
		return new Color(rVal,gVal,bVal);
	}

}
