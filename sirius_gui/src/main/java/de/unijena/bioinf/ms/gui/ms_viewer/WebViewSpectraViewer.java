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

package de.unijena.bioinf.ms.gui.ms_viewer;

import java.util.HashMap;
import javafx.concurrent.Worker;
import de.unijena.bioinf.ms.gui.webView.WebViewPanel;
import netscape.javascript.JSObject;

public class WebViewSpectraViewer extends WebViewPanel {

    HashMap<String, Object> bridges;

    public WebViewSpectraViewer() {
        super();
        addJS("d3.min.js");
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
                            webView.getEngine().executeScript("var main = new Main();");
                        }
                    });
            });
    }

    public void loadData(String json_spectra, String json_highlight, String svg) { // TEST CODE
        cancelTasks();

        queueTaskInJFXThread(() -> {
                // set data
                JSObject obj = (JSObject) webView.getEngine().executeScript("document.webview = { \"spectrum\": " + json_spectra + ", \"highlight\": " + escapeNull(json_highlight) + ", svg: null};");
                if (svg!=null) {
                    obj.setMember("svg", svg);
                }
                // load Data
                webView.getEngine().executeScript(
                    "main.loadJSONData(document.webview.spectrum, document.webview.highlight, document.webview.svg)");
            });
    }

    private String jsonString(String val) {
        if (val==null) return "null";
        else return "\""+val+"\"";
    }

    private String escapeNull(String val) {
        if (val==null) return "null";
        else return val;
    }

	public void clear(){
        executeJS("main.clear()");
	}

    public SpectraViewerConnector getConnector(){
        return (SpectraViewerConnector) bridges.get("connector");
    }

}
