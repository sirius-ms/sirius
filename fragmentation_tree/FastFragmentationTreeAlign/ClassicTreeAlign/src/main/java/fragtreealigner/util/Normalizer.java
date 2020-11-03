
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
 * Date: 6/29/11
 * Time: 1:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class Normalizer {

    public static void main(String[] args) {


        String alignment = "alignments";

        String filename = "ilp_lambda0.04_NLandNodeAlignment_NE5+0";
        String dataset = "massbankAND10x10compoundsANDQStar";
        int minSize=5;



        String directory ="normalized";
        if (minSize>0){
            directory+=">"+(minSize-1);
        }

        String path =  "/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/"+alignment+"/"+filename+".csv";


        // double[] expVektor = {0.25,0.5,2.0/3.0,0.75,0.7071,1.0};
        double[] expVektor = {0.75};



        try {

            //read input

            BufferedReader file = FileUtils.ensureBuffering( new FileReader(path));

            String line = file.readLine(); // first line only contains names
            String names[] = line.split("\\,");
            int number = names.length;
            double[][] scores = new double[number][number];
            int[][] sizes1 = new int[number][number];
            int[][] sizes2 = new int[number][number];
            int[][] losses = new int[number][number];


            int linecounter=0;

            for (line = file.readLine(); line!=null; line=file.readLine()){


                String[] split = line.split("\\,");
                System.out.println(split[0]);

                for (int i = 1; i<split.length; i++){


                    String entry = split[i];
                    entry = entry.trim();
                    entry = entry.replaceAll("\"","");
                    entry=entry.trim();

                    String[] values = entry.split("\\ ");

                    //System.out.println(entry);
                    double score = Double.parseDouble(values[0]);
                    scores[linecounter][i-1]=score;

                    int size1 = Integer.parseInt(values[2]);
                    sizes1[linecounter][i-1]=size1;

                    int size2 = Integer.parseInt(values[3]);
                    sizes2[linecounter][i-1]=size2;

                    int loss = Integer.parseInt(values[4]);
                    losses[linecounter][i-1]=loss;


                }

                linecounter++;



            }

            List<Integer> toSmall = new ArrayList<Integer>();

            //filter trees with to few losses
            for (int i=0; i<number; i++){
                if (sizes1[i][0]<=minSize){
                    toSmall.add(i);
                    System.out.println(i);
                }

            }


            for (int exp=0; exp<expVektor.length; exp++){

                double exponent=expVektor[exp];
                double[][] normalizedScores = new double[number][number];

                //compute normalization
                for (int i=0; i<number; i++){
                    for (int j=0; j<number; j++){
                        if (!toSmall.contains(i) && !toSmall.contains(j)){
                            double score = scores[i][j];
                            double smallerSingleScore =  Math.min(scores[i][i], scores[j][j]);
                            double normalizedScore = score/Math.pow(smallerSingleScore,exponent);
                            normalizedScores[i][j]=normalizedScore;
                        }
                    }
                }


                //compute euklidian distance
                double[][] euklidDistance = new double[number][number];


                for (int i=0; i<number; i++){
                    for (int j=0; j<number; j++){

                        if (!toSmall.contains(i) && !toSmall.contains(j)){
                            double sum =0;

                            for (int k=0; k<number; k++){
                                sum += Math.pow(normalizedScores[i][k]-normalizedScores[j][k],2);

                            }

                            euklidDistance[i][j]=Math.pow(sum,0.5);
                        }


                    }
                }

                //compute pearson correlation
                double[] means = new double[number];

                for (int i=0; i<number; i++){
                    if (!toSmall.contains(i)){

                        double sum =0;
                        int counter=0;

                        for (int j=0; j<number; j++){
                            if (!toSmall.contains(j)){
                                sum += normalizedScores[i][j];
                                counter++;
                            }
                        }

                        means[i] = sum/counter;

                    }
                }


                double[][] correlationCoefficient = new double[number][number];


                for (int i=0; i<number; i++){
                    for (int j=0; j<number; j++){

                        if (!toSmall.contains(i) && !toSmall.contains(j)){

                            double combinedSum =0;
                            double isum =0;
                            double jsum=0;

                            for (int k=0; k<number; k++){
                                combinedSum += (normalizedScores[i][k]-means[i])*(normalizedScores[j][k]-means[j]);
                                isum += Math.pow(normalizedScores[i][k]-means[i],2);
                                jsum += Math.pow(normalizedScores[j][k]-means[j],2);

                            }

                            correlationCoefficient[i][j]=combinedSum/(Math.pow(isum,0.5)*Math.pow(jsum,0.5));

                        }
                    }
                }

                //output
                BufferedWriter out3 = new BufferedWriter(new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/"+alignment+"/"+directory+"/"+filename+"Norm"+exponent+"Pearson.csv"));

                for (int i=0; i<number; i++){
                    if (!toSmall.contains(i)){
                        out3.write(names[i].trim());
                        for (int j=0; j<number; j++){
                            if (!toSmall.contains(j))out3.write(","+ correlationCoefficient[i][j]);
                        }
                        out3.write("\n");
                    }
                }
                out3.close();

                BufferedWriter out2 = new BufferedWriter(new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/"+alignment+"/"+directory+"/"+filename+"Norm"+exponent+"Euklid.csv"));

                for (int i=0; i<number; i++){
                    if (!toSmall.contains(i)){
                        out2.write(names[i].trim());

                        for (int j=0; j<number; j++){
                            if (!toSmall.contains(j))out2.write(","+ euklidDistance[i][j]);
                        }
                        out2.write("\n");
                    }
                }
                out2.close();

                BufferedWriter out = new BufferedWriter(new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/"+alignment+"/"+directory+"/"+filename+"Norm"+exponent+".csv"));

                for (int i=0; i<number; i++){
                    if (!toSmall.contains(i)){
                        out.write(names[i].trim());

                        for (int j=0; j<number; j++){
                            if (!toSmall.contains(j))out.write(","+normalizedScores[i][j]);
                        }
                        out.write("\n");
                    }
                }
                out.close();

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }
}
