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
