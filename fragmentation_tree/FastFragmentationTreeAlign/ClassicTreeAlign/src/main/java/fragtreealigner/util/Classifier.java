
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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: fhufsky
 * Date: 7/7/11
 * Time: 3:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class Classifier {

    public static void main(String[] args) {

        String structural ="";
        boolean structure =true;

        if (structure)structural="peakCounting_";

        String dataset = "massbank";

        String classfile =  "/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/classes.csv";

        String directoryPath = "/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/"+structural+"alignments/normalized>2/";




        File dir = new File(directoryPath);

        File[] files = dir.listFiles();


        try {
            BufferedReader file = FileUtils.ensureBuffering( new FileReader(classfile));


            Map<String,String> classes = new HashMap<String,String>();


            for (String line = file.readLine(); line!=null; line=file.readLine()){
                String[] split = line.split("\\,");
                classes.put(split[0],split[1]);
                System.out.println(split[0]+"\t"+split[1]);

            }

//                for (Map.Entry e : classes.entrySet()){
//                    System.out.println(e.getKey()+"\t"+e.getValue());
//
//                }
            for (File f : files){
                if (f!=null && f.getName().contains(".csv")){

                    String filename = f.getName();
                    System.out.println(filename);

                    file = FileUtils.ensureBuffering( new FileReader(f));

                    String line = file.readLine();
                    String[] split = line.split("\\,");
                    int number = split.length-1;
                    String[] names = new String[number];

                    double[][] scores = new double[number][number];

                    int linecounter=0;

                    while (line!=null){
                        split = line.split("\\,");


                        String name = split[0].replaceAll("\"","");
                        name = name.replaceAll("\\.dot","");
                        //System.out.println(name);

                        String compoundClass = classes.get(name);

                        String newName = compoundClass+"_"+split[0];
                        names[linecounter]=newName;

                        for (int i=1; i<number+1; i++){
                            scores[linecounter][i-1] = Double.parseDouble(split[i]);

                        }



                        line=file.readLine();
                        linecounter++;
                    }


                    BufferedWriter out = new BufferedWriter(new FileWriter("/Users/fhufsky/Uni/Projects/TreeAlignment/Data/"+dataset+"/"+structural+"alignments/normalized>2/Class"+filename));

                    for (int i=0; i<number; i++){

                        out.write(names[i].trim());
                        for (int j=0; j<number; j++){
                            out.write(","+ scores[i][j]);
                        }
                        out.write("\n");
                    }
                    out.close();




















                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }












    }
}
