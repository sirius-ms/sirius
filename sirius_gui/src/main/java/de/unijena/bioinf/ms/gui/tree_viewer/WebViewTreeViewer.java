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

package de.unijena.bioinf.ms.gui.tree_viewer;

import de.unijena.bioinf.ftalign.CommonLossScoring;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.webView.WebViewPanel;

import java.util.Arrays;

/*
NOTE: first create new WebViewTreeViewer, then add all JS resources (addJS);
finally load() (only once!)
*/
public class WebViewTreeViewer extends WebViewPanel implements TreeViewerBrowser{

    protected final String COMMON_LOSS_VAR;

    public WebViewTreeViewer() {
        super();
        StringBuilder buf = new StringBuilder("var common_losses = [");
        Arrays.stream(CommonLossScoring.LOSSES).limit(CommonLossScoring.LOSSES.length - 1).forEach(l -> buf.append("\"").append(l).append("\", "));
        buf.append("\"").append(CommonLossScoring.LOSSES[CommonLossScoring.LOSSES.length - 1]).append("\"];");
        COMMON_LOSS_VAR = buf.toString();
    }

    public void loadTree(String json_tree) {
        cancelTasks();
        // START HACK: TreeViewerConnector does not seem to work! set common losses and theme as JS variables!
        executeJS(COMMON_LOSS_VAR);
        if (Colors.isDarkTheme()) {
            executeJS("var theme = 'elegant-dark';");
        }
        // END HACK
        executeJS("loadJSONTree('" + json_tree.replaceAll("(\\r\\n|\\r|\\n)", " ")
                + "')");
    }

    public void clear(){
        executeJS("clearSVG();");
    }
}
