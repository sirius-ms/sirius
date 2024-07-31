package de.unijena.bioinf.ms.gui.lcms_viewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.utils.FxTaskList;
import de.unijena.bioinf.ms.nightsky.sdk.model.TraceSet;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import lombok.SneakyThrows;
import netscape.javascript.JSObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class LCMSWebview extends JFXPanel {

    private FxTaskList taskList;
    private List<Consumer<JSObject>> delayAfterHTMLLoading;

    private WebView webView;
    private ReentrantLock lock;

    private JSObject lcmsViewer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Console console = new Console();

    public LCMSWebview() {
        this.taskList = new FxTaskList();
        this.lock = new ReentrantLock();
        this.delayAfterHTMLLoading = new ArrayList<>();
        taskList.runJFXLater(()-> {
            this.webView = new WebView();
            final Properties props = SiriusProperties.SIRIUS_PROPERTIES_FILE().asProperties();
            final String theme = props.getProperty("de.unijena.bioinf.sirius.ui.theme", "Light");
            boolean DarkMode = theme.equals("Dark");
            if (!DarkMode) {
                this.webView.getEngine().setUserStyleSheetLocation(
                        getClass().getResource("/js/" + "styles.css").toExternalForm());
            } else {
                this.webView.getEngine().setUserStyleSheetLocation(
                        getClass().getResource("/js/" + "styles-dark.css").toExternalForm());
            }
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
                    webView.getEngine().executeScript(loadJs());
                    lock.lock();
                    ((JSObject)webView.getEngine().executeScript("window")).setMember("console", console);
                    if (!DarkMode) {
                        this.webView.getEngine().executeScript("document.setBright()");
                    } else {
                        this.webView.getEngine().executeScript("document.setDark()");
                    }
                    this.lcmsViewer = (JSObject)webView.getEngine().executeScript("document.drawPlot('#lc-plot')");
                    delayAfterHTMLLoading.forEach(x->x.accept(this.lcmsViewer));
                    delayAfterHTMLLoading.clear();
                    lock.unlock();
                    System.out.println(lcmsViewer);
                }
            });
        });
    }

    public void setInstance(TraceSet peakInformation, LCMSViewerPanel.Order order, LCMSViewerPanel.ViewType viewType, String featureId) {
        lcmsView(f->{
            try {
                final String json = objectMapper.writeValueAsString(peakInformation);
                f.call("setOrder", order.name());
                if (viewType== LCMSViewerPanel.ViewType.ALIGNMENT) {
                    f.call("loadString", json);
                } else {
                    f.call("loadStringForCompound", json, Long.parseLong(featureId));
                }
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

    @SneakyThrows
    private String getHTMLContent() {
        return Files.readString(Paths.get(LCMSWebview.class.getResource("/js/lcms_viewer/index.html").toURI()));
    }

    @SneakyThrows
    private String loadJs() {
        return Files.readString(Paths.get(LCMSWebview.class.getResource("/js/lcms_viewer/d3.v7.min.js").toURI()))
                + "\n" +
                Files.readString(Paths.get(LCMSWebview.class.getResource("/js/lcms_viewer/lcms.js").toURI()));
    }

    public void reset() {
        lcmsView(f -> {
            try {
                f.call("clear");
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    public static class Console {

        public void log(Object msg) {
            System.err.println(msg);
        }

        public void log(String msg) {
            System.err.println(msg);
        }

        public void log(JSObject msg) {
            System.err.println(msg);
        }

    }

}
