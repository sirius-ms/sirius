
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

package fragtreealigner.domainobjects.db;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import fragtreealigner.FragmentationTreeAligner.ExecutionMode;
import fragtreealigner.algorithm.ScoringFunctionNeutralLosses;
import fragtreealigner.algorithm.ScoringFunctionNeutralLosses.ScoreWeightingType;
import fragtreealigner.algorithm.TreeAligner;
import fragtreealigner.algorithm.TreeAligner.NormalizationType;
import fragtreealigner.domainobjects.Alignment;
import fragtreealigner.domainobjects.AlignmentComparator;
import fragtreealigner.domainobjects.graphs.AlignmentTree;
import fragtreealigner.domainobjects.graphs.FragmentationTree;
import fragtreealigner.domainobjects.graphs.FragmentationTreeEdge;
import fragtreealigner.domainobjects.graphs.FragmentationTreeNode;
import fragtreealigner.ui.MainFrame;
import fragtreealigner.util.Parameters;
import fragtreealigner.util.Session;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;

@SuppressWarnings("serial")
public class FragmentationTreeDatabase implements Serializable {
    private File directory;
    private List<FragmentationTreeDatabaseEntry> entries;
    private Session session;
    private DatabaseStatistics databaseStatistics;
    private Random randGen = new Random();

    public enum DecoyType { REVERSE, RANDOM, DB, ALT_DB, NONE }

    public FragmentationTreeDatabase(File directory, Session session) throws IOException, FileNotFoundException {
        this(directory, session, false);
    }

