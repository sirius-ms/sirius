
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
 * Date: 8/2/11
 * Time: 1:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class ParseToAlignmentTree {

    public static void main(String[] args) {


        File alignmentFile   =   new File("/Users/fhufsky/Uni/Projects/TreeAlignment/scripts/output/alignment.dot");



        try {
            BufferedReader reader = FileUtils.ensureBuffering(new FileReader(alignmentFile));

            Map<String,String> nodesTree1 = new HashMap<String, String>();
            Map<String,String> nodesTree2 = new HashMap<String, String>();

            Map<String,String> edgesTree1 = new HashMap<String, String>();
            Map<String,String> edgesTree2 = new HashMap<String, String>();



            for (String line = reader.readLine(); line!=null; line=reader.readLine()){
                if (line.contains("sg0")){ //tree1

                    while (!line.contains("}")){

                        if (line.contains("[")){
                            String label = line.split("=")[1].split("\"")[1];
                            //System.out.println(label);
                            String name = line.split(" ")[0];
                            if (label.contains("[")){

                                String node = label.split("\\[")[1].split(" ")[0];
                                System.out.println(node);
                                String edge = label.split(" ")[1].split("\\]")[0];
                                System.out.println(edge);

                                nodesTree1.put(name,node);
                                edgesTree1.put(name,edge);


                            }


                        }

                        line =reader.readLine();
                    }

                }



            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }
}
