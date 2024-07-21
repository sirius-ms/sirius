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
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 11.10.16.
 */

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class Colors {


    public final static Color ICON_BLUE = new Color(17, 145, 187);
    public final static Color ICON_GREEN = new Color(0, 191, 48);
    public final static Color ICON_RED = new Color(204, 71, 41);
    public final static Color ICON_YELLOW = new Color(255, 204, 0);

    public final static Color GRADIENT_GREEN;
    public final static Color GRADIENT_RED;
    public final static Color GRADIENT_YELLOW;

    public final static Color FOREGROUND;

    public final static Color BACKGROUND;

    public final static Color SWITCH_FOREGROUND;
    public final static Color SWITCH_BACKGROUND;
    public final static Color LIST_SELECTED_BACKGROUND;
    public final static Color LIST_SELECTED_FOREGROUND;
    public final static Color LIST_EVEN_BACKGROUND;
    public final static Color LIST_DISABLED_BACKGROUND = UIManager.getColor("ComboBox.background");
    public final static Color LIST_UNEVEN_BACKGROUND;
    public final static Color LIST_ACTIVATED_FOREGROUND = UIManager.getColor("List.foreground");
    public final static Color LIST_DEACTIVATED_FOREGROUND = Color.GRAY;
    public final static Color LIST_LIGHT_GREEN;
    public final static Color LIST_SELECTED_GREEN = new Color(49, 163, 84);

    public final static Color EXPANSIVE_SEARCH_WARNING = new Color(212, 15, 87);

    public final static Color DB_LINKED = new Color(155, 166, 219);
    public final static Color DB_UNLINKED = Color.GRAY;
    public final static Color DB_CUSTOM = ICON_YELLOW;
    public final static Color DB_TRAINING = Color.BLACK;
    public final static Color DB_UNKNOWN = new Color(155, 122, 0);// new Color(178,34,34);
    public final static Color DB_ELGORDO = ICON_BLUE;
    public final static Color DB_DENOVO = EXPANSIVE_SEARCH_WARNING;

    public final static Color CLASSIFIER_MAIN;
    public final static Color CLASSIFIER_OTHER;

    static {
        final Properties props = SiriusProperties.SIRIUS_PROPERTIES_FILE().asProperties();
        final String theme = props.getProperty("de.unijena.bioinf.sirius.ui.theme", "Light");

        Color listSelectedBg, listSelectedFg, listEvenBg, listUnevenBg, listLightGreen, gradientGreen, gradientRed,
                gradientYellow, fg, bg, classifierMain, classifierOther, switchForeground;

        switch (theme) {
            case "Dark":
                try {
                    UIManager.setLookAndFeel(new FlatDarculaLaf());
                    Color accent = UIManager.getColor("Component.accentColor");
                    float[] c = new float[4];
                    accent.getComponents(c);
                    Color darkAccent = new Color(c[0], c[1], c[2], 0.15f * c[3]);

                    UIManager.put("TabbedPane.selectedBackground", darkAccent);
                    UIManager.put("ComboBox:\"ComboBox.listRenderer\"[Selected].background", accent);
                    UIManager.put("ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground", Color.WHITE);

                    fg = new Color(187, 187, 187);
                    bg = new Color(0x3c3f41);

                    switchForeground = fg;

                    listSelectedBg = accent;
                    listSelectedFg = Color.WHITE;
                    listEvenBg = new Color(0x3c3f41);
                    listUnevenBg = darkAccent;
                    listLightGreen = new Color(49, 163, 84, 100);

                    gradientGreen = new Color(0, 134, 34);
                    gradientRed = new Color(143, 50, 29);
                    gradientYellow = new Color(128, 102, 0);

                    classifierMain =  new Color(49, 163, 84, 100);
                    classifierOther = darkAccent;
                    break;
                } catch (UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                }
            case "Light":
                try {
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    Color accent = UIManager.getColor("Component.accentColor");
                    float[] c = new float[4];
                    accent.getComponents(c);

                    UIManager.put("TabbedPane.selectedBackground", new Color(c[0], c[1], c[2], 0.15f * c[3]));
                    UIManager.put("ComboBox:\"ComboBox.listRenderer\"[Selected].background", accent);
                    UIManager.put("ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground", Color.WHITE);

                    fg = Color.BLACK;
                    bg = Color.WHITE;

                    switchForeground = new Color(210, 210, 210);

                    listSelectedBg = accent;
                    listSelectedFg = Color.WHITE;
                    listEvenBg = Color.WHITE;
                    listUnevenBg = new Color(213, 227, 238);
                    listLightGreen = new Color(161, 217, 155);

                    gradientGreen = new Color(0, 191, 48);
                    gradientRed = new Color(204, 71, 41);
                    gradientYellow = new Color(255, 204, 0);

                    classifierMain = new Color(0xe5f5e0);
                    classifierOther = new Color(0xdeebf7);
                    break;
                } catch (UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                }
            case "Classic":
            default:
                try {
                    for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        if ("Nimbus".equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                fg = Color.BLACK;
                bg = Color.WHITE;
                switchForeground = new Color(210, 210, 210);

                listSelectedBg = new Color(38, 117, 191);
                listSelectedFg = Color.WHITE;
                listEvenBg = Color.WHITE;
                listUnevenBg = new Color(213, 227, 238);
                listLightGreen = new Color(161, 217, 155);

                gradientGreen = new Color(0, 191, 48);
                gradientRed = new Color(204, 71, 41);
                gradientYellow = new Color(255, 204, 0);

                classifierMain = new Color(0xe5f5e0);
                classifierOther = new Color(0xdeebf7);
        }

        FOREGROUND = fg;
        BACKGROUND = bg;

        LIST_SELECTED_BACKGROUND = listSelectedBg;
        LIST_SELECTED_FOREGROUND = listSelectedFg;
        LIST_EVEN_BACKGROUND = listEvenBg;
        LIST_UNEVEN_BACKGROUND = listUnevenBg;
        LIST_LIGHT_GREEN = listLightGreen;

        GRADIENT_GREEN = gradientGreen;
        GRADIENT_RED = gradientRed;
        GRADIENT_YELLOW = gradientYellow;

        CLASSIFIER_MAIN = classifierMain;
        CLASSIFIER_OTHER = classifierOther;

        SWITCH_FOREGROUND = switchForeground;
        SWITCH_BACKGROUND = new JButton().getBackground(); //hacky
    }

}
