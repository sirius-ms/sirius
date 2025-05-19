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

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGUtils;
import de.unijena.bioinf.ms.gui.dialogs.AboutDialog;
import de.unijena.bioinf.ms.gui.login.AccountPanel;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class Icons {
    //landing page + external
    public static final FlatSVGIcon SIRIUS_WELCOME_DARK = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/sirius-welcome-bg.svg"));
    public static final FlatSVGIcon SIRIUS_WELCOME_LIGHT = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/sirius-welcome-white.svg"));
    public static final FlatSVGIcon SIRIUS_WELCOME = new FlatSVGIcon(Colors.isLightTheme() ? SIRIUS_WELCOME_DARK : SIRIUS_WELCOME_LIGHT);

    public static final FlatSVGIcon DOWNLOAD_DARK = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/download-dark.svg"));
    public static final FlatSVGIcon DOWNLOAD_LIGHT = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/download-white.svg"));
    public static FlatSVGIcon DOWNLOAD = Colors.isLightTheme() ? DOWNLOAD_DARK : DOWNLOAD_LIGHT;

    public static final FlatSVGIcon BUG2_DARK = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/bug2-dark.svg"));
    public static final FlatSVGIcon BUG2_LIGHT = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/bug2-white.svg"));
    public static FlatSVGIcon BUG2 = Colors.isLightTheme() ? BUG2_DARK : BUG2_LIGHT;

    public static final FlatSVGIcon ROCKET_DARK = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/rocket-dark.svg"));
    public static final FlatSVGIcon ROCKET_LIGHT = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/rocket-white.svg"));
    public static FlatSVGIcon ROCKET = Colors.isLightTheme() ? ROCKET_DARK : ROCKET_LIGHT;

    public static final FlatSVGIcon DOCU_DARK = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/docu-dark.svg"));
    public static final FlatSVGIcon DOCU_LIGHT = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/docu-white.svg"));
    public static FlatSVGIcon DOCU = Colors.isLightTheme() ? DOCU_DARK : DOCU_LIGHT;

    public static final FlatSVGIcon MATRIX_DARK = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/matrix-dark.svg"));
    public static final FlatSVGIcon MATRIX_LIGHT = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/matrix-white.svg"));
    public static FlatSVGIcon MATRIX = Colors.isLightTheme() ? MATRIX_DARK : MATRIX_LIGHT;

    public static final FlatSVGIcon GITHUB_DARK = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/github-dark.svg"));
    public static final FlatSVGIcon GITHUB_LIGHT = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/github-white.svg"));
    public static FlatSVGIcon GITHUB = Colors.isLightTheme() ? GITHUB_DARK : GITHUB_LIGHT;

    public static final FlatSVGIcon YT_DARK = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/yt-dark.svg"));
    public static final FlatSVGIcon YT_LIGHT = new FlatSVGIcon(MainFrame.class.getResource("/icons/landing-page/yt-white.svg"));
    public static FlatSVGIcon YT = Colors.isLightTheme() ? YT_DARK : YT_LIGHT;

    //loaders
    public static final ImageIcon FB_LOADER_RUN_32 = new ImageIcon(MainFrame.class.getResource("/icons/fb_loader@0.5x.gif"));
    public static final FlatSVGIcon FB_LOADER_STOP_64 = new FlatSVGIcon(MainFrame.class.getResource("/icons/fb_loader.svg"));

    //ICONS
    public static final FlatSVGIcon SIRIUS_WORDMARK_DARK = new FlatSVGIcon(MainFrame.class.getResource("/icons/sirius-wordmark-bg.svg"));
    public static final FlatSVGIcon SIRIUS_WORDMARK_LIGHT = new FlatSVGIcon(MainFrame.class.getResource("/icons/sirius-wordmark-white.svg"));
    public static final FlatSVGIcon SIRIUS_WORDMARK = new FlatSVGIcon(Colors.isLightTheme() ? SIRIUS_WORDMARK_DARK : SIRIUS_WORDMARK_LIGHT);

    public static final FlatSVGIcon NO = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-no.svg"));
    public static final FlatSVGIcon YES = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-yes.svg"));

    public static final FlatSVGIcon NET_NO = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-network-no.svg"));
    public static final FlatSVGIcon NET_YES = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-network-yes.svg"));
    public static final FlatSVGIcon NET_WARN = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-network-warn.svg"));
    public static final FlatSVGIcon NET = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-network.svg"));

    public static final FlatSVGIcon DOC = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-document.svg"));
    public static final FlatSVGIcon ADD_DOC = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-add-doc.svg"));
    public static final FlatSVGIcon DOCS = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-documents.svg"));
    public static final FlatSVGIcon REMOVE_DOC = new FlatSVGIcon(Icons.class.getResource("/icons/circular-icons-svg/c-remove-doc.svg"));

    public static final FlatSVGIcon FOLDER = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-folder.svg"));
    public static final FlatSVGIcon FOLDER_OPEN = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-folder-open.svg"));
    public static final FlatSVGIcon FOLDER_CLOSE = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-folder-close.svg"));
    public static final FlatSVGIcon FOLDER_FILE = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-folder-file.svg"));

    public static final FlatSVGIcon SIRIUS = new FlatSVGIcon(Icons.class.getResource("/icons/circular-icons-svg/c-sirius.svg"));
    public static final FlatSVGIcon ZODIAC = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-zodiac.svg"));
    public static final FlatSVGIcon FINGER =  new FlatSVGIcon( Icons.class.getResource("/icons/circular-icons-svg/c-fingerprint.svg"));
    public static final FlatSVGIcon DENOVO = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-msnovelist.svg"));
    public static final FlatSVGIcon DB_LENS = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/db-lens.svg"));

    public static final FlatSVGIcon DB = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-db.svg"));
    public static final FlatSVGIcon GEAR = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-gear.svg"));
    public static final FlatSVGIcon LOG = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-log.svg"));
    public static final FlatSVGIcon CLIP_BOARD = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-clipboard.svg"));
    public static final FlatSVGIcon BUG = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-bug.svg"));
    public static final FlatSVGIcon KEY = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-key.svg"));
    public static final FlatSVGIcon USER = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-user.svg"));
    public static final FlatSVGIcon USER_NOT_LOGGED_IN = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-user-no-login.svg"));
    public static final FlatSVGIcon USER_GREEN = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-user_green.svg"));
    public static final FlatSVGIcon HELP = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-question_blue.svg"));
    public static final FlatSVGIcon INFO = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-information.svg"));
    public static final FlatSVGIcon FBMN = new FlatSVGIcon(Icons.class.getResource("/icons/circular-icons-svg/c-fbmn.svg"));
    public static final FlatSVGIcon REFRESH = new FlatSVGIcon(Icons.class.getResource("/icons/circular-icons-svg/c-refresh.svg"));
    public static final FlatSVGIcon LOAD_ALL = new FlatSVGIcon(Icons.class.getResource("/icons/circular-icons-svg/c-loadAll.svg"));
    public static final FlatSVGIcon EXPORT =  new FlatSVGIcon( Icons.class.getResource("/icons/circular-icons-svg/c-export.svg"));
    public static final FlatSVGIcon RUN =  new FlatSVGIcon(Icons.class.getResource("/icons/circular-icons-svg/c-controls-play.svg"));
    public static final FlatSVGIcon LIST_ADD =  new FlatSVGIcon( Icons.class.getResource("/icons/circular-icons-svg/c-list-add.svg"));
    public static final FlatSVGIcon LIST_EDIT =  new FlatSVGIcon( Icons.class.getResource("/icons/circular-icons-svg/c-list-edit.svg"));
    public static final FlatSVGIcon LIST_REMOVE =  new FlatSVGIcon( Icons.class.getResource("/icons/circular-icons-svg/c-list-remove.svg"));
    public static final FlatSVGIcon Zoom_In =  new FlatSVGIcon( Icons.class.getResource("/icons/circular-icons-svg/c-magnify-plus.svg"));
    public static final FlatSVGIcon Zoom_Out =  new FlatSVGIcon( Icons.class.getResource("/icons/circular-icons-svg/c-magnify-minus.svg"));
    public static final FlatSVGIcon FILTER_UP =  new FlatSVGIcon( Icons.class.getResource("/icons/circular-icons-svg/c-filter-up.svg"));
    public static final FlatSVGIcon FILTER_DOWN =  new FlatSVGIcon( Icons.class.getResource("/icons/circular-icons-svg/c-filter-down.svg"));
    public static final FlatSVGIcon MOLECULAR_PROPERTY = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-molecularProperty.svg"));
    public static final FlatSVGIcon SAMPLE = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-sample.svg"));

    public static final Icon SIRIUS_SPLASH = new ImageIcon(AboutDialog.class.getResource("/icons/sirius.png"));
    public static final Icon SIRIUS_APP_ICON = new ImageIcon(Icons.class.getResource("/icons/sirius_icon.png"));
    public static final Image SIRIUS_APP_IMAGE = Toolkit.getDefaultToolkit().createImage(Icons.class.getResource("/icons/sirius_icon.png"));


    public static final FlatSVGIcon FMET_FILTER_ENABLED = new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-fmet.svg"));


    public static final Icon NO_MATCH_128 = new ImageIcon(MainFrame.class.getResource("/icons/nothing-found.png"));
    public static BufferedImage NO_RESULT_IMAGE_160() {
        try {
            return ImageIO.read(Icons.class.getResource("/icons/no-results-icon-160.png"));
        } catch (IOException e) {
            LoggerFactory.getLogger(Icons.class).error("Could not read image!", e);
            return null;
        }
    }

    public static final FlatSVGIcon DRAG_N_DROP =  new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/c-dragndrop.svg"));
    public static BufferedImage DRAG_N_DROP_IMAGE(int width, int height) {
        return FlatSVGUtils.svg2image(MainFrame.class.getResource("/icons/circular-icons-svg/c-dragndrop.svg"), width, height);
    }

    public static final FlatSVGIcon TRAFFIC_LIGHT_LOWEST =  new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/red_exclamation-mark.svg"));
    public static final FlatSVGIcon TRAFFIC_LIGHT_RED =  new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/red.svg"));
    public static final FlatSVGIcon TRAFFIC_LIGHT_YELLOW =  new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/yellow.svg"));
    public static final FlatSVGIcon TRAFFIC_LIGHT_GREEN =  new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/green.svg"));

    public static final FlatSVGIcon TRAFFIC_LIGHT_LOWEST_BOARDER =  new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/red_exclamation-mark_thick_border.svg"));
    public static final FlatSVGIcon TRAFFIC_LIGHT_RED_BOARDER =  new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/red_thick_border.svg"));
    public static final FlatSVGIcon TRAFFIC_LIGHT_YELLOW_BOARDER =  new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/yellow_thick_border.svg"));
    public static final FlatSVGIcon TRAFFIC_LIGHT_GREEN_BOARDER =  new FlatSVGIcon(MainFrame.class.getResource("/icons/circular-icons-svg/green_thick_border.svg"));

    public static final FlatSVGIcon[] TRAFFIC_LIGHT_BOARDER = new FlatSVGIcon[]{
            TRAFFIC_LIGHT_RED_BOARDER, TRAFFIC_LIGHT_YELLOW_BOARDER, TRAFFIC_LIGHT_GREEN_BOARDER,
    };

    public static final FlatSVGIcon[] TRAFFIC_LIGHT = new FlatSVGIcon[]{
            TRAFFIC_LIGHT_RED, TRAFFIC_LIGHT_YELLOW, TRAFFIC_LIGHT_GREEN,
    };

    public static Image makeEllipse(Image image) {
        return makeRoundedCorner(image);
    }
    public static Image makeRoundedCorner(Image image) {
        return makeRoundedCorner(image, null);
    }
    public static Image makeRoundedCorner(Image image, @Nullable Integer cornerRadius) {
        if (!(image instanceof BufferedImage)){
            LoggerFactory.getLogger(AccountPanel.class).warn("Cannot crop Image to circle. Only BufferedSImages are supported.");
            return image;
        }

        int w = ((BufferedImage)image).getWidth();
        int h = ((BufferedImage)image).getHeight();

        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = output.createGraphics();

        // This is what we want, but it only does hard-clipping, i.e. aliasing
        // g2.setClip(new RoundRectangle2D ...)

        // so instead fake soft-clipping by first drawing the desired clip shape
        // in fully opaque white with antialiasing enabled...
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        if (cornerRadius == null)
            g2.fill(new Ellipse2D.Float(0, 0, w, h));
        else
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));
        //Ellipse2D
        // ... then compositing the image on top,
        // using the white shape from above as alpha source
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, null);

        g2.dispose();

        return output;
    }

    public static Image scaledInstance(Image image, int width, int height) {
        return image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }
}
