
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package fragtreealigner.domainobjects.graphs;

import de.unijena.bioinf.ChemistryBase.math.MathUtils;
import fragtreealigner.algorithm.ScoringFunctionNeutralLosses;
import fragtreealigner.domainobjects.chem.basics.MolecularFormula;
import fragtreealigner.domainobjects.chem.components.Compound;
import fragtreealigner.domainobjects.chem.components.NeutralLoss;
import fragtreealigner.domainobjects.chem.structure.MolecularStructure;
import fragtreealigner.domainobjects.db.DatabaseStatistics;
import fragtreealigner.domainobjects.db.FragmentationTreeDatabase.DecoyType;
import fragtreealigner.util.Session;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.ValidityException;
import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.base.CMLElement;
import org.xmlcml.cml.element.*;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import nu.xom.Serializer;
//import org.xmlcml.cml.element.CMLFormula;
//import org.xmlcml.cml.element.CMLName;

@SuppressWarnings("serial")
public class FragmentationTree extends Tree<FragmentationTreeNode, FragmentationTreeEdge> {
    public static final int MAX_INTENSITY = 100;
    private boolean warnDoubleNodes = false;
    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public FragmentationTree(Session session) {
        super(session);
    }

    public FragmentationTree(Session session, String filename) {
        this(session);
        this.fileName = filename;
    }

    public boolean isWarnDoubleNodes() {
        return warnDoubleNodes;
    }

    public void setWarnDoubleNodes(boolean warnDoubleNodes) {
        this.warnDoubleNodes = warnDoubleNodes;
    }

    @Override
    public FragmentationTreeNode addNode(String label) {
        FragmentationTreeNode node = new FragmentationTreeNode(label);
        super.addNode(node, label);
        return node;
    }

    public FragmentationTreeNode addNode(String label, Compound compound, float doubleBondEquivalent, double deviation, String collisionEnergy) {
        FragmentationTreeNode node = new FragmentationTreeNode(label, compound, doubleBondEquivalent, deviation, collisionEnergy);
        super.addNode(node, label);
        return node;
    }

    private FragmentationTreeNode addNode(String label, Compound compound, double intensity, float doubleBondEquivalent, double deviation, String collisionEnergy) {
        FragmentationTreeNode node = new FragmentationTreeNode(label, compound, doubleBondEquivalent, deviation, collisionEnergy);
        node.setIntensity(intensity);
        super.addNode(node, label);
        return node;
    }

    @Override
    public FragmentationTreeEdge connect(FragmentationTreeNode parent, FragmentationTreeNode child) {
        FragmentationTreeEdge edge = new FragmentationTreeEdge(parent, child);
        super.connect(edge);
        return edge;
    }

    public FragmentationTreeEdge connect(FragmentationTreeNode parent, FragmentationTreeNode child, String label, NeutralLoss neutralLoss, float doubleBondEquivalent, double score) {
        FragmentationTreeEdge edge = new FragmentationTreeEdge(parent, child, label, neutralLoss, doubleBondEquivalent, score);
        super.connect(edge);
        return edge;
    }

    public AlignmentTree toAlignmentTree() {
        if (session.getParameters().makeVerboseOutput) System.err.print("Converting to alignment tree");

        this.normaliseIntensities();
        if (session.getParameters().scoreWeightingType == ScoringFunctionNeutralLosses.ScoreWeightingType.NODE_WEIGHT){
            for (FragmentationTreeNode v : getNodes()){
//                if (Double.isNaN(v.getIntensity())){
//                    System.err.println("Vertex "+v.getLabel()+" has no intensity.");
//                    throw new RuntimeException();
//                }
            }
        }

        AlignmentTree alignmentTree = new AlignmentTree(this, session);
        alignmentTree.setId(id);
        alignmentTree.setType(type);
        /*if (session.getParameters().useNodeLabels){
              return toAlignmentTreeWithNodeLabels(alignmentTree);
          }*/
        AlignmentTreeNode aligRoot = alignmentTree.addNode("root");
        aligRoot.setCompound(root.getCompound());
        buildAlignmentTree(alignmentTree, root, aligRoot, this.getMaxEdgeScore());
        alignmentTree.determineRoot();

        if (session.getParameters().makeVerboseOutput) System.err.println("\t --> Finished (" + this.getId() + ")");
        return alignmentTree;
    }

