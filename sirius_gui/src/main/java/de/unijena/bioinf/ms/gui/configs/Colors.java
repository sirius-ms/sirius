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

package de.unijena.bioinf.ms.gui.configs;

import de.unijena.bioinf.ms.properties.PropertyManager;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;

/**
 * @author Markus Fleischauer
 * @author Marcus Ludwig
 */
@Slf4j
public class Colors {
    public enum Theme {LIGHT, DARK}

    private final static Theme THEME = PropertyManager.getEnum("de.unijena.bioinf.sirius.ui.theme", Theme.LIGHT);

    public static Theme THEME() {
        return THEME;
    }

    public static boolean isDarkTheme() {
        return THEME() == Theme.DARK;
    }

    public static boolean isLightTheme() {
        return THEME() == Theme.LIGHT;
    }

    //--------------------------------------------


    private final static Color CUSTOM_BLUE = Color.decode("#4da6bf");
    private final static Color CUSTOM_LIGHT_BLUE = Color.decode("#b3d9e5");

    private final static Color CUSTOM_GREEN = Color.decode("#68da58"); // quality icon green
    private final static Color CUSTOM_YELLOW = Color.decode("#feff66"); // quality icon yellow
    private final static Color CUSTOM_ORANGE = Color.decode("#ffc14c");
    private final static Color CUSTOM_PINK = Color.decode("#f570a1"); // quality icon pink



//------------------

    public final static Color GOOD_IS_GREEN_PALE = Color.decode("#b4edab");
    public final static Color GOOD_IS_GREEN_VIBRANT = Color.decode("#8ee481");


    public static class Themes {
        public static class Light {
            public final static Color FOREGROUND = Color.BLACK;
            public final static Color BACKGROUND = Color.WHITE;

            public final static Color SWITCH_FOREGROUND = new Color(210, 210, 210);
            public final static Color SWITCH_BACKGROUND = Color.WHITE; //this is (new FlatIntelliJLaf()).getDefaults().getColor("Button.background");

            public final static Color WEBVIEW_TEXT_IN_POPUPS_BACKGROUND = new Color(242, 242, 242);//(new FlatIntelliJLaf()).getDefaults().getColor("Panel.background");

            public static class LargerCells {
                public final static Color ALTERNATING_CELL_1 = Color.WHITE;
                public final static Color ALTERNATING_CELL_2 = Color.decode("#e6e6e6");
                public final static Color CELL_TEXT = Color.BLACK; //(new FlatIntelliJLaf()).getDefaults().getColor("List.foreground");

                public final static Color SELECTED_CELL = CUSTOM_LIGHT_BLUE;
            }

            public static class Tables {
                public final static Color ALTERNATING_ROW_1 = Color.WHITE;
                public final static Color ALTERNATING_ROW_2 = Color.decode("#f2f2f2");
                public final static Color SELECTED_ROW = CUSTOM_LIGHT_BLUE;
            }
            public final static Color BEST_HIT_MAIN = GOOD_IS_GREEN_PALE;
            public final static Color BEST_HIT_WITH_TRANSPARENCY = withTransparency(BEST_HIT_MAIN, 0.7);
            public final static Color BEST_HIT_ALTERNATING_CELL_1 = Color.decode("#caf2c4"); // same as 70% transparency for main color on white table cell color
            public final static Color BEST_HIT_ALTERNATING_CELL_2 = Color.decode("#c3ebbd"); // same as 70% transparency for main color on gray table cell color
            public final static Color BEST_HIT = BEST_HIT_ALTERNATING_CELL_1;
            public final static Color BEST_HIT_SELECTED = GOOD_IS_GREEN_VIBRANT;

            public final static Color ALTERNATING_CELL_ROW_TEXT_COLOR = Color.decode("#000000");

            public static class Spectrum {
                public final static Color SELECTED_PEAK_BACKGROUND_BOX = Color.decode("#f2f2f2");
                public final static Color SIMULATED_ISOTOPE_PATTTEN_PEAK = Color.decode("#000000");
            }

