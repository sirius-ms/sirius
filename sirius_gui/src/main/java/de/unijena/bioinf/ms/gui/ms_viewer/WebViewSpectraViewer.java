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

import de.unijena.bioinf.ms.gui.webView.WebViewPanel;
import netscape.javascript.JSObject;

public class WebViewSpectraViewer extends WebViewPanel {

    public void loadData(String json_spectra, String json_highlight, String svg) { // TEST CODE
        cancelTasks();

        queueTaskInJFXThread(() -> {
                    JSObject obj = (JSObject) webView.getEngine().executeScript("document.webview = { \"spectrum\": " + json_spectra + ", \"highlight\": " + escapeNull(json_highlight) + ", svg: null};");
                    System.out.println("document.webview = { \"spectrum\": " + json_spectra + ", \"highlight\": " + escapeNull(json_highlight) + ", \"svg\": null};");
                    if (svg!=null) {
                        obj.setMember("svg", svg);
                    }
                    webView.getEngine().executeScript("loadJSONData(document.webview.spectrum, document.webview.highlight, document.webview.svg)");
        }
        );
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
		executeJS("clear()");
	}
}