    private AlignmentTree toAlignmentTreeWithNodeLabels(
            AlignmentTree alignmentTree) {
        Map<FragmentationTreeNode, AlignmentTreeNode> map = new HashMap<FragmentationTreeNode, AlignmentTreeNode>();
        double sd = session.getParameters().ppm_error;
        for (FragmentationTreeNode v : this.getNodes()){
            double score = (MathUtils.erfc(Math.abs(v.getDeviation())/sd)*2-1)*session.getParameters().manipStrength;
            NeutralLoss pseudoLoss = new NeutralLoss(v.getCompound().getName(), v.getCompound().getMass(), v.getCompound().getMolecularFormula(), session);
            AlignmentTreeNode alNode = alignmentTree.addNode(Integer.toString(alignmentTree.size()), pseudoLoss, score);
            map.put(v, alNode);
        }
        for (FragmentationTreeEdge e : this.getEdges()){
            alignmentTree.connect(map.get(e.getFromNode()), map.get(e.getToNode()), e.getLabel());
        }
        alignmentTree.determineRoot();
        return alignmentTree;
    }

    public void buildAlignmentTree(AlignmentTree alignmentTree, FragmentationTreeNode fragParent, AlignmentTreeNode aligParent, double scoreScalingValue) {
        AlignmentTreeNode child;
        double score;
        if (fragParent == null){
            System.err.println(getId()+" fragparent null");
        }
        for ( FragmentationTreeEdge fragTreeEdge : fragParent.getOutEdges()) {
            // weight with the original tree score of the edge
            //score = fragTreeEdge.getScore();
            //if (scoreScalingValue != 0) score /= scoreScalingValue;

            // add scores using manipulators
            // scale manipulators to have range -1 to +1
            //score = ((Math.min(fragParent.getIntensity(), fragTreeEdge.toNode.getIntensity())*2/MAX_INTENSITY)-1)*session.getParameters().manipStrength;
            score = calculateDeviationManipulator(fragTreeEdge)*session.getParameters().manipStrength;
            child = alignmentTree.addNode(Integer.toString(alignmentTree.size()), fragTreeEdge.getNeutralLoss(), score);
            child.setCompound(fragTreeEdge.getToNode().getCompound());
            alignmentTree.connect(aligParent, child);
            buildAlignmentTree(alignmentTree, fragTreeEdge.getToNode(), child, scoreScalingValue);
        }
    }

    private double calculateDeviationManipulator(FragmentationTreeEdge fragTreeEdge) {
        double sd = session.getParameters().ppm_error;
        double scoreParent = MathUtils.erfc(Math.abs(fragTreeEdge.getFromNode().getDeviation())/sd);
        double scoreChild = MathUtils.erfc(Math.abs(fragTreeEdge.getToNode().getDeviation())/sd);
        if (session.getParameters().oneNodePenalty){
            return (scoreChild*2)-1;
        }
        //return Math.min(scoreParent, scoreChild)*2-1;
        return scoreParent + scoreChild-1; // average between parent and child, scaled to -1;+1
    }

    private void normaliseIntensities(){
        double maxInt = Double.NEGATIVE_INFINITY;
        for (FragmentationTreeNode v : getNodes()){
            maxInt = Math.max(maxInt, v.getIntensity());
        }
        for (FragmentationTreeNode v : getNodes()){
            v.setIntensity(v.getIntensity()/maxInt*MAX_INTENSITY);
        }

    }