            public static class CompoundClassesView {
                public final static Color CLASSIFIER_MAIN = GOOD_IS_GREEN_PALE;
                public final static Color CLASSIFIER_OTHER = CUSTOM_LIGHT_BLUE;
                public final static Color CLASSIFIER_TEXT = Color.BLACK;
            }

            public static class MolecularStructures {
                public final static Color SELECTED_SUBSTRUCTURE = Color.decode("#000000");
                public final static Color SELECTED_SUBSTRUCTURE_WITH_GLOW_HIGHLIGHT = SELECTED_SUBSTRUCTURE;
                public final static Color BACKGROUND_STRUCTURE = Color.decode("#d9d9d9");
                public final static Color BREAKING_BOND = Color.decode("#f570a1"); //be aware that grey/pink contrast is not good with green deficiency
            }

            public static class LCMSVIEW {
                //for adduct/isotope view
                public final static Color SELECTED_FEATURE_TRACE_COLOR = Color.decode("#000000");
                public final static Color CORRELATED_FEATURE_TRACE_COLOR = Color.decode("#666666");
            }

            public static class StructuresView {
                public static class Sources {
                    public final static Color MAIN_DB_NO_LINK_TEXT = Color.decode("#808080").darker();
                    public final static Color MAIN_DB_NO_LINK_BORDER = Color.decode("#808080").darker();
                    //only tagged, but DB not known to SIRIUS instance
                    public final static Color CUSTOM_DB_NOT_LOADED_TEXT = Color.decode("#999999").darker();
                    public final static Color CUSTOM_DB_NOT_LOADED_BORDER = Color.decode("#999999").darker();
                }
            }
        }

        public static class Dark {
            public final static Color FOREGROUND_INTERFACE = new Color(187, 187, 187); //e.g. (new FlatDarculaLaf()).getDefaults().getColor("List.foreground");
            public final static Color FOREGROUND_DATA = Color.decode("#f2f2f2"); //95% #f2f2f2 // 98% #fafafa
            public final static Color BACKGROUND = new Color(0x3c3f41);

            public final static Color SWITCH_FOREGROUND = FOREGROUND_INTERFACE;
            public final static Color SWITCH_BACKGROUND = new Color (78, 80, 82);//new FlatDarculaLaf()).getDefaults().getColor("Button.background");

            public final static Color WEBVIEW_TEXT_IN_POPUPS_BACKGROUND = new Color(60, 63, 65); //new FlatDarculaLaf()).getDefaults().getColor("Panel.background");

            protected final static Color DARK_ACCENT;
            static {
                Color accent_FlatDarculaLaf = new Color(75, 110, 175, 255); //new FlatDarculaLaf()).getDefaults().getColor("Component.accentColor");
                float[] c = accent_FlatDarculaLaf.getComponents(new float[4]);
                DARK_ACCENT = new Color(c[0], c[1], c[2], 0.15f * c[3]);
            }

            public static class LargerCells {
                public final static Color ALTERNATING_CELL_1 = Color.decode("#595959");
                public final static Color ALTERNATING_CELL_2 = Color.decode("#3d3f41");
                public final static Color CELL_TEXT = FOREGROUND_DATA; // new Color(187, 187, 187) ; //(new FlatDarculaLaf()).getDefaults().getColor("List.foreground");

                public final static Color SELECTED_CELL = CUSTOM_LIGHT_BLUE;
            }

            public static class Tables {
                public final static Color ALTERNATING_ROW_1 = Color.decode("#595959");
                public final static Color ALTERNATING_ROW_2 = Color.decode("#3d3f41");
                public final static Color SELECTED_ROW = CUSTOM_LIGHT_BLUE;
            }

            public final static Color BEST_HIT_MAIN = GOOD_IS_GREEN_PALE;
            public final static Color BEST_HIT_WITH_TRANSPARENCY = withTransparency(BEST_HIT_MAIN, 0.7);
            public final static Color BEST_HIT_ALTERNATING_CELL_1 = Color.decode("#caf2c4"); // same as 70% transparency for main color on white table cell color
            public final static Color BEST_HIT_ALTERNATING_CELL_2 = Color.decode("#c3ebbd"); // same as 70% transparency for main color on gray table cell color
            public final static Color BEST_HIT = BEST_HIT_ALTERNATING_CELL_1;
            public final static Color BEST_HIT_SELECTED = GOOD_IS_GREEN_VIBRANT;

