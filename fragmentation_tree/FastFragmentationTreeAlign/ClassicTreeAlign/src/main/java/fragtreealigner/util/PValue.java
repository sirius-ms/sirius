
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: fhufsky
 * Date: 7/1/11
 * Time: 9:53 AM
 * To change this template use File | Settings | File Templates.
 */
public class PValue {

    public static void main(String[] args) {

        String filename = "ilp_lambda0.04";
        String dataset = "10x10compounds";
        int minSize=0;

        String directory ="normalized";
        if (minSize>0){
            directory+=">"+(minSize-1);
        }

        String path =  "/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/alignments/"+filename+".csv";
        String statsFilePath =  "/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/evdParamList1000";



        try {

            //reading alignment file

            BufferedReader file = FileUtils.ensureBuffering( new FileReader(path));

            //Score, p-Value, sizeTree1, sizeTree2, numberOfMatchedLosses


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
                    String[] values = entry.split("\\ ");

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



            //reading stats file

            BufferedReader statsFile = FileUtils.ensureBuffering(new FileReader(statsFilePath));

            //filename, loc, scale, pValue, length(values)


            Map<String, Double> locParamMap = new HashMap<String, Double>();
            Map<String, Double> scaleParamMap= new HashMap<String, Double>();

            for (line = statsFile.readLine(); line!=null; line=statsFile.readLine()){
                String[] split = line.split("\\ ");

                //System.out.println(split[0]);

                double loc = Double.parseDouble(split[1]);
                double scale = Double.parseDouble(split[2]);

                locParamMap.put(split[0],loc);
                scaleParamMap.put(split[0],scale);


            }


            List<Integer> toSmall = new ArrayList<Integer>();

            for (int i=0; i<number; i++){
                //System.out.println(sizes1[i][0]);
                if (sizes1[i][0]<=minSize){
                    toSmall.add(i);
                    System.out.println(i);
                }

            }

            // pValue computation

            float[][] minusLogPValues = new float[number][number];
            float[][] pValues = new float[number][number];



            for (int i=0; i<number; i++){
                for (int j=0; j<number; j++){
                    int min = Math.min(sizes1[i][j],sizes2[i][j]);
                    int max = Math.max(sizes1[i][j],sizes2[i][j]);


                    if (min>3){
                        Double location = locParamMap.get(min+"x"+max);
                        if (location== null){
                            minusLogPValues[i][j]= Float.NEGATIVE_INFINITY;
                            System.out.println("location null for "+names[i]+"("+sizes1[i][0]+") "+names[j]+"("+sizes1[j][0]+") ");

                        }else{
                            Double scale = scaleParamMap.get(min+"x"+max);

                            double result =  cumulativeGumbelComplement(scores[i][j],location,scale);

                            if (result==0){
                                System.err.println("Warning: log(0) used in p-Val calculation of "+names[i]+" "+names[j]);
                            }

                            pValues[i][j] = (float) result;
                            minusLogPValues[i][j]= (float) (-1*Math.log(result));
                        }
                    }




                }
            }

            BufferedWriter out = new BufferedWriter(new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/alignments/"+directory+"/"+filename+"minusLogPValues.csv"));

            for (int i=0; i<number; i++){
                if (!toSmall.contains(i)){
                    out.write(names[i].trim());
                    for (int j=0; j<number; j++){
                        if (!toSmall.contains(j))out.write(","+ minusLogPValues[i][j]);
                    }
                    out.write("\n");
                }

            }
            out.close();

//            BufferedWriter out2 = new BufferedWriter(new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/alignments/normalized/"+filename+"minusLogPValuesInfo.csv"));
//
//            for (int i=0; i<number; i++){
//                out2.write(names[i].trim());
//                if (i<number-1) out2.write(", ");
//                else out2.write("\n");
//            }
//
//            for (int i=0; i<number; i++){
//                out2.write(names[i].trim());
//                for (int j=0; j<number; j++){
//                    out2.write(", \""+ minusLogPValues[i][j]+" 0.0 "+sizes1[i][j]+" "+sizes2[i][j]+" "+losses[i][j]+"\"");
//                }
//                out2.write("\n");
//            }
//            out2.close();


            out = new BufferedWriter(new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/alignments/"+directory+"/"+filename+"PValues.csv"));

            for (int i=0; i<number; i++){
                if (!toSmall.contains(i)){
                    out.write(names[i].trim());

                    for (int j=0; j<number; j++){
                        if (!toSmall.contains(j))out.write(","+ pValues[i][j]);
                    }
                    out.write("\n");
                }
            }
            out.close();

//            out2 = new BufferedWriter(new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/alignments/normalized/"+filename+"PValuesInfo.csv"));
//
//            for (int i=0; i<number; i++){
//                out2.write(names[i].trim());
//                if (i<number-1) out2.write(", ");
//                else out2.write("\n");
//            }
//
//            for (int i=0; i<number; i++){
//                out2.write(names[i].trim());
//                for (int j=0; j<number; j++){
//                    out2.write(", \""+ pValues[i][j]+" 0.0 "+sizes1[i][j]+" "+sizes2[i][j]+" "+losses[i][j]+"\"");
//                }
//                out2.write("\n");
//            }
//            out2.close();






        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static double cumulativeGumbelComplement(double x, double loc, double scale){
        double exponent = (loc-x)/scale;
        return expComplement(-1*Math.exp(exponent));
    }

    private static double expComplement(double x){
        if (x < -2 || x > 10){
            return 1-Math.exp(x);
        }
        double res = 0;
        int factorial = 1;
        for (int i = 1; i <= 10; ++i){
            factorial *= i;
            res -= Math.pow(x,i)/factorial;
        }
        return res;
    }


}