    public FragmentationTreeDatabase(File directory, Session session, boolean isAltDB) throws IOException, FileNotFoundException {
        this.directory = directory;
        this.session = session;

        if (isAltDB) {
            session.setAltFragTreeDB(this);
        } else {
            session.setFragTreeDB(this);
        }
        entries = new ArrayList<FragmentationTreeDatabaseEntry>();
        FragmentationTreeDatabaseEntry entry;
        FragmentationTree fragTree, decoyFragTree;
        AlignmentTree aligTree, decoyAligTree;
        //		boolean useDecoyDB = session.getParameters().useDecoyDB;
        DecoyType decoyType = session.getParameters().decoyType;

        String[] fileList = directory.list();
        Arrays.sort(fileList);
        for (String fragTreeStr : fileList) {
            if (fragTreeStr.endsWith(".dot")) {
                if (session.getParameters().makeVerboseOutput) System.err.println("Reading "+fragTreeStr);
                fragTree = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader(directory.getPath() + "/"+ fragTreeStr)), fragTreeStr, session);
            } else if (fragTreeStr.endsWith(".cml")) {
                fragTree = FragmentationTree.readFromCml(FileUtils.ensureBuffering(new FileReader(directory.getPath() + "/"+ fragTreeStr)), fragTreeStr, session);
            } else fragTree = null;
            //TODO selection of large trees
            if (fragTree != null /*&& fragTree.size() > 5*/) {
                fragTree.setId(fragTreeStr);
                fragTree.setType(fragTreeStr.substring(0, 1));
                aligTree = fragTree.toAlignmentTree();
                entry = new FragmentationTreeDatabaseEntry(fragTreeStr, fragTree, aligTree);
                entries.add(entry);
            }
        }
        computeStatistics();

        if (!decoyType.equals(DecoyType.NONE) && !isAltDB) {
            for (FragmentationTreeDatabaseEntry dbEntry : entries) {
                fragTree = dbEntry.getFragmentationTree();
                decoyFragTree = fragTree.buildDecoyTree();
                decoyAligTree = decoyFragTree.toAlignmentTree();
                decoyAligTree.setId("DECOY_"+fragTree.getId()+"_"+ decoyAligTree.getId());
                decoyFragTree.writeToDot(new BufferedWriter(new FileWriter("decoy/"+decoyAligTree.getId())));
                decoyAligTree.setType("d");
                dbEntry.setDecoyAlignmentTree(decoyAligTree);
            }
        }
        //		unnecessary due to the simple self alig method in AlignmentTree.getSelfAligScore();
        //if (session.getParameters().normalizationType.equals(NormalizationType.SELF_ALIG_ARITHMETIC) || session.getParameters().normalizationType.equals(NormalizationType.SELF_ALIG_GEOMETRIC)) computeSelfAlignments();
    }

    private void computeStatistics() {
        databaseStatistics = new DatabaseStatistics(session);
        for (FragmentationTreeDatabaseEntry dbEntry : entries) {
            databaseStatistics.addFragmentationTree(dbEntry.getFragmentationTree());
        }
        //		databaseStatistics.printStatistics();
    }

    private void computeSelfAlignments() {
        AlignmentTree aligTree;
        ScoringFunctionNeutralLosses sFuncNL = null;
        if (session.getParameters().scoreWeightingType.equals(ScoreWeightingType.NEUTRAL_LOSS_FREQUENCY)) sFuncNL = new ScoringFunctionNeutralLosses(this, session);
        else sFuncNL = new ScoringFunctionNeutralLosses(session);
        for (FragmentationTreeDatabaseEntry dbEntry : entries) {
            aligTree = dbEntry.getAlignmentTree();
            TreeAligner treeAligner = new TreeAligner(aligTree, aligTree, sFuncNL, session);
            treeAligner.setLocal(false);
            treeAligner.setNormalizationType(NormalizationType.NONE);
            aligTree.setSelfAligScore(treeAligner.performAlignment().getScore());
        }
    }

    public File getDirectory() {
        return directory;
    }

    public DatabaseStatistics getDatabaseStatistics() {
        return databaseStatistics;
    }

    public float[] compareFragmentationTreeWithDatabase(String file) throws IOException, FileNotFoundException {
        Parameters params = session.getParameters();
        FragmentationTree queryFragTree = null;
        String fileLastPart = file.substring(file.lastIndexOf(File.separatorChar)+1);
        if (file.endsWith(".dot")) queryFragTree = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader(file)), fileLastPart, session);
        else if (file.endsWith(".cml")) queryFragTree = FragmentationTree.readFromCml(FileUtils.ensureBuffering(new FileReader(file)), fileLastPart, session);
        if (queryFragTree == null) { return null; }
        queryFragTree.setType(file.substring(file.lastIndexOf("/") + 1, file.lastIndexOf("/") + 2));
        queryFragTree.setId(file.substring(file.lastIndexOf("/") + 1));
        AlignmentTree queryAligTree = queryFragTree.toAlignmentTree();
        ScoringFunctionNeutralLosses sFuncNL = null;
        if (params.scoreWeightingType.equals(ScoreWeightingType.NEUTRAL_LOSS_FREQUENCY)) sFuncNL = new ScoringFunctionNeutralLosses(this, session);
        else sFuncNL = new ScoringFunctionNeutralLosses(session);

        List<Alignment> alignments = new ArrayList<Alignment>();
        TreeAligner treeAligner;
        Alignment alignment;
        boolean useDecoyDB = !params.decoyType.equals(DecoyType.NONE);

        if (session.getParameters().makeGraphicalOutput && session.getMainFrame() == null) {
            session.setMainFrame(new MainFrame(session));
        }

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long startCpuTimeNano, endCpuTimeNano;
        String runningTimes= "";

        //if the dirs are equal, we need to calculate only half of the aligns.
        //if not, we are always past the query entry, every Align is calculated
        boolean pastQueryEntry = !(getDirectory().equals(new File(file).getParent())) || session.getParameters().makeGraphicalOutput;
        for (FragmentationTreeDatabaseEntry dbEntry : entries) {
            String fullFileName = getDirectory().getAbsolutePath()+File.separator+dbEntry.getFilename();
            if (fullFileName.equals(file) && !session.getParameters().makeGraphicalOutput) {
                treeAligner = new TreeAligner(queryAligTree, dbEntry.getAlignmentTree(), sFuncNL, session);
                alignment = treeAligner.performSelfAlignment();
                alignments.add(alignment);
                pastQueryEntry = true;
            } else if (!pastQueryEntry) {
                Alignment reverseAlignment = null;
                for (Alignment al : alignments){
                    String name1 = al.getTree1().getCorrespondingFragTree().getFileName();
                    String name2 = al.getTree2().getCorrespondingFragTree().getFileName();
                    if (name1.equals(dbEntry.getFilename()) && name2.equals(fileLastPart)){
                        reverseAlignment = al;
                    }
                    if (reverseAlignment == null) {
                        throw new NullPointerException(file+" "+dbEntry.getFilename()+"not in alignments");
                    }
                    alignments.add(reverseAlignment.reverse());
                }
            } else {
                try {
                    treeAligner = new TreeAligner(queryAligTree, dbEntry.getAlignmentTree(), sFuncNL, session);

                    startCpuTimeNano = System.currentTimeMillis();
                    alignment = treeAligner.performAlignment();
                    endCpuTimeNano = System.currentTimeMillis();

                    String tree1 =  queryAligTree.getCorrespondingFragTree().getFileName();//.split("\\.")[0].split("\\_")[1];
                    String tree2 =  dbEntry.getAlignmentTree().getCorrespondingFragTree().getFileName();//.split(".")[0].split("\\_")[1];

                    runningTimes+=tree1+"_"+tree2+"\t";
                    runningTimes+=(endCpuTimeNano-startCpuTimeNano)+"\t"+alignment.getScore()+"\n";

                    alignments.add(alignment);
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                    alignment = new Alignment(queryAligTree, dbEntry.getAlignmentTree(), null, Float.NaN, null, 0, 0, session);
                    alignments.add(alignment);
                }
            }
            if (dbEntry.getDecoyAlignmentTree() != null && useDecoyDB) {
                try {
                    treeAligner = new TreeAligner(queryAligTree, dbEntry.getDecoyAlignmentTree(), sFuncNL, session);
                    alignment = treeAligner.performAlignment();
                    alignments.add(alignment);
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                    alignment = new Alignment(queryAligTree, dbEntry.getDecoyAlignmentTree(), null, Float.NaN, null, 0, 0, session);
                    alignments.add(alignment);
                }
            }
        }

