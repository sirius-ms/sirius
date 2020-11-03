
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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: fhufsky
 * Date: 7/11/11
 * Time: 10:08 AM
 * To change this template use File | Settings | File Templates.
 */
public class StructureScore2Distance {

    public static void main(String[] args) {
          String filename = "featureTrees";
        String dataset = "massbank";
        String trees ="ilp_lambda0.01";

        int minSize=0;

//        String directory ="normalized";
//        if (minSize>0){
//            directory+=">"+(minSize-1);
//        }

        String path =  "/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/structural_alignments/"+filename+".csv";

        String compoundsPath =  "/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/alignments/normalized>2/"+trees+"PValues.csv";




        try {

            BufferedReader compoundsFile = FileUtils.ensureBuffering(new FileReader(compoundsPath));

            String line =  "";
            List<String> compoundNames = new ArrayList<String>();

            for (line = compoundsFile.readLine(); line!=null; line=compoundsFile.readLine()){

                String name = line.split("\\,")[0];
                //System.out.println(name);
                compoundNames.add(name);




            }







            BufferedReader file = FileUtils.ensureBuffering( new FileReader(path));


            line = file.readLine(); // first line only contains names
            System.out.println(line);
            String names[] = line.split("\\, ");
            int number = names.length;
            double[][] scores = new double[number][number];

            int linecounter=0;

            for (line = file.readLine(); line!=null; line=file.readLine()){


                String[] split = line.split("\\,");
                System.out.println(split[0]);

                for (int i = 1; i<split.length; i++){


                    String entry = split[i];
                    entry = entry.trim();
                    entry = entry.replaceAll("\"","");


                    String[] values = entry.split("\\ ");

                    double score = Double.parseDouble(values[0]);
                    scores[linecounter][i-1]=score;




                }

                linecounter++;



            }

            List<Integer> toSmall = new ArrayList<Integer>();

//            for (int i=0; i<number; i++){
//                //System.out.println(sizes1[i][0]);
//                if (sizes1[i][0]<=minSize){
//                    toSmall.add(i);
//                    System.out.println(i);
//                }
//
//            }



            for (int i=0; i<number; i++){

                if(compoundNames.contains(names[i])) System.out.println(names[i]+" contained in name list");
                else System.out.println(names[i] + " not conatined in name list");

            }




            double[][] euklidDistance = new double[number][number];


            for (int i=0; i<number; i++){
                for (int j=0; j<number; j++){

                    if (!toSmall.contains(i) && !toSmall.contains(j) && compoundNames.contains(names[i])&& compoundNames.contains(names[j])){
                        double sum =0;

                        for (int k=0; k<number; k++){
                            sum += Math.pow(scores[i][k]-scores[j][k],2);
                        }

                        euklidDistance[i][j]=Math.pow(sum,0.5);
                    }


                }
            }

            double[] means = new double[number];

            for (int i=0; i<number; i++){
                if (!toSmall.contains(i) && compoundNames.contains(names[i])){

                    double sum =0;

                    for (int j=0; j<number; j++){
                        if (!toSmall.contains(j)&& compoundNames.contains(names[j]))sum += scores[i][j];
                    }

                    means[i] = sum/number;
                }
            }


            double[][] correlationCoefficient = new double[number][number];


            for (int i=0; i<number; i++){
                for (int j=0; j<number; j++){

                    if (!toSmall.contains(i) && !toSmall.contains(j)&& compoundNames.contains(names[i])&& compoundNames.contains(names[j])){
                        double combinedSum =0;
                        double isum =0;
                        double jsum=0;

                        for (int k=0; k<number; k++){

                            combinedSum += (scores[i][k]-means[i])*(scores[j][k]-means[j]);
                            isum += Math.pow(scores[i][k]-means[i],2);
                            jsum += Math.pow(scores[j][k]-means[j],2);
                        }

                        correlationCoefficient[i][j]=combinedSum/(Math.pow(isum,0.5)*Math.pow(jsum,0.5));
                    }
                }
            }

            BufferedWriter out3 = new BufferedWriter(new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/structural_alignments/"+trees+filename+"Pearson.csv"));

            for (int i=0; i<number; i++){
                if (!toSmall.contains(i)&& compoundNames.contains(names[i])){
                    out3.write(names[i].trim());
                    for (int j=0; j<number; j++){
                        if (!toSmall.contains(j)&& compoundNames.contains(names[j]))out3.write(","+ euklidDistance[i][j]);
                    }
                    out3.write("\n");
                }
            }
            out3.close();

            BufferedWriter out2 = new BufferedWriter(new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/structural_alignments/"+trees+filename+"Euklid.csv"));

            for (int i=0; i<number; i++){
                if (!toSmall.contains(i)&& compoundNames.contains(names[i])){
                    out2.write(names[i].trim());

                    for (int j=0; j<number; j++){
                        if (!toSmall.contains(j)&& compoundNames.contains(names[j]))out2.write(","+ euklidDistance[i][j]);
                    }
                    out2.write("\n");
                }
            }
            out2.close();

            BufferedWriter out = new BufferedWriter(new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/structural_alignments/"+trees+filename+"Norm.csv"));

            for (int i=0; i<number; i++){
                if (!toSmall.contains(i)&& compoundNames.contains(names[i])){
                    out.write(names[i].trim());

                    for (int j=0; j<number; j++){
                        if (!toSmall.contains(j)&& compoundNames.contains(names[j]))out.write(","+scores[i][j]);
                    }
                    out.write("\n");
                }
            }
            out.close();



        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }




    }
}
