
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
import fragtreealigner.domainobjects.chem.basics.MolecularFormula;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: fhufsky
 * Date: 7/20/11
 * Time: 1:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class VertexMatcher {


    public static void main(String[] args) {


        String dataset = "massbank";
        String trees = "ilp_lambda0.04";

        File directory = new File("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/" + dataset + "/ilp_trees/" + trees);

        Map<String, Integer> scores = new HashMap<String, Integer>();

        boolean countOnlyCAndHAtoms = false;

        int filecounter =0;

        for (File file1 : directory.listFiles()) {

            filecounter++;
            System.out.println(filecounter+"\t"+file1.getName());

            for (File file2 : directory.listFiles()) {

                boolean hiddenFile =false;
                if (file1.getName().startsWith(".")) hiddenFile=true;
                if (file2.getName().startsWith(".")) hiddenFile=true;

                if (file1.isFile() && file2.isFile() && !hiddenFile) {


                    try {
                        BufferedReader f1 = FileUtils.ensureBuffering(new FileReader(file1));
                        BufferedReader f2 = FileUtils.ensureBuffering(new FileReader(file2));

                        List<String> explanations1 = new ArrayList<String>();
                        List<String> explanations2 = new ArrayList<String>();


                        String line = f1.readLine();

                        for (line = f1.readLine(); line != null; line = f1.readLine()) {

                            if (line.contains("->")) break;

                            if (line.contains("[")) {
                                String vertexName = line.split("\\[")[0];
                                explanations1.add(vertexName);
                            }

                        }

                        line = f2.readLine();


                        for (line = f2.readLine(); line != null; line = f2.readLine()) {

                            if (line.contains("->")) break;
                            if (line.contains("[")) {
                                String vertexName = line.split("\\[")[0];
                                explanations2.add(vertexName);
                            }
                        }


                        int matchCounter = 0;
                        int specialMatchCounter =0;


                        for (String v1 : explanations1) {
                            for (String v2 : explanations2) {

                                Session s = new Session();

                                Parameters param = new Parameters(s);

                                MolecularFormula m1 = new MolecularFormula(v1,s);
                                MolecularFormula m2 = new MolecularFormula(v2,s);

                                int c1 = m1.getNumberOfAtom("C");
                                int c2 = m2.getNumberOfAtom("C");

                                int h1 = m1.getNumberOfAtom("H");
                                int h2 = m1.getNumberOfAtom("H");

                                if (c1==c2 && h1==h2){
                                    specialMatchCounter++;
                                }

                                if (v1.equals(v2)) {
                                    matchCounter++;
                                }
                            }
                        }

                        //change type of counter here;
                        int counter = 0;

                        if (countOnlyCAndHAtoms) counter=specialMatchCounter;
                        else counter =matchCounter;


                        String key = file1.getName() + " x " + file2.getName();

                        //System.out.println(file1.getName() + " x " + file2.getName() + " : " + counter);


                        if (scores.get(key) == null) scores.put(key, counter);
                        else if (scores.get(key) != counter) System.err.println("scores are not matching!!");

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }

                }


            }


        }


        try {

            String path ="";
            if (countOnlyCAndHAtoms)path= "/Users/fhufsky/Uni/Projects/TreeAlignment/Data/" + dataset + "/vertexMatching_alignments/vertexMatchingCH.csv";
            else path = "/Users/fhufsky/Uni/Projects/TreeAlignment/Data/" + dataset + "/vertexMatching_alignments/vertexMatching.csv" ;

            BufferedWriter out = new BufferedWriter(new FileWriter(path));


            File[] fileList = directory.listFiles();

            int writtenFileNames = 0;

            for (int i = 0; i < fileList.length; i++) {

                if (fileList[i].isFile()) {
                    if (writtenFileNames != 0) {
                        out.write(",");
                    }
                    out.write(fileList[i].getName());
                    writtenFileNames++;
                }

            }

            out.write("\n");


            for (int i = 0; i < fileList.length; i++) {

                if (fileList[i].isFile()) {
                    String file1 = fileList[i].getName();

                    out.write(file1);

                    for (int j = 0; j < fileList.length; j++) {

                        if (fileList[j].isFile()) {
                            String file2 = fileList[j].getName();

                            String key = (file1 + " x " + file2);
                            String tree1 =  (file1 + " x " + file1);
                            String tree2 = (file2 + " x " + file2);

                            out.write(", \"" + scores.get(key)+" 0.0 "+scores.get(tree1)+" "+scores.get(tree2)+" "+scores.get(key)+"\"");

                        }


                    }

                    out.write("\n");
                }


            }

            out.close();


        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }
}
