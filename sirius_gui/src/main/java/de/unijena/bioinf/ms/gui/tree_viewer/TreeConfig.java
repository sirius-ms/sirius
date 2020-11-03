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

import java.util.HashMap;
import java.util.Map;

/*
stores and allows access to settings for the treeViewer
*/
public class TreeConfig{

    public static final String[] SETTINGS = {
        "colorVariant", "colorScheme2", "colorScheme3", "colorBar",
        "nodeAnnotations", "popupAnnotations", "edgeLabels", "nodeLabels",
        "edgeLabelMode", "lossColors", "deviationColors",
        "centeredNodeLabels",    // not configurable in GUI for now
        "editMode"               // not configurable in GUI for now
    };

    private Map<String, Object> map;

    public TreeConfig(){
        map = new HashMap<>();
    }

    public TreeConfig(Map<String, Object> settings){
        map = settings;
    }

    public Object get(String setting){
        return map.get(setting);
    }

    public void set(String setting, Object value){
        map.put(setting, value);
    }

    public String getAsString(String setting){
        Object value = map.get(setting);
        if (value instanceof String[]){
            return String.join(",", (String[]) value);
        } else {
            return value.toString();
        }
    }

    public void setFromString(String setting, String value){
        switch (setting){
        case "colorBar":
        case "edgeLabels":
        case "nodeLabels":
        case "lossColors":
        case "deviationColors":
        case "centeredNodeLabels":
        case "editMode":
            map.put(setting, Boolean.valueOf(value));
            break;
        case "nodeAnnotations":
        case "popupAnnotations":
        case "presets":
            map.put(setting, value.split(","));
            break;
        default:
            map.put(setting, value);
        }
    }

    public String[] getSettings(){
        // JS cannot access SETTINGS otherwise
        return SETTINGS;
    }
}