    public FragmentationTree buildDecoyTree() {
        DecoyType decoyType = session.getParameters().decoyType;
        switch (decoyType) {
            case RANDOM:
                return (FragmentationTree) this.rearrangeEdges(RearrangementType.RANDOM);

            case REVERSE:
                return (FragmentationTree) this.rearrangeEdges(RearrangementType.REVERSE);

            case DB:
                DatabaseStatistics dbStatistics = session.getFragTreeDB().getDatabaseStatistics();
                FragmentationTree decoyFragTree = this.clone();
                List<FragmentationTreeEdge> edges = decoyFragTree.getEdges();
                for (FragmentationTreeEdge edge : edges) {
                    edge.setNeutralLoss(dbStatistics.getRandomNeutralLoss());
//					TODO: other edge attributes like dbe and score have to be adapted, too!!!
                }
                return decoyFragTree;

            case ALT_DB:
				FragmentationTree decoyFragTree2 = session.getAltFragTreeDB().getRandomTreeTopology(this.getNodes().size());				
				// workaround if DB does not contain a larger tree, should not happen!
				if (decoyFragTree2 == null){
					System.err.println("at tree "+id);
					decoyFragTree2 = this.clone();
				}
				decoyFragTree2.root.setContent(this.root);
				boolean treeIsCalculated=false;
				int attempt=1;
				List<FragmentationTreeEdge> orderedEdges=orderEdges(decoyFragTree2.getEdges(),decoyFragTree2.root);
				while(!treeIsCalculated){
					treeIsCalculated=true;
					for (FragmentationTreeEdge edge : orderedEdges){						
						boolean edgeIsSet=false;
						List<NeutralLoss> randomNeutralLossList=session.getFragTreeDB().getDatabaseStatistics().getRandomNeutralLossList();
						Iterator<NeutralLoss> i=randomNeutralLossList.iterator();						
						while(!edgeIsSet&&i.hasNext()){
							NeutralLoss nl=i.next();
							String diff=edge.fromNode.getCompound().getMolecularFormula().diff(nl.getMolecularFormula()).toString();
							if (!diff.contains("-")){					
								edge.setNeutralLoss(nl);
								edge.toNode.setLabel(diff);
								Compound c=new Compound(diff, 0.0, diff, session);
								edge.toNode.setCompound(c);
								edgeIsSet=true;
							}						
						}
						if(!edgeIsSet){
							System.err.println(attempt++ + ". attempt for generating decoy tree for "+this.getFileName()+" failed.");							
							treeIsCalculated=false;
							break;
						}
					}
//					TODO: other edge attributes like dbe and score have to be adapted, too!!!
				}
				return decoyFragTree2;
            default:
                System.err.println("Unknown decoy type.");
                return this;
        }
    }
    
    private List<FragmentationTreeEdge> orderEdges(List<FragmentationTreeEdge> fragmentationTreeEdgeList, FragmentationTreeNode root){
		List<FragmentationTreeEdge> result=new ArrayList<FragmentationTreeEdge>();
		List<FragmentationTreeNode> nodes=new ArrayList<FragmentationTreeNode>();
		nodes.add(root);
		List<FragmentationTreeEdge> tmp=new ArrayList<FragmentationTreeEdge>();
		tmp.addAll(fragmentationTreeEdgeList);		
		while(tmp.size()>0){
			for(Iterator<FragmentationTreeEdge> i = tmp.iterator(); i.hasNext();){
				FragmentationTreeEdge curr=i.next();
				if(nodes.contains(curr.fromNode)){
					result.add(curr);
					nodes.add(curr.toNode);
					i.remove();
				}
			}
		}		
		return result;	
	}

    public static FragmentationTree readFromDot(BufferedReader reader, String fileName, Session session) throws IOException {
        FragmentationTree result= readFromDot(reader, session);
        if (result != null) result.setFileName(fileName);
        return result;
    }

