package de.unijena.bioinf.ms.gui.tree_viewer;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

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

    public void queueTask(Runnable runnable){
        FutureTask<Void> task = new FutureTask<>(runnable, null);
        tasks.add(task);
        Platform.runLater(task);
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
        queueTask(() -> {
                this.webView = new WebView();
                this.setScene(new Scene(this.webView));
    });
    }

    public void addJS(String resource_name){
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
        queueTask(() -> {
            this.webView.getEngine().setJavaScriptEnabled(true);
            this.webView.getEngine().loadContent(html_builder.toString(),
                                                 "text/html");
        });
        // TODO: notify the class when the loading is complete!
    }

    public void load(Map<String, Object> bridges){
        this.html_builder.append("</body></html>");
        queueTask(() -> {
                this.webView.getEngine().setJavaScriptEnabled(true);
                this.webView.getEngine().loadContent(html_builder.toString(),
                                                     "text/html");
                // wait for the engine to finish loading
                webView.getEngine().getLoadWorker().stateProperty().addListener(
                    new ChangeListener<Worker.State>() {
                        public void changed(ObservableValue ov,
                                            Worker.State oldState,
                                            Worker.State newState) {
                            if (newState == Worker.State.SUCCEEDED) {
                                JSObject win = (JSObject) getJSObject("window");
                                for (Map.Entry<String, Object> entry :
                                        bridges.entrySet())
                                    win.setMember(entry.getKey(),
                                            entry.getValue());
                                executeJS("applySettings()");
                            }
                        }
                    });
        });
    }

    public void loadTree(String json_tree) {
        cancelTasks();
            executeJS("loadJSONTree('" + json_tree.replace("\n", " ")
                      + "')");
    }

    public void clear(){
        executeJS("clearSVG();");
    }

    public void executeJS(String js_code) {
        queueTask(() -> {
            webView.getEngine().executeScript(js_code);});
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
}
