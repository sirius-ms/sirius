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
 * 26.01.17.
 */

import de.unijena.bioinf.ms.properties.PropertyManager;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class Fonts {
    public static final Font FONT;
    public static final Font FONT_ITALIC;
    public static final Font FONT_MEDIUM;
    public static final Font FONT_MEDIUM_ITALIC;
    public static final Font FONT_BOLD;
    public static final Font FONT_BOLD_ITALIC;
    public static final Font FONT_MONO;
    public static final Font FONT_DEJAVU_SANS;


    public static final String LOCATION_FONT = "/ttf/Roboto-Regular.ttf";
    public static final String LOCATION_FONT_ITALIC = "/ttf/Roboto-Italic.ttf";
    public static final String LOCATION_FONT_MEDIUM = "/ttf/Roboto-Medium.ttf";
    public static final String LOCATION_FONT_MEDIUM_ITALIC = "/ttf/Roboto-MediumItalic.ttf";
    public static final String LOCATION_FONT_BOLD = "/ttf/Roboto-Bold.ttf";
    public static final String LOCATION_FONT_BOLD_ITALIC = "/ttf/Roboto-BoldItalic.ttf";
    public static final String LOCATION_FONT_MONO = "/ttf/RobotoMono-Regular.ttf";
    public static final String LOCATION_FONT_DEJAVU_SANS = "/ttf/DejaVuSans.ttf";

     static {
         Font tm1 = null;
         Font tm2 = null;
         Font tm3 = null;
         Font tm4 = null;
         Font tm5 = null;
         Font tm6 = null;
         Font tm7 = null;
         Font tm8 = null;
         try {
             tm1 = Font.createFont(Font.TRUETYPE_FONT, Fonts.class.getResourceAsStream(LOCATION_FONT));
             tm2 = Font.createFont(Font.TRUETYPE_FONT, Fonts.class.getResourceAsStream(LOCATION_FONT_ITALIC));
             tm3 = Font.createFont(Font.TRUETYPE_FONT, Fonts.class.getResourceAsStream(LOCATION_FONT_MEDIUM));
             tm4 = Font.createFont(Font.TRUETYPE_FONT, Fonts.class.getResourceAsStream(LOCATION_FONT_MEDIUM_ITALIC));
             tm5 = Font.createFont(Font.TRUETYPE_FONT, Fonts.class.getResourceAsStream(LOCATION_FONT_BOLD));
             tm6 = Font.createFont(Font.TRUETYPE_FONT, Fonts.class.getResourceAsStream(LOCATION_FONT_BOLD_ITALIC));
             tm7 = Font.createFont(Font.TRUETYPE_FONT, Fonts.class.getResourceAsStream(LOCATION_FONT_MONO));
             tm8 = Font.createFont(Font.TRUETYPE_FONT, Fonts.class.getResourceAsStream(LOCATION_FONT_DEJAVU_SANS));
         } catch (FontFormatException | IOException e) {
             LoggerFactory.getLogger(Fonts.class).error("Could not load default font", e);
         }
         FONT = tm1;
         FONT_ITALIC = tm2;
         FONT_MEDIUM = tm3;
         FONT_MEDIUM_ITALIC = tm4;
         FONT_BOLD = tm5;
         FONT_BOLD_ITALIC = tm6;
         FONT_MONO = tm7;
         FONT_DEJAVU_SANS = tm8;
     }

     public static void initFonts(){
         if (PropertyManager.getBoolean("de.unijena.bioinf.sirius.ui.enableaa",false)){
             System.setProperty("awt.useSystemAAFontSettings","on");
             System.setProperty("swing.aatext", "true");
             LoggerFactory.getLogger(Fonts.class).debug("Global Anti Aliasing enabled");
         }

         float defaultFontSize = getDefaultFontSize();
         UIManager.getDefaults().put("defaultFont", FONT.deriveFont(defaultFontSize));
     }

    private static float getDefaultFontSize() {
         //if this ever does not work, there also exists 'Toolkit.getToolkit().getFontLoader().getSystemFontSize()'
        Font defaultFont = UIManager.getFont("defaultFont");
        if (defaultFont == null) defaultFont = UIManager.getFont("Label.font");
        if (defaultFont == null) defaultFont = (new JLabel().getFont());

        return defaultFont.getSize();
    }

    public static String getFontURLExternalForm() {
        return Fonts.class.getResource(LOCATION_FONT).toExternalForm();
    }

}
