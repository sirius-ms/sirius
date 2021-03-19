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
    public static final Font  FONT_BOLD;
    public static final Font  FONT;
    public static final Font  FONT_ITALIC;
    public static final Font  FONT_BOLD_ITALIC;
    public static final Font  FONT_MONO;

     static {
         Font tm1 = null;
         Font tm2 = null;
         Font tm3 = null;
         Font tm4 = null;
         Font tm5 = null;
         try {
             tm1 = Font.createFont(Font.TRUETYPE_FONT, Fonts.class.getResourceAsStream("/ttf/DejaVuSans-Bold.ttf"));
             tm2 = Font.createFont(Font.TRUETYPE_FONT, Fonts.class.getResourceAsStream("/ttf/DejaVuSans.ttf"));
             tm3 = Font.createFont(Font.TRUETYPE_FONT, Fonts.class.getResourceAsStream("/ttf/DejaVuSans-Oblique.ttf"));
             tm4 = Font.createFont(Font.TRUETYPE_FONT, Fonts.class.getResourceAsStream("/ttf/DejaVuSans-BoldOblique.ttf"));
             tm5 = Font.createFont(Font.TRUETYPE_FONT, Fonts.class.getResourceAsStream("/ttf/DejaVuSansMono-Bold.ttf"));
         } catch (FontFormatException | IOException e) {
             LoggerFactory.getLogger(Fonts.class).error("Could not load default font", e);
         }
         FONT_BOLD = tm1;
         FONT = tm2;
         FONT_ITALIC = tm3;
         FONT_BOLD_ITALIC = tm4;
         FONT_MONO = tm5;

     }

     public static void initFonts(){
         if (PropertyManager.getBoolean("de.unijena.bioinf.sirius.ui.enableaa",false)){
             System.setProperty("awt.useSystemAAFontSettings","on");
             System.setProperty("swing.aatext", "true");
             LoggerFactory.getLogger(Fonts.class).debug("Global Anti Aliasing enabled");
         }

         if (PropertyManager.getBoolean("de.unijena.bioinf.sirius.ui.enforcefont",false)){
             UIManager.getLookAndFeelDefaults().put("defaultFont", FONT);
             LoggerFactory.getLogger(Fonts.class).debug(FONT.getFontName() +  " enforced!");
         }
     }
}