            public final static Color ALTERNATING_CELL_ROW_TEXT_COLOR = FOREGROUND_DATA;

            public static class Spectrum {
                public final static Color SELECTED_PEAK_BACKGROUND_BOX = Color.decode("#626262");
                public final static Color SIMULATED_ISOTOPE_PATTTEN_PEAK = Color.decode("#b2b2b2");
            }

            public static class CompoundClassesView {
                public final static Color CLASSIFIER_MAIN = GOOD_IS_GREEN_VIBRANT.darker().darker(); //new Color(49, 163, 84, 100);
                public final static Color CLASSIFIER_OTHER = CUSTOM_BLUE.darker().darker();///DARK_ACCENT;
                public final static Color CLASSIFIER_TEXT = FOREGROUND_DATA;
            }

            public static class MolecularStructures {
                public final static Color SELECTED_SUBSTRUCTURE = FOREGROUND_DATA;
                public final static Color SELECTED_SUBSTRUCTURE_WITH_GLOW_HIGHLIGHT = BACKGROUND;
                public final static Color BACKGROUND_STRUCTURE = Color.decode("#666666");
                public final static Color BREAKING_BOND = Color.decode("#f570a1"); //be aware that grey/pink contrast is not good with green deficiency
            }

            public static class LCMSVIEW {
                //for adduct/isotope view
                public final static Color SELECTED_FEATURE_TRACE_COLOR = FOREGROUND_DATA;
                public final static Color CORRELATED_FEATURE_TRACE_COLOR = Color.decode("#cccccc");
            }

            public static class StructuresView {
                public static class Sources {
                    public final static Color MAIN_DB_NO_LINK_TEXT = Color.decode("#808080").darker();
                    public final static Color MAIN_DB_NO_LINK_BORDER = Color.decode("#808080").brighter();
                    //only tagged, but DB not known to SIRIUS instance
                    public final static Color CUSTOM_DB_NOT_LOADED_TEXT = Color.decode("#999999").darker();
                    public final static Color CUSTOM_DB_NOT_LOADED_BORDER = Color.decode("#999999").brighter();
                }
            }
        }
    }




    public final static Color FOREGROUND_INTERFACE = (THEME == Theme.LIGHT ? Themes.Light.FOREGROUND : Themes.Dark.FOREGROUND_INTERFACE);
    public final static Color FOREGROUND_DATA = (THEME == Theme.LIGHT ? Themes.Light.FOREGROUND : Themes.Dark.FOREGROUND_DATA);
    public final static Color BACKGROUND = (THEME == Theme.LIGHT ? Themes.Light.BACKGROUND : Themes.Dark.BACKGROUND);

    public final static Color SWITCH_FOREGROUND = (THEME == Theme.LIGHT ? Themes.Light.SWITCH_FOREGROUND : Themes.Dark.SWITCH_FOREGROUND);
    public final static Color SWITCH_BACKGROUND = (THEME == Theme.LIGHT ? Themes.Light.SWITCH_BACKGROUND : Themes.Dark.SWITCH_BACKGROUND);

    public final static Color WEBVIEW_TEXT_IN_POPUPS_BACKGROUND = (THEME == Theme.LIGHT ? Themes.Light.WEBVIEW_TEXT_IN_POPUPS_BACKGROUND : Themes.Dark.WEBVIEW_TEXT_IN_POPUPS_BACKGROUND);


    public final static Color EXPANSIVE_SEARCH_WARNING_TEXT = Color.WHITE;
    public final static Color EXPANSIVE_SEARCH_WARNING = Menu.BUTTON_HIGHLIGHT_PINK;


    public final static Color GOOD = CUSTOM_GREEN;
    public final static Color INFO = CUSTOM_LIGHT_BLUE;
    public final static Color WARN = CUSTOM_YELLOW.brighter();
    public final static Color ERROR = CUSTOM_PINK;

