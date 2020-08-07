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

import java.util.Map;

/*
Interface for classes displaying the TreeViewer Javascript file (WebView implementation vs JXBrowser implementation).
Allows communication with JavaScript.
 */
public interface TreeViewerBrowser  {
    void addJS(String resource_url);
    void load();
    void load(Map<String, Object> bridges);
    void loadTree(String jsonTree);
    void cancelTasks();
    void clear();
    void executeJS(String js_code);
    Object getJSObject(String name);
    Object[] getJSArray(String name);
    void setJSArray(String name, Object[] newArray);
}