    public static FragmentationTree readFromDot(BufferedReader reader, Session session) throws IOException {
        if (session.getParameters().makeVerboseOutput) System.err.print("Reading fragmentation tree");

        FragmentationTree fragTree = new FragmentationTree(session);

        Pattern pNode = Pattern.compile( "^([0-9A-Za-z]*).*Mass: (.*)..DBE: (.*)..Dev: (.*)..CE:(-?[0-9]* -?[0-9]*)" );
        Pattern pNode2 = Pattern.compile( "^([0-9A-Za-z]*).*Mass: (.*)..Int: (.*)..ppm: (.*)..CE:(-?[0-9]* -?[0-9]*)" );
        Pattern pEdge = Pattern.compile( "^([0-9A-Za-z]*) -> ([0-9A-Za-z]*).*label=\"(.*)..Mass: (.*)..DBE: (.*)..Score: (.*)\"" );
        Pattern pMaxScore = Pattern.compile( "^// Max Edge Score: (.*)" );
        Matcher mNode, mNode2;
        Matcher mEdge;
        Matcher mMaxScore;
        boolean nodeMatchFound, nodeMatchFound2, edgeMatchFound;
        String nodeLabel, molFormula, nodeCE, edgeFrom, edgeTo, edgeLabel;
        int indexTms;
        double nodeMass, nodeDev, nodeIntensity, edgeMass, edgeScore;
        float nodeDBE, edgeDBE;
        Compound compound;
        NeutralLoss neutralLoss;
        FragmentationTreeNode edgeFromNode, edgeToNode;

        for (int i = 0; i < 3; i++) reader.readLine();
        while (reader.ready()) {
            String line = reader.readLine();
            mNode = pNode.matcher( line );
            mNode2 = pNode2.matcher( line );
            mEdge = pEdge.matcher( line );
            mMaxScore = pMaxScore.matcher( line );
            nodeMatchFound = mNode.find();
            nodeMatchFound2 = mNode2.find();
            edgeMatchFound = mEdge.find();
            if (nodeMatchFound || nodeMatchFound2) {
                if (nodeMatchFound2){
                    nodeLabel = mNode2.group(1);
                    nodeMass = Double.parseDouble(mNode2.group(2));
                    nodeIntensity = Double.parseDouble(mNode2.group(3));
                    // set DBE to unknown
                    nodeDBE = Float.NaN;
                    nodeDev  = Double.parseDouble(mNode2.group(4));
                    nodeCE = mNode2.group(5);
                } else {
                    nodeLabel= mNode.group(1);
                    nodeMass = Double.parseDouble(mNode.group(2));
                    // set intensity to unknown
                    nodeIntensity = Double.NaN;
                    nodeDBE  = Float.parseFloat(mNode.group(3));
                    nodeDev  = Double.parseDouble(mNode.group(4));
                    nodeCE   = mNode.group(5);
                }
                if (fragTree.getNodeByName(nodeLabel) != null) {
                    if (fragTree.isWarnDoubleNodes()) System.out.println("Node with label " + nodeLabel + " already exists in the tree.");
                }
                else {
                    indexTms = nodeLabel.indexOf('T');
                    molFormula = (indexTms > 0) ? nodeLabel.substring(0, nodeLabel.indexOf('T')) : nodeLabel;
                    compound = new Compound(molFormula, nodeMass, nodeLabel, session);
                    fragTree.addNode(nodeLabel, compound, nodeIntensity, nodeDBE, nodeDev, nodeCE);
                }
            } else if ( edgeMatchFound ) {
            	edgeFrom = mEdge.group(1);
                edgeTo   = mEdge.group(2);
                edgeLabel= mEdge.group(3);
                edgeMass = Double.parseDouble(mEdge.group(4));
                edgeDBE  = Float.parseFloat(mEdge.group(5));
                edgeScore= Double.parseDouble(mEdge.group(6));

                edgeFromNode = (FragmentationTreeNode)fragTree.getNodeByName(edgeFrom);
                edgeToNode   = (FragmentationTreeNode)fragTree.getNodeByName(edgeTo);

                neutralLoss = new NeutralLoss(edgeLabel, edgeMass, edgeLabel, session);
                fragTree.connect(edgeFromNode, edgeToNode, edgeLabel, neutralLoss, edgeDBE, edgeScore);
            } else if ( mMaxScore.find()) {
                fragTree.setMaxEdgeScore(Double.parseDouble(mMaxScore.group(1)));
            }
        }
        if (fragTree.getNodes().isEmpty()){
            return null;
        }
        fragTree.determineRoot();
        if (session.getParameters().makeVerboseOutput) {
        	System.err.println(fragTree.getEdges().size()+" "+fragTree.getNodes().size());
        }
        if (session.getParameters().makeVerboseOutput) System.err.println("\t --> Finished");
        return fragTree;
    }