    public final static Color TEXT_WARN = CUSTOM_BLUE; //CUSTOM_ORANGE; (orange not good in light theme)
    public final static Color TEXT_WARN_ORANGE = CUSTOM_ORANGE; //for specific text where contras is good enough and a true warning color is required.
    public final static Color TEXT_ERROR = Menu.BUTTON_HIGHLIGHT_PINK;
    public final static Color TEXT_GOOD = CUSTOM_GREEN;
    public final static Color TEXT_LINK = CUSTOM_BLUE;
    public final static Color TEXT_LINK_VISITED = CUSTOM_LIGHT_BLUE;
    public final static Color TEXT_LINK_ACTIVE = GOOD_IS_GREEN_VIBRANT;
    public final static Color TEXT_LINK_HOVER = CUSTOM_LIGHT_BLUE;


    public static class SplashScreen {
        public final static Color PROGRESS_BAR = new Color(212, 20, 90);
        public final static Color BACKGROUND = Color.WHITE; //is this really used as background?
    }


    public static class Menu {
        public final static Color BUTTON_HIGHLIGHT_PINK = Color.decode("#d40f57");
        public final static Color BUTTON_HIGHLIGHT_GREEN = GOOD_IS_GREEN_VIBRANT;

        public final static Color ICON_BLUE = CUSTOM_BLUE;
        public final static Color BUTTON_LIGHT_BLUE = CUSTOM_LIGHT_BLUE;

        public final static Color FILTER_BUTTON = CUSTOM_BLUE;
        public final static Color FILTER_BUTTON_INVERTED = CUSTOM_ORANGE;

        public final static Color FILTER_BUTTON_TEXT = Color.WHITE;
        public final static Color FILTER_BUTTON_INVERTED_TEXT = Color.BLACK;
    }

    public static class CellsAndRows {

        //feature navigation and structure detail view
        public static class LargerCells {
            public final static Color ALTERNATING_CELL_1 = (THEME == Theme.LIGHT ? Themes.Light.LargerCells.ALTERNATING_CELL_1 : Themes.Dark.LargerCells.ALTERNATING_CELL_1);
            public final static Color ALTERNATING_CELL_2 = (THEME == Theme.LIGHT ? Themes.Light.LargerCells.ALTERNATING_CELL_2 : Themes.Dark.LargerCells.ALTERNATING_CELL_2);
            public final static Color CELL_TEXT = (THEME == Theme.LIGHT ? Themes.Light.LargerCells.CELL_TEXT : Themes.Dark.LargerCells.CELL_TEXT);

            public final static Color SELECTED_CELL = (THEME == Theme.LIGHT ? Themes.Light.LargerCells.SELECTED_CELL : Themes.Dark.LargerCells.SELECTED_CELL);
            public final static Color SELECTED_CELL_TEXT = Color.BLACK;
        }

        public static class Tables {
            public final static Color ALTERNATING_ROW_1 = (THEME == Theme.LIGHT ? Themes.Light.Tables.ALTERNATING_ROW_1 : Themes.Dark.Tables.ALTERNATING_ROW_1);
            public final static Color ALTERNATING_ROW_2 = (THEME == Theme.LIGHT ? Themes.Light.Tables.ALTERNATING_ROW_2 : Themes.Dark.Tables.ALTERNATING_ROW_2);

            public final static Color SELECTED_ROW = (THEME == Theme.LIGHT ? Themes.Light.Tables.SELECTED_ROW : Themes.Dark.Tables.SELECTED_ROW);

            public final static Color SELECTED_ROW_TEXT = Color.BLACK;
        }
        public final static Color BEST_HIT = (THEME == Theme.LIGHT ? Themes.Light.BEST_HIT : Themes.Dark.BEST_HIT);
        public final static Color BEST_HIT_ALTERNATING_CELL_1 = (THEME == Theme.LIGHT ? Themes.Light.BEST_HIT_ALTERNATING_CELL_1 : Themes.Dark.BEST_HIT_ALTERNATING_CELL_1);
        public final static Color BEST_HIT_ALTERNATING_CELL_2 = (THEME == Theme.LIGHT ? Themes.Light.BEST_HIT_ALTERNATING_CELL_2 : Themes.Dark.BEST_HIT_ALTERNATING_CELL_2);
        public final static Color BEST_HIT_SELECTED = (THEME == Theme.LIGHT ? Themes.Light.BEST_HIT_SELECTED : Themes.Dark.BEST_HIT_SELECTED);
        public final static Color BEST_HIT_TEXT = Color.BLACK;

