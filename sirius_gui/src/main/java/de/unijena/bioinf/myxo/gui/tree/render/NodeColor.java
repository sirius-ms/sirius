package de.unijena.bioinf.myxo.gui.tree.render;

@SuppressWarnings("unused")
public enum NodeColor {

	rgbIntensity {public String toString(){return "RGB intensity";}},
	rbgIntensity {public String toString(){return "RBG intensity";}},
	rgIntensity {public String toString(){return "RG intensity";}},
	bgrIntensity {public String toString(){return "BGR Intensity";}},
	rwbIntensity {public String toString(){return "RWB intensity";}},
	rgbMassDeviation {public String toString(){return "RGB mass deviation";}},
	none {public String toString(){return "no color";}}

}
