
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