    public static FragmentationTree readFromCml(BufferedReader reader, String fileName, Session session) throws IOException {
        FragmentationTree result= readFromCml(reader, session);
        if (result != null) result.setFileName(fileName);
        return result;
    }

    public static FragmentationTree readFromCml(BufferedReader reader, Session session) throws IOException {
        if (session.getParameters().makeVerboseOutput) System.err.println("Reading fragmentation tree");

        FragmentationTree fragTree = new FragmentationTree(session);

        Document doc = null;
        try {
            doc = new CMLBuilder().build(reader);
        } catch (ValidityException e) {
            System.out.println("File is not a valid CML file: " + e);
            e.printStackTrace();
        } catch (ParsingException e) {
            System.out.println("File is not a valid CML file: " + e);
            e.printStackTrace();
        }
        CMLElement rootElement = (CMLElement)doc.getRootElement();
//		rootElement.debug();

        String moleculeId, moleculeRef1 = "", moleculeRef2 = "", moleculeRef3 = "", formulaStr;
        Compound compound;
        NeutralLoss neutralLoss;
        FragmentationTreeNode edgeFromNode, edgeToNode;

        HashMap<String, NeutralLoss> neutralLossHash = new HashMap<String, NeutralLoss>();
        MolecularFormula molecularFormula;
        CMLMolecule molecule;
        CMLReaction reaction;
        for (CMLElement cmlElement : rootElement.getChildCMLElements()) {
            if (cmlElement.getLocalName().equals("moleculeList")) {
                for (CMLElement cmlElement2 : cmlElement.getChildCMLElements()) {
                    molecule = (CMLMolecule)cmlElement2;
                    moleculeId = molecule.getId();
                    formulaStr = molecule.getFormula();
                    if (formulaStr == null) formulaStr = "";
                    molecularFormula = new MolecularFormula(formulaStr, session);
                    if (((CMLMoleculeList)cmlElement).getTitle().equals("Ion list")) {
                        if (fragTree.getNodeByName(moleculeId) != null) {
                            if (fragTree.isWarnDoubleNodes()) System.out.println("Node with label " + moleculeId + " already exists in the tree.");
                        }
                        else {
                            compound = new Compound(moleculeId, Double.NaN, molecularFormula, session);
                            compound.setMolecularStructure(MolecularStructure.convertFromCml(molecule, session));
                            fragTree.addNode(moleculeId, compound, Float.NaN, Double.NaN, "");
                        }
                    } else {
                        neutralLoss = new NeutralLoss(moleculeId, Double.NaN, molecularFormula, session);
                        neutralLoss.setMolecularStructure(MolecularStructure.convertFromCml(molecule, session));
                        neutralLossHash.put(moleculeId, neutralLoss);
                    }
                }
            } else {
                for (CMLElement cmlElement2 : cmlElement.getChildCMLElements()) {
                    reaction = (CMLReaction)cmlElement2;
                    for (CMLReactant reactant : reaction.getReactantList().getReactantElements()) {
                        moleculeRef1 = reactant.getMolecule().getRef();
                    }
                    for (CMLProduct product : reaction.getProductList().getProductElements()) {
                        if (product.getRole().equals("ion")) moleculeRef2 = product.getMolecule().getRef();
                        if (product.getRole().equals("neutral_loss")) moleculeRef3 = product.getMolecule().getRef();
                    }
                    edgeFromNode = (FragmentationTreeNode)fragTree.getNodeByName(moleculeRef1);
                    edgeToNode   = (FragmentationTreeNode)fragTree.getNodeByName(moleculeRef2);
                    neutralLoss = neutralLossHash.get(moleculeRef3);
                    fragTree.connect(edgeFromNode, edgeToNode, moleculeRef3, neutralLoss, Float.NaN, Double.NaN);
                }
            }
        }
        fragTree.determineRoot();
        if (session.getParameters().makeVerboseOutput) System.err.println("\t --> Finished");
        return fragTree;
    }

