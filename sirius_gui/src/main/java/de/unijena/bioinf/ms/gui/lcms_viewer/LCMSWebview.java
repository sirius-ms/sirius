package de.unijena.bioinf.ms.gui.lcms_viewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ms.gui.utils.FxTaskList;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class LCMSWebview extends JFXPanel {

    private LCMSPeakInformation lcmsPeakInformation;
    private int activeId;

    private FxTaskList taskList;
    private List<Consumer<JSObject>> delayAfterHTMLLoading;

    private WebView webView;
    private ReentrantLock lock;

    private JSObject lcmsViewer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public LCMSWebview() {
        this.taskList = new FxTaskList();
        this.lock = new ReentrantLock();
        this.delayAfterHTMLLoading = new ArrayList<>();
        taskList.runJFXLater(()-> {
            this.webView = new WebView();
//            System.out.println("A");
            setScene(new Scene(webView));
//            System.out.println("B");
            final String htmlContent = getHTMLContent();
//            System.out.println("C");
            webView.getEngine().setJavaScriptEnabled(true);
//            System.out.println("D");
            webView.getEngine().loadContent(htmlContent,"text/html");
//            System.out.println("E");
            webView.getEngine().getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
//                System.out.println(oldState + " -> " + newState);
                if (newState == Worker.State.SUCCEEDED) {
                    lock.lock();
                    ((JSObject)webView.getEngine().executeScript("window")).setMember("console", new Console());
                    this.lcmsViewer = (JSObject)webView.getEngine().executeScript(
                            "document.lcmsViewer = new LCMSViewer(\"#lcms\", \"#lcms_view\");"
                    );
                    delayAfterHTMLLoading.forEach(x->x.accept(this.lcmsViewer));
                    delayAfterHTMLLoading.clear();
                    lock.unlock();
                }
            });
        });
    }

    public void setInstance(LCMSPeakInformation peakInformation) {
        lcmsView(f->{
            try {
                final String json = objectMapper.writeValueAsString(peakInformation);
//                System.err.println(json);
                f.call("bindStringData", json);
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    public void lcmsView(Consumer<JSObject> functor) {
        if (lcmsViewer != null) {
            runForLcms(functor);
        } else {
            lock.lock();
            if (lcmsViewer!=null) {
                runForLcms(functor);
            } else delayAfterHTMLLoading.add(functor);
            lock.unlock();
        }
    }

    private void runForLcms(Consumer<JSObject> functor) {
        taskList.runJFXLater(()->functor.accept(lcmsViewer));
    }

    private String getHTMLContent() {
        return "<html><head><script>" + loadJs() +
                "</script><body><div id=\"lcms\"><svg id=\"lcms_view\"></svg></div></body></html>";
    }

    private String loadJs() {
        try {
            return Resources.toString(LCMSWebview.class.getResource(
                    "/js/lcms_viewer/d3.min.js"
            ), Charsets.UTF_8) + "\n" + Resources.toString(LCMSWebview.class.getResource(
                    "/js/lcms_viewer/lcms_viewer.js"
            ), Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSampleIndex(int activeIndex) {
        lcmsView(x->x.call("setSample", activeIndex));
    }

    private static class Console {

        public void log(String msg) {
            System.err.println(msg);
        }
        public void log(JSObject msg) {
            System.err.println(msg.toString());
        }

    }

}
