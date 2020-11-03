
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

package fragtreealigner.util;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import fragtreealigner.algorithm.ScoringFunctionNeutralLosses;
import fragtreealigner.domainobjects.graphs.*;

import java.io.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: fhufsky
 * Date: 8/3/11
 * Time: 6:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class ParseToAlignmentInstance {

    public static void main(String[] args) {

        String database = "massbank";

        File dir = new  File("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+database+"/ilp_trees/ilp_lambda0.04");

        File[] files = dir.listFiles();



        Session session = new Session();
        Parameters params = new Parameters(session);
        params.useCnl=false;
        params.testRDiff=false;
        params.testRDiffH2=false;
        params.testH2=false;

        session.setParameters(params);

        for (int i=0; i<files.length; i++){

            for (int j=i; j<files.length; j++){


                if (files[i].isFile() && files[j].isFile() /*&& files[i].getName().contains("Serine") && files[j].getName().contains("Serine")*/){

                    String name1 = "";
                    String name2="";
                    if (database.equals("10x10compounds")){
                        name1=files[i].getName().split("\\.")[0].split("\\_")[1];
                        name2 = files[j].getName().split("\\.")[0].split("\\_")[1];
                    } else{
                        name1=files[i].getName().split("\\.")[0];
                        name2 = files[j].getName().split("\\.")[0];
                    }


                    System.out.println(name1 +" vs. "+name2);


                    String filename1 = files[i].getAbsolutePath();
                    String filename2 = files[j].getAbsolutePath();


                    //read input
                    FragmentationTree  fTree1 =null;
                    FragmentationTree fTree2 =null;
                    try {
                        fTree1 = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader(filename1)), session);
                        fTree2 = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader(filename2)), session);
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }


                    AlignmentTree aTree1 = fTree1.toAlignmentTree();
                    List<AlignmentTreeNode> nodes1 = aTree1.getNodes();
                    List<AlignmentTreeEdge> edges1 = aTree1.getEdges();

                    AlignmentTree aTree2 = fTree2.toAlignmentTree();
                    List<AlignmentTreeNode> nodes2 = aTree2.getNodes();
                    List<AlignmentTreeEdge> edges2 = aTree2.getEdges();


                    try {

                        FileWriter stats = new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/ILPInstances/"+database+"/stats.txt",true);

                        int maxdegree1 =0;
                        for (FragmentationTreeNode n : fTree1.getNodes()){
                            maxdegree1 = Math.max(n.getOutEdges().size(),maxdegree1);
                        }

                        int maxdegree2 = 0;
                        for (FragmentationTreeNode n : fTree2.getNodes()){
                            maxdegree2 = Math.max(n.getOutEdges().size(), maxdegree2);
                        }


                       // stats.write(name1+"\t"+name2+"\t"+fTree1.getNodes().size()+"\t"+fTree2.getNodes().size()+"\t"+maxdegree1+"\t"+maxdegree2+"\n");
                       // stats.close();

                        if (maxdegree1>3 && maxdegree2>3){


                            FileWriter output = new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/ILPInstances/"+database+"/instances/"+name1+"_"+name2+".txt");

                            //write tree 1
                            for (AlignmentTreeEdge edge : edges1){
                                if (edge.getFromNode().toString().equals("root")){
                                    output.write("0\t" + edge.getToNode().getLabel()+"\n" );

                                } else{
                                    output.write(""+edge.getFromNode().getLabel()+"\t" + edge.getToNode().getLabel()+"\n" );
                                }

                            }

                            output.write("\n");

                            //write tree 1
                            for (AlignmentTreeEdge edge : edges2){
                                if (edge.getFromNode().toString().equals("root")){
                                    output.write("0\t" + edge.getToNode().getLabel()+"\n" );

                                } else{
                                    output.write(""+edge.getFromNode().getLabel()+"\t" + edge.getToNode().getLabel()+"\n" );
                                }

                            }

                            output.write("\n");

                            //write  gaps tree 1
                            int counter =0;
                            for (AlignmentTreeNode n1 : nodes1){
                                if (counter==0) output.write(params.scoreGap+"");
                                else output.write("\t"+params.scoreGap);
                                counter++;
                            }
                            output.write("\n\n");

                            //write  gaps tree 2
                            counter =0;
                            for (AlignmentTreeNode n2 : nodes2){
                                if (counter==0) output.write(params.scoreGap+"");
                                else output.write("\t"+params.scoreGap);
                                counter++;
                            }
                            output.write("\n\n");

                            ScoringFunctionNeutralLosses sFuncNL = new ScoringFunctionNeutralLosses(session);

                            //write  matches
                            for (AlignmentTreeNode n1 : nodes1) {
                                counter=0;
                                for (AlignmentTreeNode n2 : nodes2){
                                    double score = sFuncNL.score(n1,n2);
                                    if (counter==0) output.write(""+score);
                                    else  output.write("\t"+score);
                                    counter++;
                                }
                                output.write("\n");

                            }
                            output.write("\n");


                            //write  joins
                            for (AlignmentTreeNode n1 : nodes1) {
                                counter=0;
                                for (AlignmentTreeNode n2 : nodes2){
                                    double score = sFuncNL.score(n1,n1.getParent(),n2);

                                    String s = "";
                                    if (Double.isInfinite(score))  s="*";
                                    else{
                                        score -= sFuncNL.score(n1,n2);
                                        s+=score;
                                    }

                                    if (counter==0) output.write(""+s);
                                    else  output.write("\t"+s);
                                    counter++;

                                }
                                output.write("\n");

                            }
                            output.write("\n");


                            //write  joins
                            for (AlignmentTreeNode n2 : nodes2) {
                                counter=0;
                                for (AlignmentTreeNode n1 : nodes1){
                                    double score = sFuncNL.score(n2,n2.getParent(),n1);
                                    String s = "";
                                    if (Double.isInfinite(score))  s="*";
                                    else{
                                        score -= sFuncNL.score(n2,n1);

                                        s+=score;
                                    }

                                    if (counter==0) output.write(""+s);
                                    else  output.write("\t"+s);
                                    counter++;

                                }
                                output.write("\n");

                            }

                            //write root score

                            output.close();


                            //TreeAligner treeAligner = new TreeAligner(aTree1, aTree2, sFuncNL, session);
                            //Alignment alig = treeAligner.performAlignment();
                            //Alignment alig = Macros.performFragTreeAlignment(files[i].getAbsolutePath(), files[j].getAbsolutePath(), session);
                            //System.out.println(alig.getScore());

                        }


                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }



//
//
                }


//
//                try {
//                    BufferedReader f1 = FileUtils.ensureBuffering(new FileReader(files[i]));
//                    BufferedReader f2 = FileUtils.ensureBuffering(new FileReader(files[j]));
//
//
//                    //read tree 1
//
//                    for (String line = f1.readLine(); line!=null; line=f1.readLine()){
//
//                        if (line.contains("->")){
//
//
//
//                        }
//
//                    }







//            } catch (FileNotFoundException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            } catch (IOException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }


            }

        }





    }
}