    public static FragmentationTree generateRandomTree(int numNodes, int outDegree, Session session) throws IOException {
        FragmentationTree fragTree = new FragmentationTree(session);
        FragmentationTreeNode parentNode, currentNode;
        NeutralLoss neutralLoss;
        String edgeLabel;
        int childNumber = 0;

        LinkedList<FragmentationTreeNode> parentList = new LinkedList<FragmentationTreeNode>();
        parentNode = fragTree.addNode(Integer.toString(0));
        for (int i = 1; i < numNodes; i++) {
            //compound = new Compound(molFormula, nodeMass, nodeLabel, session);
            //fragTree.addNode(nodeLabel, compound, nodeDBE, nodeDev, "0 0");
            currentNode = fragTree.addNode(Integer.toString(i));
            parentList.add(currentNode);
            childNumber++;

            edgeLabel = "NL_" + Integer.toString(i);
            neutralLoss = new NeutralLoss(edgeLabel, 0, "CO", session);
            fragTree.connect(parentNode, currentNode, edgeLabel, neutralLoss, 0.0f, 1.0);

            if (childNumber == outDegree) {
                parentNode = parentList.removeFirst();
                childNumber = 0;
            }
        }

        fragTree.determineRoot();
        return fragTree;
    }

    @Override
    public void writeToDot(BufferedWriter writer) throws IOException {
        ListIterator<FragmentationTreeNode> nodeIt;
        ListIterator<FragmentationTreeEdge> edgeIt;
        FragmentationTreeNode node;
        FragmentationTreeEdge edge;
        String label;
        int i = 0;
        writer.write(isDirected() ? "digraph G {\n" : "graph G {\n");
        writer.newLine();
        writer.newLine();
        for (nodeIt = nodes.listIterator(); nodeIt.hasNext();) {
            node = nodeIt.next();
           // writer.write(node.getLabel() +"[label=\""+node.getLabel()+"\\nMass: "+node.getCompound().getMass()+"\\nInt: "+node.getIntensity()+"\\nppm: "+node.getDeviation()+"\\nCE: "+node.getCollisionEnergy()+"\"]\n");
            writer.write(node.getLabel() +"[label=\""+node.getLabel()+"\\nMass: 0.0\\nInt: 0.0\\nppm: 0.0\\nCE: 1 1\"]\n");

            i++;
        }
        //	writer.write("\t" + getRoot().getLabel() + " [label=\"" + getRoot() + "\", style=dashed];\n");

        i = 0;
        for (edgeIt = edges.listIterator(); edgeIt.hasNext();) {
            edge = edgeIt.next();
            //label = " [label=\"" + ((edge.getLabel() == null) ? "" : edge.getLabel()) + "\"" + edge.dotParams() + "]";
//			label = (edge.getLabel() == null) ? "" : " [label=\"" + edge.getLabel() + "\"]";
            //writer.write("\t" + edge.getFromNode().getLabel() + (isDirected() ? "->" : " -- ") + edge.getToNode().getLabel() + label + ";\n");

           // writer.write(edge.getFromNode().getLabel()+ (isDirected() ? "->" : " -- ") + edge.getToNode().getLabel()+"[label=\"" + edge.getLabel() + "\\nMass: " + edge.getNeutralLoss().getMass() + "\\nDBE: " + edge.getDoubleBondEquivalent() + "\\nScore: " + edge.getScore() + "\"]\n");

            writer.write(edge.getFromNode().getLabel()+ (isDirected() ? " -> " : " -- ") + edge.getToNode().getLabel()+" [label=\"" + edge.getNeutralLoss().getMolecularFormula().toString() + "\\nMass: 0.0\\nDBE: 0.0\\nScore: 0.0\"]\n");

            i++;
        }
        writer.write("}");
        writer.close();
    }


