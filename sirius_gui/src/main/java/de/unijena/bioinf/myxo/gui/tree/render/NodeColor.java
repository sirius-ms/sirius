package de.unijena.bioinf.myxo.gui.tree.render;

@SuppressWarnings("unused")
public enum NodeColor {

	rgbScore {public String toString(){return "RGB score";}},
	rgbIntensity {public String toString(){return "RGB intensity";}},
	rbgScore {public String toString(){return "RBG score";}},
	rbgIntensity {public String toString(){return "RBG intensity";}},
	rgScore {public String toString(){return "RG score";}},
	rgIntensity {public String toString(){return "RG intensity";}},
	bgrScore {public String toString(){return "BGR Score";}},
	bgrIntensity {public String toString(){return "BGR Intensity";}},
	rwbScore {public String toString(){return "RWB score";}},
	rwbIntensity {public String toString(){return "RWB intensity";}},
	none {public String toString(){return "no color";}}

}