        public final static Color ALTERNATING_CELL_ROW_TEXT_COLOR = (THEME == Theme.LIGHT ? Themes.Light.ALTERNATING_CELL_ROW_TEXT_COLOR : Themes.Dark.ALTERNATING_CELL_ROW_TEXT_COLOR);
    }

    public static class Quality {
        public final static Color GOOD = CUSTOM_GREEN;
        public final static Color DECENT = CUSTOM_YELLOW;
        public final static Color BAD = CUSTOM_PINK;
    }

    public static class FormulasView {
        public final static Color BEST_HIT = GOOD_IS_GREEN_PALE;
        public final static Color SCORE_BAR = CUSTOM_BLUE;
    }


    public static class Spectrum {
        public final static Color SELECTED_PEAK_BACKGROUND_BOX = (THEME == Theme.LIGHT ? Themes.Light.Spectrum.SELECTED_PEAK_BACKGROUND_BOX : Themes.Dark.Spectrum.SELECTED_PEAK_BACKGROUND_BOX);
        public final static Color SIMULATED_ISOTOPE_PATTTEN_PEAK = (THEME == Theme.LIGHT ? Themes.Light.Spectrum.SIMULATED_ISOTOPE_PATTTEN_PEAK : Themes.Dark.Spectrum.SIMULATED_ISOTOPE_PATTTEN_PEAK);

        public final static Color EXPLAINED_PEAKS_FORMULA = CUSTOM_BLUE;
        public final static Color EXPLAINED_PEAKS_STRUCTURE = Color.decode("#8ee481");
    }

    public static class FragementationTree {
        //one-color, e.g. relative intensity
        public final static Color ONE_COLOR_GRADIENT_MIN = Color.decode("#ecf5f9"); //not starting with white, but rather pale blue. Else too many nodes look white
        public final static Color ONE_COLOR_GRADIENT_MAX = CUSTOM_BLUE;

        //two-color, e.g. mass deviation
        public final static Color TWO_COLOR_GRADIENT_LEFT = CUSTOM_ORANGE; //note, that CUSTOM_ORANGE (#ffc14c) is also used hard-coded in treeViewer.js (for edge case of text-coloring by mass deviation)
        public final static Color TWO_COLOR_GRADIENT_MIDDLE = Color.WHITE;
        public final static Color TWO_COLOR_GRADIENT_RIGHT = Color.decode("#a879d2"); //note, that #a879d2 is also used hard-coded in treeViewer.js (for edge case of text-coloring by mass deviation)
    }

    public static class FingerprintsView {
        public final static Color PROBABILITY_OVER_50 = Color.decode("#8ee481");
        public final static Color PROBABILITY_UNDER_50 = Color.decode("#f570a1");
        public final static Color SCORE_BARS = CUSTOM_BLUE;
    }

    public static class MolecularStructures {
        public final static Color SELECTED_SUBSTRUCTURE = (THEME == Theme.LIGHT ? Themes.Light.MolecularStructures.SELECTED_SUBSTRUCTURE : Themes.Dark.MolecularStructures.SELECTED_SUBSTRUCTURE);
        public final static Color SELECTED_SUBSTRUCTURE_WITH_GLOW_HIGHLIGHT = (THEME == Theme.LIGHT ? Themes.Light.MolecularStructures.SELECTED_SUBSTRUCTURE_WITH_GLOW_HIGHLIGHT : Themes.Dark.MolecularStructures.SELECTED_SUBSTRUCTURE_WITH_GLOW_HIGHLIGHT);
        public final static Color BACKGROUND_STRUCTURE = (THEME == Theme.LIGHT ? Themes.Light.MolecularStructures.BACKGROUND_STRUCTURE : Themes.Dark.MolecularStructures.BACKGROUND_STRUCTURE);
        public final static Color BREAKING_BOND = (THEME == Theme.LIGHT ? Themes.Light.MolecularStructures.BREAKING_BOND : Themes.Dark.MolecularStructures.BREAKING_BOND);

    }


