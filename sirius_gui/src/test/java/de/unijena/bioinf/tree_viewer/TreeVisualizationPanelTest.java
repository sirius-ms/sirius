package de.unijena.bioinf.tree_viewer;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javafx.concurrent.Worker;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;


public class TreeVisualizationPanelTest{
    public static void main(String[] args) throws InterruptedException {
        JFrame frame = new JFrame("TreeViewer Test");
        WebViewTreeViewer browser = new WebViewTreeViewer();
        browser.addJS("d3.min.js");
        browser.addJS("d3-colorbar.js");
        browser.addJS("treeViewer.js");
        browser.addJS("treeViewerSettings.js");
        frame.add((JFXPanel) browser);
        browser.load();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(1200, 1400);
        frame.setVisible(true);
        Thread.sleep(2000);
        try{
            InputStream data_json_stream = WebViewTreeViewer.class.
                getResourceAsStream("/de/unijena/bioinf/js/" + "example_tree.json");
            StringBuilder data_json_builder = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(data_json_stream));
            String line;
            while((line = br.readLine()) != null){
                data_json_builder.append(line + "\n");
            }
            br.close();
            String data_json = data_json_builder.toString();
            Platform.runLater(() -> {
                    browser.loadTree(data_json);
                });
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
