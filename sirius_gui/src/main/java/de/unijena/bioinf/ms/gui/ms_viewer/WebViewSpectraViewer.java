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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.ms_viewer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.webView.WebViewPanel;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Properties;

public class WebViewSpectraViewer extends WebViewPanel {

    HashMap<String, Object> bridges;

    public WebViewSpectraViewer() {
        super();
        addJS("d3.min.js");
        addJS("svg-export.js");
        addJS("spectra_viewer/spectra_viewer_oop.js");
        SpectraViewerConnector svc = new SpectraViewerConnector();
        bridges = new HashMap<String, Object>() {{
            put("connector", svc);
        }};
        load(bridges);
        // create Main instance
        queueTaskInJFXThread(() -> {
            // after bridges are queued to load, we have to wait (again)
            webView.getEngine().getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    final Properties props = SiriusProperties.SIRIUS_PROPERTIES_FILE().asProperties();
                    final String theme = props.getProperty("de.unijena.bioinf.sirius.ui.theme", "Light");
                    if (!theme.equals("Dark")) {
                        webView.getEngine().executeScript("var fg_color = 'black';");
                    } else {
                        webView.getEngine().executeScript("var fg_color = '#bbb';");
                    }
                    webView.getEngine().executeScript("var main = new Main();");
                }
            });
        });
    }

    public void loadDataOrThrow(@NotNull SpectraViewContainer data,  @Nullable String svg,  @Nullable String mirrorViewMode,  @Nullable Integer showMzTopK) {
        try {
            loadData(data, svg, mirrorViewMode, showMzTopK);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadData(@NotNull SpectraViewContainer data, @Nullable String svg, @Nullable String mirrorViewMode, @Nullable Integer showMzTopK) throws JsonProcessingException { //
        loadData(new ObjectMapper().writeValueAsString(data), svg, mirrorViewMode, showMzTopK);
    }

    public void loadData(@NotNull String json_spectra, @Nullable String svg, @Nullable String mirrorViewMode, @Nullable Integer showMzTopK) { // TEST CODE
        cancelTasks();

        queueTaskInJFXThread(() -> {
            // set data
            JSObject obj = (JSObject) webView.getEngine().executeScript("document.webview = { "
                    + "\"spectrum\": " + json_spectra
                    + ", \"svg\": null"
                    + ", \"mirrorStyle\": null"
                    + ", \"showMz\": null"
                    + "};");
            if (mirrorViewMode != null)
                obj.setMember("mirrorStyle", mirrorViewMode);
            if (showMzTopK != null)
                obj.setMember("showMz", showMzTopK);
            if (svg != null)
                obj.setMember("svg", svg);

            // load Data
            webView.getEngine().executeScript(
                    "main.loadJSONDataAndStructure(document.webview.spectrum, document.webview.svg, document.webview.mirrorStyle, document.webview.showMz)");
        });
    }

    private String jsonString(String val) {
        if (val == null) return "null";
        else return "\"" + val + "\"";
    }

    private String escapeNull(String val) {
        if (val == null) return "null";
        else return val;
    }

    public void clear() {
        executeJS("main.clear()");
    }

    public SpectraViewerConnector getConnector() {
        return (SpectraViewerConnector) bridges.get("connector");
    }

}