    public static class CompoundClassesView {
        public final static Color CLASSIFIER_MAIN = (THEME == Theme.LIGHT ? Themes.Light.CompoundClassesView.CLASSIFIER_MAIN : Themes.Dark.CompoundClassesView.CLASSIFIER_MAIN);
        public final static Color CLASSIFIER_OTHER = (THEME == Theme.LIGHT ? Themes.Light.CompoundClassesView.CLASSIFIER_OTHER: Themes.Dark.CompoundClassesView.CLASSIFIER_OTHER);
        public final static Color CLASSIFIER_TEXT = (THEME == Theme.LIGHT ? Themes.Light.CompoundClassesView.CLASSIFIER_TEXT: Themes.Dark.CompoundClassesView.CLASSIFIER_TEXT);
    }

    public static class StructuresView {
        public final static Color SUBSTRUCTURE_BOX_GRADIENT_LOW_PROBABILITY = Color.decode("#f570a1");
        public final static Color SUBSTRUCTURE_BOX_GRADIENT_MEDIUM_PROBABILITY = Color.WHITE;
        public final static Color SUBSTRUCTURE_BOX_GRADIENT_HIGH_PROBABILITY = Color.decode("#8ee481");

        public final static Color SELECTED_SUBSTUCTURE_HIGHLIGHTING_PRIMARY = CUSTOM_BLUE;
        public final static Color SELECTED_SUBSTUCTURE_HIGHLIGHTING_ALTERNATIVES = Color.decode("#b3d9e5");

        public final static Color HIGHLIGHT_MATCHING_SUBSTRUCTURES_AGREEMENT_GOOD = Color.decode("#8ee481");
        public final static Color HIGHLIGHT_MATCHING_SUBSTRUCTURES_AGREEMENT_UNCERTAIN = Color.decode("#feff99");
        public final static Color HIGHLIGHT_MATCHING_SUBSTRUCTURES_AGREEMENT_DISAGREE = Color.decode("#f570a1");

        public static class Sources {
            public final static Color DEFAULT_TEXT = Color.BLACK;
            public final static Color DEFAULT_BORDER  = Color.BLACK;

            public final static Color MAIN_DB_WITH_LINK = Color.decode("#b3d9e5");

            public final static Color MAIN_DB_NO_LINK = Color.decode("#b3d9e5");
            public final static Color MAIN_DB_NO_LINK_TEXT  = (THEME == Theme.LIGHT ? Themes.Light.StructuresView.Sources.MAIN_DB_NO_LINK_TEXT : Themes.Dark.StructuresView.Sources.MAIN_DB_NO_LINK_TEXT);
            public final static Color MAIN_DB_NO_LINK_BORDER  = (THEME == Theme.LIGHT ? Themes.Light.StructuresView.Sources.MAIN_DB_NO_LINK_BORDER : Themes.Dark.StructuresView.Sources.MAIN_DB_NO_LINK_BORDER);

            public final static Color CUSTOM_DB = Color.decode("#feff9e");

            //only tagged, but DB not known to SIRIUS instance
            public final static Color CUSTOM_DB_NOT_LOADED = Color.decode("#feff9e");
            public final static Color CUSTOM_DB_NOT_LOADED_TEXT = (THEME == Theme.LIGHT ? Themes.Light.StructuresView.Sources.CUSTOM_DB_NOT_LOADED_TEXT : Themes.Dark.StructuresView.Sources.CUSTOM_DB_NOT_LOADED_TEXT);
            public final static Color CUSTOM_DB_NOT_LOADED_BORDER = (THEME == Theme.LIGHT ? Themes.Light.StructuresView.Sources.CUSTOM_DB_NOT_LOADED_BORDER : Themes.Dark.StructuresView.Sources.CUSTOM_DB_NOT_LOADED_BORDER);


            public final static Color DE_NOVO = Color.decode("#d40f57");
            public final static Color DE_NOVO_TEXT = Color.WHITE;
            public final static Color DE_NOVO_BORDER = Color.BLACK;

            //Lipid, PFAS and such
            public final static Color SPECIAL = CUSTOM_BLUE;
            public final static Color SPECIAL_TEXT = Color.WHITE;

