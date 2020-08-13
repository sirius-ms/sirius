/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.tree_viewer;

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.FutureTask;

/*
NOTE: first create new WebViewTreeViewer, then add all JS resources (addJS);
finally load() (only once!)
*/
public class WebViewTreeViewer extends JFXPanel implements TreeViewerBrowser{

    LinkedList<FutureTask<Void>> tasks = new LinkedList<>();

    public void queueTaskInJFXThread(Runnable runnable){
        FutureTask<Void> task = new FutureTask<>(runnable, null);
        tasks.add(task);
        Jobs.runJFXLater(task);
    }

    public void cancelTasks(){
        for (FutureTask<Void> task : tasks)
            task.cancel(true);
        tasks.clear();
    }

    public WebView webView;

    private StringBuilder html_builder;

    public WebViewTreeViewer(){
        this.html_builder = new StringBuilder("<html><head></head><body>\n");
        queueTaskInJFXThread(() -> {
                this.webView = new WebView();
                this.setScene(new Scene(this.webView));
    });
    }

    public boolean addJSCode(String scriptTag) {
        scriptTag = scriptTag.strip();
        if (scriptTag != null && scriptTag.startsWith("<script")) {
            this.html_builder.append(scriptTag);
            return true;
        } else {
            LoggerFactory.getLogger(getClass()).error("Not a valid script tag: " + scriptTag);
            return false;
        }
    }

    public void addJS(String resource_name) {
        String res_html;
        try{
            res_html = getJSResourceInHTML(WebViewTreeViewer.class.
                    getResourceAsStream("/js/"
                                        + resource_name));
        } catch (IOException e){
            e.printStackTrace();
            res_html = "";
        }
        this.html_builder.append(res_html);
    }

    public void load(){
        this.html_builder.append("</body></html>");
        queueTaskInJFXThread(() -> {
            this.webView.getEngine().setJavaScriptEnabled(true);
            this.webView.getEngine().loadContent(html_builder.toString(),
                                                 "text/html");
        });
        // TODO: notify the class when the loading is complete!
    }

    public void load(Map<String, Object> bridges){
        this.html_builder.append("</body></html>");
        queueTaskInJFXThread(() -> {
                this.webView.getEngine().setJavaScriptEnabled(true);
                this.webView.getEngine().loadContent(html_builder.toString(),
                                                     "text/html");
                // wait for the engine to finish loading
                webView.getEngine().getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
                            if (newState == Worker.State.SUCCEEDED) {
                                JSObject win = (JSObject) getJSObject("window");
                                for (Map.Entry<String, Object> entry : bridges.entrySet())
                                    win.setMember(entry.getKey(), entry.getValue());
                                executeJS("applySettings()");
                            }
                        });
        });
    }



    public void executeJS(String js_code) {
        queueTaskInJFXThread(() -> webView.getEngine().executeScript(js_code));
    }


    public void setJSArray(String name, Object[] newArray){
        JSObject array = (JSObject) getJSObject(name);
        int arrayLength = (int) array.getMember("length");
        for (int i = 0; i < newArray.length; i++) {
            array.setSlot(i, newArray[i]);
        }
        if (newArray.length < arrayLength)
            // slice doesn't work for some reason
            for (int i = 0; i < (arrayLength-newArray.length); i++)
                array.call("pop");
    }

    public Object getJSObject(String name){
        return webView.getEngine().executeScript(name);
    }

    public Object[] getJSArray(String name){
        // returns JSObject array as a *real* (JAVA) array
        JSObject jsArray = (JSObject) getJSObject(name);
        int jsArrayLength = (int) jsArray.getMember("length");
        Object[] array = new String[jsArrayLength];
        for (int i = 0; i < jsArrayLength; i++)
            array[i] = jsArray.getSlot(i);
        return array;
    }

    private String getJSResourceInHTML(InputStream stream) throws IOException{
        // Resource files have to be read in manually
        StringBuilder raw_script = new StringBuilder("<script>");
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        String line;
        while((line = br.readLine()) != null){
            raw_script.append(line + "\n");
        }
        br.close();
        raw_script.append("</script>");
        return raw_script.toString();
    }

    public void loadTree(String json_tree) {
        cancelTasks();
        executeJS("loadJSONTree('" + json_tree.replaceAll("(\\r\\n|\\r|\\n)", " ")
                + "')");
    }

    public void clear(){
        executeJS("clearSVG();");
    }
}
