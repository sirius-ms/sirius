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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.unijena.bioinf.ms.gui.configs.Colors;
import netscape.javascript.JSObject;
import org.slf4j.LoggerFactory;

public class TreeViewerBridge {

    static public final Map<String, String> COLOR_VARIANTS = new HashMap<String,
        String>() {{
            put("none", "none");
            put("mass deviation in m/z", "md_mz");
            put("mass deviation in mz (absolute)", "md_mz_abs");
            put("mass deviation in ppm", "md_ppm");
            put("mass deviation in ppm (absolute)", "md_ppm_abs");
            put("relative intensity", "rel_int");
        }};
    static public final String[] COLOR_VARIANTS_DESC = {
        "none", "mass deviation in m/z", "mass deviation in mz (absolute)",
        "mass deviation in ppm", "mass deviation in ppm (absolute)",
        "relative intensity" };
    static public final String[] COLOR_VARIANTS_IDS = {
        "none", "md_mz", "md_mz_abs", "md_ppm", "md_ppm_abs", "rel_int" };

    static public final String[] COLOR_VARIANTS_2 = {"md_mz_abs", "md_ppm_abs",
                                                     "rel_int" };
    static public final String[] COLOR_VARIANTS_3 = { "md_mz", "md_ppm" };

    static public final String[] COLOR_SCHEME_NAMES_2 = {"default" };
    static public final String[] COLOR_SCHEME_NAMES_3 = { "default" };

    static public final String[] COLOR_SCHEME_COLORS_2 = {"['"+Colors.asHex(Colors.FragementationTree.ONE_COLOR_GRADIENT_MIN)+"', '"+Colors.asHex(Colors.FragementationTree.ONE_COLOR_GRADIENT_MAX)+"']"};
    static public final String[] COLOR_SCHEME_COLORS_3 = {"['"+Colors.asHex(Colors.FragementationTree.TWO_COLOR_GRADIENT_LEFT)+"', '"+Colors.asHex(Colors.FragementationTree.TWO_COLOR_GRADIENT_MIDDLE)+"', '"+Colors.asHex(Colors.FragementationTree.TWO_COLOR_GRADIENT_RIGHT)+"']"};

    static public final String COLOR_SCHEME_NAME_2 = "default";
    static public final String COLOR_SCHEME_NAME_3 = "default";

    static public final String[] NODE_ANNOTATIONS = {
        "m/z", "mass deviation in m/z", "mass deviation in ppm",
        "relative intensity", "score" };
    static public final String[] NODE_ANNOTATIONS_IDS = {
        "mz", "massDeviationMz", "massDeviationPpm",
        "relativeIntensity", "score", };

    public static final int TREE_SCALE_MIN = 25;
    public static final int TREE_SCALE_MAX = 200;
    public static final int TREE_SCALE_INIT = 100;

    TreeViewerBrowser browser;

    public String color_scheme_2_selected;
    public String color_scheme_3_selected;

    public TreeViewerBridge(TreeViewerBrowser browser) {
        this.browser = browser;
        this.color_scheme_2_selected = COLOR_SCHEME_NAME_2;
        this.color_scheme_3_selected = COLOR_SCHEME_NAME_3;
    }

    public String functionString(String function, String... args) {
        return function + "(" + String.join(", ", args) + ");";
    }

    public void updateConfig(TreeConfig config){
        JSObject win = (JSObject) browser.getJSObject("window");
        win.setMember("config", config);
    }

    public void settingsChanged(){
        browser.executeJS("settingsChanged()");
    }

    public void scaleTree(float mag) {
        browser.executeJS(functionString("scaleTree", String.valueOf(mag)));
    }

    public float getTreeScale() {
        // NOTE: this value is 1/tree_scale!
        return tryCastNumber(browser.getJSObject("tree_scale"),1);
    };

    public float getTreeScaleMin() {
        return tryCastNumber(browser.getJSObject("tree_scale_min"),.5f);
    }

    private float tryCastNumber(Object number, float defValue){
        try {
            if (number instanceof Number){
                return ((Number) number).floatValue();
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error when casting to Number. Returning default: " + defValue, e);
        }
        return defValue;
    }

    public void resetTree() {
        browser.executeJS("reset();");
    }

    public void resetZoom(){
        browser.executeJS("resetZoom();");
    }

    public String getSVG() {
        // NOTE: PDF export does not support the style 'text-anchor'
        // with this first function coordinates for all texts with this
        // style will be recalculated to the same effect as using the style
        browser.executeJS("realignAllText()");
        String svg = (String) browser.getJSObject("getSVGString()");
        browser.executeJS("drawTree()");
        return svg;
    }

    public String getJSONTree(){
        return (String) browser.getJSObject("getJSONTree()");
    }

    public static String get2ColorPaletteByNameOrDefault(String colorName) {
        for (int i = 0; i < COLOR_SCHEME_NAMES_2.length; i++) {
            if (colorName.equalsIgnoreCase(COLOR_SCHEME_NAMES_2[i])) return COLOR_SCHEME_COLORS_2[i];
        }
        return COLOR_SCHEME_COLORS_2[0];
    }

    public static String get3ColorPaletteByNameOrDefault(String colorName) {
        for (int i = 0; i < COLOR_SCHEME_NAMES_3.length; i++) {
            if (colorName.equalsIgnoreCase(COLOR_SCHEME_NAMES_3[i])) return COLOR_SCHEME_COLORS_3[i];
        }
        return COLOR_SCHEME_COLORS_3[0];
    }

    // capabilities below are handled by TreeConfig now
    @Deprecated
    public void colorCode(String color_variant) {
        String color_scheme;
        String variant_id = COLOR_VARIANTS.get(color_variant);
        if (Arrays.asList(COLOR_VARIANTS_2).contains(variant_id))
            color_scheme = color_scheme_2_selected;
        else
            color_scheme = color_scheme_3_selected;
        String function_string = functionString("colorCode", "'" + variant_id + "'", "'" + color_scheme + "'");
        browser.executeJS(function_string);
    }

    @Deprecated
    public void setColorBarVis(boolean visible) {
        browser.executeJS(functionString("toggleColorBar", visible ? "true" : "false"));
    }

    @Deprecated
    public void setNodeAnnotations(List<String> nodeAnnotations) {
        int nodeLabelIndex = -1;
        for (int i = 0; i < nodeAnnotations.size(); i++)
            if (nodeAnnotations.get(i).equals("molecular formula"))
                nodeLabelIndex = i;
        browser.executeJS("toggleNodeLabels(" + ((nodeLabelIndex != -1) ? "true" : "false") + ")");
        if (nodeLabelIndex != -1)
            nodeAnnotations.remove(nodeLabelIndex);
        browser.setJSArray("annot_fields", nodeAnnotations.toArray(new String[0]));
        browser.executeJS("updateBoxHeight()");

    }

    @Deprecated
    public String[] getNodeAnnotations() {
        return (String[]) browser.getJSArray("annot_fields");
    }

    @Deprecated
    public void setEdgeLabel(boolean enabled) {
        browser.executeJS(functionString("showEdgeLabels", enabled ? "true" : "false"));
    }
}