            public final static Color TRAINING_DATA = SPECIAL;
            public final static Color TRAINING_DATA_TEXT = SPECIAL_TEXT;

        }

    }


    public static class LCMSVIEW {
        protected final static Color[] FEATUR_TRACE_COLORS = new Color[] {
                Color.decode("#8ee481"),
                Color.decode("#b48dd8"),
                Color.decode("#ffbf47"),
                Color.decode("#7cbdd0"),
                Color.decode("#fab8d0"),
                Color.decode("#ffeb62"),
                Color.decode("#7eb1f6"),
                Color.decode("#fb8072"),
                Color.decode("#bfbfbf"),
                Color.decode("#d6885c")
        };

        //for adduct/isotope view
        public final static Color SELECTED_FEATURE_TRACE_COLOR = (THEME == Theme.LIGHT ? Themes.Light.LCMSVIEW.SELECTED_FEATURE_TRACE_COLOR : Themes.Dark.LCMSVIEW.SELECTED_FEATURE_TRACE_COLOR);
        public final static Color CORRELATED_FEATURE_TRACE_COLOR = (THEME == Theme.LIGHT ? Themes.Light.LCMSVIEW.CORRELATED_FEATURE_TRACE_COLOR : Themes.Dark.LCMSVIEW.CORRELATED_FEATURE_TRACE_COLOR);

        public final static String ISOTOPE_DASH_STYLE = "5,3"; //not a Color, but style

        public final static Color MAIN_FEATURE_BOX_FILL = Color.decode("#999999");
        public final static Color MAIN_FEATURE_BOX_STROKE = Color.decode("#999999");

        public final static Color SECONDARY_FEATURE_BOX_FILL = (THEME == Theme.LIGHT ? MAIN_FEATURE_BOX_FILL.brighter() : MAIN_FEATURE_BOX_FILL.darker());
        public final static Color SECONDARY_FEATURE_BOX_STROKE = (THEME == Theme.LIGHT ? MAIN_FEATURE_BOX_STROKE.brighter() : MAIN_FEATURE_BOX_STROKE.darker());

        public final static Color LEGEND_FOCUS = (THEME == Theme.LIGHT ? Color.decode("#f2f2f2") : Color.decode("#595959"));



        public static Color getFeatureTraceColor(int index) {
            if (index < 0) return null;
            Color baseColor = FEATUR_TRACE_COLORS[index % FEATUR_TRACE_COLORS.length];
            double variationFactor = (int) Math.floor(index / FEATUR_TRACE_COLORS.length);

            Color color;
            if (THEME == Theme.DARK) {
                color = brighter(baseColor, variationFactor/10.0 + 0.1);
            } else {
                color = darker(baseColor, variationFactor/10.0);
            }
            return color;
        }


        // Method to convert an RGB color to HSL
        private static float[] rgbToHsl(Color color) {
            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;

            float max = Math.max(r, Math.max(g, b));
            float min = Math.min(r, Math.min(g, b));
            float delta = max - min;

            float h = 0;
            float s;
            float l = (max + min) / 2;

            if (delta == 0) {
                h = s = 0; // achromatic
            } else {
                if (max == r) {
                    h = (g - b) / delta + (g < b ? 6 : 0);
                } else if (max == g) {
                    h = (b - r) / delta + 2;
                } else if (max == b) {
                    h = (r - g) / delta + 4;
                }
                h /= 6;

                s = l > 0.5 ? delta / (2 - max - min) : delta / (max + min);
            }

            return new float[]{h * 360, s, l}; // H (0-360), S (0-1), L (0-1)
        }

