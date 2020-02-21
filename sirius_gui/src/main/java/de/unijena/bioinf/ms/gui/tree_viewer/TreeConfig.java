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
            System.out.println(setting + " is not a String array but a "
                               + value.getClass());
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
