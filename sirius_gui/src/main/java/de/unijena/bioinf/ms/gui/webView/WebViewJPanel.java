/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.webView;

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.concurrent.FutureTask;

public class WebViewJPanel extends JFXPanel {
    public WebView webView;

    public WebViewJPanel() {
        this("/js/styles.css", "/js/styles-dark.css");
    }
    public WebViewJPanel(@NotNull String cssLightResource, @NotNull String cssDarkResource) {
        queueTaskInJFXThread(() -> {
            this.webView = new WebView();
            webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.equals(Worker.State.SUCCEEDED)) {
                    Document document = webView.getEngine().getDocument();
                    NodeList nodeList = document.getElementsByTagName("a");
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Node node = nodeList.item(i);
                        EventTarget eventTarget = (EventTarget) node;
                        eventTarget.addEventListener("click", new EventListener() {
                            @Override
                            public void handleEvent(Event evt) {
                                EventTarget target = evt.getCurrentTarget();
                                HTMLAnchorElement anchorElement = (HTMLAnchorElement) target;
                                final String href = anchorElement.getHref();
                                //handle opening URL outside JavaFX WebView
                                evt.preventDefault();
                                Jobs.runInBackground(() -> {
                                    try {
                                        // open external links in system browser
                                        GuiUtils.openURLInSystemBrowser(new URI(href));
                                    } catch (URISyntaxException | IOException e) {
                                        LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
                                    }
                                });
                            }
                        }, false);
                    }
                }
            });
            this.webView.getEngine().setUserStyleSheetLocation(
                    WebViewUtils.textToDataURL(WebViewUtils.loadCSSAndSetColorThemeAndFont(
                            Colors.isDarkTheme() ? cssDarkResource : cssLightResource))
            );

            this.setScene(new Scene(this.webView));
        });
    }

    public void load(String html) {
        queueTaskInJFXThread(() -> this.webView.getEngine().loadContent(html, "text/html"));
        // TODO: notify the class when the loading is complete!
    }

    LinkedList<FutureTask<Void>> tasks = new LinkedList<>();

    public void queueTaskInJFXThread(Runnable runnable) {
        FutureTask<Void> task = new FutureTask<>(runnable, null);
        tasks.add(task);
        Jobs.runJFXLater(task);
    }

    public void cancelTasks() {
        for (FutureTask<Void> task : tasks)
            task.cancel(false);
        tasks.clear();
    }
}