        // Method to convert HSL to RGB
        private static Color hslToRgb(float h, float s, float l) {
            float r, g, b;

            if (s == 0) {
                r = g = b = l; // achromatic
            } else {
                float q = l < 0.5 ? l * (1 + s) : l + s - l * s;
                float p = 2 * l - q;

                r = hueToRgb(p, q, h + 1f / 3);
                g = hueToRgb(p, q, h);
                b = hueToRgb(p, q, h - 1f / 3);
            }

            return new Color(Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
        }

        // Helper method for HSL to RGB conversion
        private static float hueToRgb(float p, float q, float t) {
            if (t < 0) t += 1;
            if (t > 1) t -= 1;
            if (t < 1f / 6) return p + (q - p) * 6 * t;
            if (t < 1f / 2) return q;
            if (t < 2f / 3) return p + (q - p) * (2f / 3 - t) * 6;
            return p;
        }

        // Brighter method similar to D3's col.brighter(k)
        private static Color brighter(Color color, double factor) {
            return brighterOrDarker(color, factor, true);
        }

        // Darker method similar to D3's col.darker(k)
        private static Color darker(Color color, double factor) {
            return brighterOrDarker(color, factor, false);
        }

        private static Color brighterOrDarker(Color color, double factor, boolean makeBrighter) {
            // Convert RGB to HSL
            float[] hsl = rgbToHsl(color);
            float h = hsl[0];  // Hue (0-360)
            float s = hsl[1];  // Saturation (0-1)
            float l = hsl[2];  // Lightness (0-1)

            if (makeBrighter) {
                // Adjust the lightness by a factor of 1.1^k
                l = (float) Math.min(1, l * Math.pow(1.1, factor));
            } else {
                // Adjust the lightness by a factor of 0.9^k
                l = (float) Math.max(0, l * Math.pow(0.9, factor));
            }
            // Convert back to RGB
            return hslToRgb(h / 360, s, l);
        }
    }

    static {
        //it seems this needs to be initialized for use in JS css file if not used elsewhere in Java
        new Spectrum();
    }

    public static void adjustLookAndFeel() {
        Color accent = UIManager.getColor("Component.accentColor");
        if (accent != null) {
            float[] c = new float[4];
            Menu.ICON_BLUE.getComponents(c);
            Color darkAccent = new Color(c[0], c[1], c[2], 0.15f * c[3]);

            UIManager.put("TabbedPane.underlineColor", new ColorUIResource(Menu.ICON_BLUE));
            UIManager.put("TabbedPane.inactiveUnderlineColor", new ColorUIResource(Menu.ICON_BLUE));
            UIManager.put("TabbedPane.selectedBackground", new ColorUIResource(darkAccent)); //same as DARK_ACCENT in theme above
            UIManager.put("ComboBox:\"ComboBox.listRenderer\"[Selected].background", new ColorUIResource(accent));
            UIManager.put("ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground", new ColorUIResource(Color.WHITE));
        }  else {
            System.err.println("Could not load look and feel colors");
        }


        //set further component colors
        //todo some components are still missing. in light mode, it seems that some property names are different
        UIManager.put("ProgressBar.foreground", new ColorUIResource(Menu.ICON_BLUE));  // The fill color of the progress bar
        UIManager.put("ProgressBar.background", new ColorUIResource(Menu.BUTTON_LIGHT_BLUE));  // The background color
        UIManager.put("ProgressBar.selectionForeground", new ColorUIResource(Color.BLACK)); // Color of the percentage text (foreground)
        UIManager.put("ProgressBar.selectionBackground", new ColorUIResource(Color.BLACK)); // Background color of the percentage text


        UIManager.put("CheckBox.icon.selectedBackground", new ColorUIResource(Menu.ICON_BLUE)); //radio buttons, checkboxes //todo this only works in dark mode, not light mode
        UIManager.put("CheckBox.icon.checkmarkColor", new ColorUIResource(Color.WHITE));

        UIManager.put("Component.focusedBorderColor", (THEME == Theme.LIGHT ? CUSTOM_LIGHT_BLUE : CUSTOM_LIGHT_BLUE.darker()));
        UIManager.put("Component.focusColor", (THEME == Theme.LIGHT ? CUSTOM_LIGHT_BLUE : CUSTOM_LIGHT_BLUE.darker()));


        UIManager.put("OptionPane.questionIcon", Icons.HELP.derive(32,32));
    }


    public static String asHex(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        // Format the RGB values into a hex string
        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     *
     * @param c
     * @param transparency value between 0 and 1 to specify the transparency using alpha (1 corresponds to alpha of 256)
     * @return
     */
    public static Color withTransparency(Color c, double transparency) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(256*transparency));
    }

}