    public void writeToCml(String filename) throws IOException {
        ListIterator<FragmentationTreeNode> nodeIt;
        ListIterator<FragmentationTreeEdge> edgeIt;
        FragmentationTreeNode node;
        FragmentationTreeEdge edge;

        CMLElement rootElement = new CMLElement("cml");
        CMLMoleculeList moleculeList = new CMLMoleculeList();
        CMLMolecule molecule;
        rootElement.appendChild(moleculeList);
        moleculeList.setTitle("Ion list");

        node = this.getRoot();
        node.getCompound().getName();
        molecule = new CMLMolecule();
        molecule.setId(node.getCmlId());
        moleculeList.appendChild(molecule);
        molecule.setFormula(node.getCompound().getMolecularFormula().toCmlString());

        for (nodeIt = nodes.listIterator(); nodeIt.hasNext();) {
            node = nodeIt.next();
            if (node == this.getRoot()) continue;
            node.getCompound().getName();
            molecule = new CMLMolecule();
            molecule.setId(node.getCmlId());
            moleculeList.appendChild(molecule);
            molecule.setFormula(node.getCompound().getMolecularFormula().toCmlString());
        }

        moleculeList = new CMLMoleculeList();
        rootElement.appendChild(moleculeList);
        moleculeList.setTitle("Neutral loss list");
        for (edgeIt = edges.listIterator(); edgeIt.hasNext();) {
            edge = edgeIt.next();

            molecule = new CMLMolecule();
            molecule.setId(edge.getCmlId());
            moleculeList.appendChild(molecule);
            molecule.setFormula(edge.getNeutralLoss().getMolecularFormula().toCmlString());
        }


        CMLReactionList reactionList = new CMLReactionList();
        CMLReaction reaction;
        CMLReactantList reactantList;
        CMLReactant reactant;
        CMLProductList productList;
        CMLProduct product;
        for (edgeIt = edges.listIterator(); edgeIt.hasNext();) {
            edge = edgeIt.next();
            reactantList = new CMLReactantList();
            reactant = new CMLReactant();
            productList = new CMLProductList();

            molecule = new CMLMolecule();
            molecule.setRef(edge.getFromNode().getCmlId());
            reaction = new CMLReaction();
            reactant.addMolecule(molecule);
            reactantList.addReactant(reactant);

            molecule = new CMLMolecule();
            molecule.setRef(edge.getToNode().getCmlId());
            product = new CMLProduct();
            product.addMolecule(molecule);
            product.setRole("ion");
            productList.addProduct(product);

            molecule = new CMLMolecule();
            molecule.setRef(edge.getCmlId());
            product = new CMLProduct();
            product.addMolecule(molecule);
            product.setRole("neutral_loss");
            productList.addProduct(product);

            reaction.addReactantList(reactantList);
            reaction.addProductList(productList);
            reactionList.addReaction(reaction);
        }
        rootElement.appendChild(reactionList);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filename);
            e.printStackTrace();
        }
        try {
            rootElement.debug(fos, 4);
        } catch (IOException e) {
            System.err.println("Cannot output file: "+e);
            e.printStackTrace();
        }
    }

    @Override
    public FragmentationTree clone() {
        FragmentationTree clonedFragTree = new FragmentationTree(session, fileName);
        buildUpClonedGraph(clonedFragTree);
        return clonedFragTree;
    }

    protected void buildUpClonedGraph(FragmentationTree clonedGraph) {
        super.buildUpClonedGraph(clonedGraph);
        clonedGraph.setWarnDoubleNodes(warnDoubleNodes);
    }

    public void deleteNodes(List<FragmentationTreeNode> nodesToDelete) {
        for (Iterator<FragmentationTreeNode> nodeIter = nodesToDelete.iterator(); nodeIter.hasNext();) {

            FragmentationTreeNode node = nodeIter.next(), parent = node.getParent();
            // do not delete the root
            if (node == root) {
                System.err.println("Tried to delete the root!");
                return;
            }
            List<FragmentationTreeNode> children = node.getChildren();
            for (Iterator<FragmentationTreeEdge> iter = edges.iterator(); iter.hasNext();) {
                FragmentationTreeEdge e = iter.next();
                if (e.getFromNode() == node) {
                    iter.remove();
                    e.getToNode().removeInEdge(e);
                }
                if (e.getToNode() == node){
                    iter.remove();
                    e.getFromNode().removeOutEdge(e);
                }
            }

            for (FragmentationTreeNode child : children) {
                this.connect(parent, child);
            }
        }
        nodes.removeAll(nodesToDelete);

    }

}
