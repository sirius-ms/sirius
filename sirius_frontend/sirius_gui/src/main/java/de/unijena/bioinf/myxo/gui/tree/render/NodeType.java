package de.unijena.bioinf.myxo.gui.tree.render;

@SuppressWarnings("unused")
public enum NodeType{
	preview {public String toString(){return "preview";}},
	thumbnail{public String toString(){return "thumbnail";}},
	small {public String toString(){return "small";}}, 
	big{public String toString(){return "big";}}, 
	score{public String toString(){return "scores";}};
}