//        if (params.isNodeUnionAllowed){
//           BufferedWriter out = new BufferedWriter(new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/ILPInstances/runningTimesJoin.txt",true));
//            out.write(runningTimes);
//            out.close();
//        }else{
//            BufferedWriter out = new BufferedWriter(new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/ILPInstances/runningTimes.txt",true));
//            out.write(runningTimes);
//            out.close();
//        }


        if (!params.makeGraphicalOutput && !params.makeMatrixOutput && !params.executionMode.equals(ExecutionMode.PARAMETER_OPTIMIZATION)) {
            if (params.makeTopOutput) System.out.print(queryFragTree.getId().substring(2, queryFragTree.getId().indexOf('.')));
            else System.out.println(queryFragTree.getId() + "\n-----------");
        }

        int k = 0;
        float top3 = 0, top5 = 0, top10 = 0;
        if (!params.makeMatrixOutput) {
            Collections.sort(alignments, new AlignmentComparator());
            if (queryAligTree.getType() != null) {
                float[] topV = new float[alignments.size()];
                int x = 0, j, correctClass;
                float score;
                for (int i = alignments.size() - 1; i >= 0; i--) {
                    if (alignments.get(i).getTree2().getId().equals(queryAligTree.getId())) continue;
                    score = alignments.get(i).getScore();
                    correctClass = 0;
                    k = 0;
                    for (j = i; j >= 0; j--) {
                        if (alignments.get(j).getTree2().getId().equals(queryAligTree.getId())) continue;
                        if (alignments.get(j).getScore() != score) break;
                        k++;
                        if (alignments.get(j).getTree2().getType() != null && alignments.get(j).getTree2().getType().equals(queryAligTree.getType())) {
                            correctClass++;
                        }
                    }
                    for (int j2 = i; j2 > j; j2--) {
                        if (alignments.get(j2).getTree2().getId().equals(queryAligTree.getId())) continue;
                        topV[x] = (float)correctClass / (float)(k);
                        x++;
                    }
                    i = j + 1;
                }
                for (int i = 0; i < topV.length && i < 10; i++) {
                    if (i < 3) top3 += topV[i];
                    if (i < 5) top5 += topV[i];
                    if (i < 10) top10 += topV[i];
                }
            }
        }

        float[] score = new float[]{(float)top3 / 3, (float)top5 / 5, (float)top10 / 10};

        if (!params.executionMode.equals(ExecutionMode.PARAMETER_OPTIMIZATION)) {
            if (params.makeGraphicalOutput) {
                session.getMainFrame().addAlignmentSet(queryAligTree.getId(), alignments);
            }
            if (params.makeMatrixOutput) {
                if (params.normalizationType.equals(NormalizationType.ALL)) {
                    System.out.print("\"" + queryAligTree.getId() + "\"");
                    for (Alignment resAlignment : alignments) {
                        String additionalInfo = getAdditionalInfo(resAlignment, params.makeMatrixExtOutput);
                        System.out.print(", \"" + resAlignment.getScore() + additionalInfo + "\"");
                    }
                    System.out.println();
                    for (int i = 0; i < NormalizationType.values().length; i++) {
                        System.out.print("\"" + queryAligTree.getId() + "_n" + i + "\"");
                        for (Alignment resAlignment : alignments) {
                            String additionalInfo = getAdditionalInfo(resAlignment, params.makeMatrixExtOutput);
                            System.out.print(", \"" + resAlignment.getScoreList().get(i) + additionalInfo + "\"");
                        }
                        System.out.println();
                    }
                } else {
                    System.out.print("\"" + queryAligTree.getId() + "\"");
                    for (Alignment resAlignment : alignments) {
                        String additionalInfo = getAdditionalInfo(resAlignment, params.makeMatrixExtOutput);
                        System.out.print(", \"" + resAlignment.getScore() + additionalInfo + "\"");
                    }
                    System.out.println();
                }
            } else if (params.makeTopOutput) {
                System.out.println("\t& " + Float.toString(score[0]) + "\t& "+ Float.toString(score[1]) + "\t& " + Float.toString(score[2]));
            } else if (!params.makeGraphicalOutput){
                for (Alignment resAlignment : alignments) {
                    System.out.println((int)(resAlignment.getScore()) + "\t" + resAlignment.getTree2().getId());
                }
                System.out.println("Top3: " + Float.toString(score[0]) + "  Top5: "+ Float.toString(score[1]) + "  Top10: " + Float.toString(score[2]) + "\n");
            }
        }
        return score;
    }

    private String getAdditionalInfo(Alignment alignment, boolean makeMatrixExtOutput) {
        if (!makeMatrixExtOutput) return "";
        String additionalInfo = "";
        additionalInfo += " " + alignment.getPlikeValue();
        additionalInfo += " " + alignment.getTree1().size();
        additionalInfo += " " + alignment.getTree2().size();
        if (alignment.getAlignmentResult() != null){
            additionalInfo += " " + alignment.getAlignmentResult().size();
        } else {
            additionalInfo += " 0";
        }
        return additionalInfo;
    }

    public float compareFragmentationTreesWithDatabase(String directory) throws IOException, FileNotFoundException {
        File fragTreeDir = new File(directory);
        String[] fileList = fragTreeDir.list();
        Arrays.sort(fileList);
        float[] score;
        float[] scoreSum = new float[]{0, 0, 0};
        float overallScore = 0;
        float fragTrees = 0;

        if (session.getParameters().makeGraphicalOutput) {
            session.setMainFrame(new MainFrame(session));
        }
        if (session.getParameters().makeMatrixOutput) {
            int i = 0;
            for (FragmentationTreeDatabaseEntry dbEntry : entries) {
                System.out.print("\"" + dbEntry.getAlignmentTree().getId() + "\"");
                if (!session.getParameters().decoyType.equals(DecoyType.NONE)) {
                    System.out.print(", \"DECOY_" + dbEntry.getAlignmentTree().getId() + "\"");
                }
                if (i != (entries.size() - 1)) System.out.print(", ");
                i++;
            }
            System.out.println();
        }

        for (String fragTreeFile : fileList) {
            if (fragTreeFile.endsWith(".dot") || fragTreeFile.endsWith(".cml")) {
                score = compareFragmentationTreeWithDatabase(directory + "/" + fragTreeFile);
                if (!(score == null) && !fragTreeFile.startsWith("u")) {
                    fragTrees++;
                    scoreSum[0] += score[0];
                    scoreSum[1] += score[1];
                    scoreSum[2] += score[2];
                }
            }
        }
        scoreSum[0] /= fragTrees;
        scoreSum[1] /= fragTrees;
        scoreSum[2] /= fragTrees;
        overallScore = scoreSum[0] * 2.0f + scoreSum[1] * 1.5f + scoreSum[2];
        if (!session.getParameters().executionMode.equals(ExecutionMode.PARAMETER_OPTIMIZATION) && !session.getParameters().makeMatrixOutput) {
            System.out.println("\nScore sum\nTop3: " + scoreSum[0] + "  Top5: " + scoreSum[1] + "  Top10: " + scoreSum[2] + "\nOverall score: " + overallScore);
        }
        if (session.getParameters().makeGraphicalOutput) {
            //			session.getMainFrame().pack();
            //			session.getMainFrame().setVisible(true);
        }
        return overallScore;
    }

    public FragmentationTree getRandomTreeTopology(int outTreeSize) {
        // For tree selection, calculate the weights
        int[] treeWeights = new int[this.entries.size()];
        int totaltreeWeights = 0;
        int i = 0;
        for (FragmentationTreeDatabaseEntry entry : this.entries){
            int thisWeight = entry.getFragmentationTree().size()-outTreeSize+1;
            if (thisWeight > 0){
                treeWeights[i] = thisWeight;
                totaltreeWeights += thisWeight;
            } else {
                treeWeights[i] = 0;
            }
            ++i;
        }
        int randomNumber = randGen.nextInt(totaltreeWeights);
        i = 0;
        for (int sum = 0; sum <= randomNumber && i < treeWeights.length; ){
            sum += treeWeights[i];
            ++i;
        }
        if (i == 0 || i > treeWeights.length){
            System.err.println("There were no trees with size > "+outTreeSize+" in the given decoyDB");
            return null;
        }

        /*
          // Sampling
          // Tree i-1 is the randomly chosen:
          FragmentationTree decoyTree = entries.get(i-1).getFragmentationTree().clone();

          // Create a sampler weighting the nodes by out degree
          int[] nodeWeights = new int[decoyTree.size()];
          int totalNodeWeights = 0;
          i = 0;
          for (FragmentationTreeNode node : decoyTree.getNodes()){
              nodeWeights[i] = node.numChildren()+1;
              totalNodeWeights += node.numChildren()+1;
              ++i;
          }
          Set<FragmentationTreeNode> retainedNodes = new HashSet<FragmentationTreeNode>(outTreeSize);
          // always retain the root
          retainedNodes.add(decoyTree.determineRoot());
          while (retainedNodes.size() < outTreeSize){
              randomNumber = randGen.nextInt(totalNodeWeights);
              i = 0;
              for (int sum = 0; sum <= randomNumber && i < nodeWeights.length; ){
                  sum += nodeWeights[i];
                  ++i;
              }
              if (i == 0 || i > nodeWeights.length){
                  System.err.println("Problem with node sampling");
                  return null;
              }
              retainedNodes.add(decoyTree.getNodes().get(i-1));
          }
          List<FragmentationTreeNode> nodesToRemove = new ArrayList<FragmentationTreeNode>(decoyTree.size());
          nodesToRemove.addAll(decoyTree.getNodes());
          for (FragmentationTreeNode node : retainedNodes){
              nodesToRemove.remove(node);
          }
          if (nodesToRemove.size() + outTreeSize != decoyTree.size()){
              System.err.println("Something is wrong with the nodes to delete!");
              System.err.println(nodesToRemove.size()+" + "+outTreeSize+" = "+decoyTree.size()+" "+retainedNodes.size());
          }
          decoyTree.deleteNodes(nodesToRemove);
           */

        // Grow decoy tree
        // Tree i-1 is the randomly chosen:
        FragmentationTree baseTree = entries.get(i-1).getFragmentationTree();
        FragmentationTree decoyTree = new FragmentationTree(session);
        decoyTree.setId(baseTree.getId());
        randomNumber = randGen.nextInt(baseTree.size());
        FragmentationTreeNode newNode = baseTree.getNodes().get(randomNumber);
        Map<FragmentationTreeNode, FragmentationTreeNode> addedNodes = new HashMap<FragmentationTreeNode, FragmentationTreeNode>(outTreeSize);
        addedNodes.put(newNode, decoyTree.addNode(newNode.getLabel()));
        List<FragmentationTreeEdge> environment = new ArrayList<FragmentationTreeEdge>();
        environment.addAll(newNode.getOutEdges());
        if (newNode.getInEdge() != null){
            environment.add(newNode.getInEdge());
        }
        while (decoyTree.size() < outTreeSize){
            randomNumber = randGen.nextInt(environment.size());
            FragmentationTreeEdge newEdge = environment.remove(randomNumber);
            // determine which of the nodes is already there.
            if (addedNodes.containsKey(newEdge.getFromNode())){
                FragmentationTreeNode newDecoyNode = decoyTree.addNode(newEdge.getToNode().getLabel());
                addedNodes.put(newEdge.getToNode(), newDecoyNode);
                decoyTree.connect(addedNodes.get(newEdge.getFromNode()), newDecoyNode);
                environment.addAll(newEdge.getToNode().getOutEdges());
            } else if (addedNodes.containsKey(newEdge.getToNode())){
                FragmentationTreeNode newDecoyNode = decoyTree.addNode(newEdge.getFromNode().getLabel());
                addedNodes.put(newEdge.getFromNode(), newDecoyNode);
                decoyTree.connect(newDecoyNode, addedNodes.get(newEdge.getToNode()));
                environment.addAll(newEdge.getFromNode().getOutEdges());
                environment.remove(newEdge);
                if (newEdge.getFromNode().getInEdge() != null){
                    environment.add(newEdge.getFromNode().getInEdge());
                }
            } else {
                System.err.println("something wrong in the growing process");
            }
        }
        decoyTree.determineRoot();

        return decoyTree;
    }

    public List<FragmentationTreeDatabaseEntry> getEntries() {
        return entries;
    }

    public FragmentationTree generateRandomTree(int size){
        FragmentationTree randomTree = session.getAltFragTreeDB().getRandomTreeTopology(size);
        // workaround if DB does not contain a larger tree, should not happen!
        if (randomTree == null){
            System.err.println("at size "+size);
        }
        for (FragmentationTreeEdge edge : randomTree.getEdges()){
            edge.setNeutralLoss(this.getDatabaseStatistics().getRandomNeutralLoss());
            //			TODO: other edge attributes like dbe and score have to be adapted, too!!!
        }
        return randomTree;

    }

    public void calculateStatistics(int maxSize) throws IOException, FileNotFoundException {
        Parameters params = session.getParameters();
        ScoringFunctionNeutralLosses sFuncNL = null;
        if (params.scoreWeightingType.equals(ScoreWeightingType.NEUTRAL_LOSS_FREQUENCY)) sFuncNL = new ScoringFunctionNeutralLosses(this, session);
        else sFuncNL = new ScoringFunctionNeutralLosses(session);

        new File(params.statOutDir).mkdirs();
        for (int size1 = 1; size1<= maxSize; ++size1){
            for (int size2 = size1; size2<=maxSize; ++size2) {
                File outfile = new File(params.statOutDir+File.pathSeparator+size1+"x"+size2);
                try { // to catch outOfMemoryError, perhaps...
                    // check whether statistics for tree sizes already exist
                    if (!outfile.exists()) {
                        System.out.println("Starting " + size1 + " " + size2);
                        BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
                        for (int i = 0; i < params.calcStatistics; i++) {
                            AlignmentTree tree1 = generateRandomTree(size1)
                                    .toAlignmentTree();
                            AlignmentTree tree2 = generateRandomTree(size2)
                                    .toAlignmentTree();

                            TreeAligner ta = new TreeAligner(tree1, tree2,
                                    sFuncNL, session);
                            try {
                                double score = ta.performAlignment().getScore();
                                out.write(Double.toString(score));
                                out.newLine();
                                if (i % 10 == 0)
                                    out.flush();
                            } catch (ArrayIndexOutOfBoundsException e) {
                                // try again
                                System.out.println("Another try");
                                --i;
                            }
                        }
                        out.close();
                    }
                } catch (OutOfMemoryError e) {
                    //no op
                }
            }
        }
    }
}
