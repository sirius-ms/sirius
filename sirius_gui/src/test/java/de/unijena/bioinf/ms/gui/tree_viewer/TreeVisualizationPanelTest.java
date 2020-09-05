package de.unijena.bioinf.ms.gui.tree_viewer;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.SwingJobManager;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.properties.PropertyManager;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;


public class TreeVisualizationPanelTest{
    public static void main(String[] args) throws InterruptedException {
        SiriusJobs.setGlobalJobManager(new SwingJobManager(PropertyManager.getNumberOfThreads(), 1));

        JFrame frame = new JFrame("TreeViewer Test");
        WebViewTreeViewer browser = new WebViewTreeViewer();
//        System.out.println("FIREBUG: " + browser.addJSCode("<script type='text/javascript' src='https://cdnjs.cloudflare.com/ajax/libs/firebug-lite/1.4.0/firebug-lite.js'></script>"));
        browser.addJS("d3.min.js");
        browser.addJS("d3-colorbar.js");
        browser.addJS("tree_viewer/treeViewer.js");
        // browser.addJS("tree_viewer/treeViewerSettings.js");
        browser.addJS("tree_viewer/treeViewerConnector.js");
        frame.add(browser);
        HashMap<String, Object> bridges = new HashMap<String, Object>() {{
                put("config", new TreeConfig());
                put("connector", new TreeViewerConnector());
            }};
        browser.load(bridges);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(1200, 1400);
        frame.setVisible(true);
        Thread.sleep(2000);
        try{
            InputStream data_json_stream = WebViewTreeViewer.class.
                getResourceAsStream("/example_tree.json");
            StringBuilder data_json_builder = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(data_json_stream));
            String line;
            while((line = br.readLine()) != null){
                data_json_builder.append(line + "\n");
            }
            br.close();
            String data_json = data_json_builder.toString();
            Jobs.runJFXLater(() -> browser.loadTree(data_json));
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
