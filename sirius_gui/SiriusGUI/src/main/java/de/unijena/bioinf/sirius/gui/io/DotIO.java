package de.unijena.bioinf.sirius.gui.io;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.myxo.tools.MolecularFormulaTools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DotIO {

	public DotIO() {
		// TODO Auto-generated constructor stub
	}
	
	public static void writeTree(File path,TreeNode root,double treeScore){
		
		List<TreeNode> nodes = new ArrayList<TreeNode>();
		List<TreeEdge> edges = new ArrayList<TreeEdge>();
		
		ArrayDeque<TreeNode> deque = new ArrayDeque<>();
		deque.add(root);
		while(!deque.isEmpty()){
			TreeNode node = deque.remove();
			nodes.add(node);
			for(TreeEdge edge : node.getOutEdges()){
				deque.add(edge.getTarget());
				edges.add(edge);
			}
		}
		
		DecimalFormat massFormat = new DecimalFormat("#0.####");
		DecimalFormat intFormat = new DecimalFormat("#.####%");
		DecimalFormat scoreFormat = new DecimalFormat("#0.##");
		
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(path))){
			writer.write("strict digraph {\n");
			writer.write("\tnode [shape=rect,style=rounded];\n");
			writer.write("\tlabelloc=\"t\";\n");
			writer.write("\tlabel=\"Compound Score: "+treeScore+"\";\n");
			for(TreeNode node : nodes){
				writer.write("\t"+node.getMolecularFormula()+" [label=<"+getMolecularFormulaString(node.getMolecularFormula())+
						"<FONT POINT-SIZE=\"8\"><BR /> <BR />"+massFormat.format(node.getPeakMass())+" Da, "+
						intFormat.format(node.getPeakRelativeIntensity())+"<BR />"+scoreFormat.format(node.getScore())+"</FONT>>];\n");
			}
			writer.write("\n");
			for(TreeEdge edge : edges){
				writer.write("\t"+edge.getSource().getMolecularFormula()+" -> "+edge.getTarget().getMolecularFormula()+" [label=<"+getMolecularFormulaString(edge.getLossFormula())+">];\n");
			}
			writer.write("}\n");
			
		}catch(IOException e){
			throw new RuntimeException(e);
		}
		
	}
	
	
	private static String getMolecularFormulaString(String mf){
		Map<String, Integer> eles = MolecularFormulaTools.getElements(mf);
		StringBuilder sb = new StringBuilder();
		for(String ele : eles.keySet()){
			int amount = eles.get(ele);
			sb.append(amount==1? ele : ele+"<SUB>"+amount+"</SUB>");
		}
		return sb.toString();
	}

}
