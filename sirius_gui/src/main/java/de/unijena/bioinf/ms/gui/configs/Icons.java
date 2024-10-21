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
    //ICONS

//    public static final ImageIcon ATOM_LOADER_120 = new ImageIcon(MainFrame.class.getResource("/icons/loaders/AtomSpinner_120_2s.gif"));
//    public static final ImageIcon ATOM_LOADER_200 = new ImageIcon(MainFrame.class.getResource("/icons/loaders/AtomSpinner_200_2s.gif"));

    public static final ImageIcon ECLIPSE_LOADER_120 = new ImageIcon(MainFrame.class.getResource("/icons/loaders/eclipseSpinner-120.gif"));
    public static final ImageIcon ECLIPSE_LOADER_160 = new ImageIcon(MainFrame.class.getResource("/icons/loaders/eclipseSpinner-160.gif"));
    public static final ImageIcon ECLIPSE_LOADER_THICK_160 = new ImageIcon(MainFrame.class.getResource("/icons/loaders/eclipseSpinnerThick-160.gif"));
    public static final ImageIcon ECLIPSE_LOADER_200 = new ImageIcon(MainFrame.class.getResource("/icons/loaders/eclipseSpinner-200.gif"));

    public static final Icon FP_LOADER = new ImageIcon(MainFrame.class.getResource("/icons/fp-binary-sirius.gif"));

    public static final Icon NO_MATCH_128 = new ImageIcon(MainFrame.class.getResource("/icons/nothing-found.png"));

    public static final Icon NO_16 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-no-16px.png"));

    public static final Icon YES_16 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-yes-16px.png"));

    public static final Icon FB_LOADER_RUN_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/fb_loader.gif"));
    public static final Icon FB_LOADER_STOP_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/fb_loader.png"));

    public static final Icon NET_NO_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-network-no@0.5x.png"));
//    public static final Icon NET_NO_16 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-network-no-16px.png"));
    public static final Icon NET_YES_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-network-yes@0.5x.png"));
//    public static final Icon NET_YES_16 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-network-yes-16px.png"));
    public static final Icon NET_WARN_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-network-warn@0.5x.png"));
//    public static final Icon NET_WARN_16 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-network-warn-16px.png"));
    public static final Icon NET_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-network@0.5x.png"));
    public static final Icon NET_64 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-network.png"));
    public static final Icon NET_16 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-network-16px.png"));

    public static final Icon ADD_DOC_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-add-doc@0.5x.png"));
    public static final Icon DOC_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-document@0.5x.png"));
    public static final Icon DOCS_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-documents@0.5x.png"));

    public static final Icon FOLDER_OPEN_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-folder-open@0.5x.png"));
    public static final Icon FOLDER_CLOSE_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-folder-close@0.5x.png"));
    public static final Icon FOLDER_FILE_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-folder-file@0.5x.png"));

    public static final Icon DB_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-db@0.5x.png"));
    public static final Icon DB_24 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-db-24px.png"));
    public static final Icon DB_64 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-db.png"));

    public static final Icon DENOVO_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-denovo@0.5x.png"));
    public static final Icon DENOVO_24 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-denovo-24px.png"));
    public static final Icon DENOVO_64 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-denovo.png"));

    public static final Icon DB_LENS_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/db-lens@0.5x.png"));
    public static final Icon DB_LENS_24 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/db-lens-24px.png"));
    public static final Icon DB_LENS_64 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/db-lens.png"));

    public static final Icon GEAR_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-gear@0.5x.png"));
    public static final Icon GEAR_64 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-gear.png"));

    public static final Icon LOG_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-log@0.5x.png"));
    public static final Icon LOG_64 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-log.png"));

    public static final Icon CLIP_BOARD_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-clipboard@0.5x.png"));

    public static final Icon BUG_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-bug@0.5x.png"));
    public static final Icon BUG_64 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-bug.png"));

    public static final Icon KEY_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-key@0.5x.png"));
    public static final Icon KEY_64 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-key.png"));

    public static final Icon USER_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-user@0.5x.png"));
    public static final Icon USER_64 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-user.png"));
    public static final Icon USER_128 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-user@2x.png"));

    public static final Icon USER_GREEN_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-user_green@0.5x.png"));
    public static final Icon USER_GREEN_64 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-user_green.png"));
    public static final Icon USER_GREEN_128 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-user_green@2x.png"));

    public static final Icon HELP_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-question_blue@0.5x.png"));

    public static final Icon INFO_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-information@0.5x.png"));
    public static final Icon CHOOSE_FILE_16 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-folder-file-16px.png"));

    public static final Icon FBMN_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-fbmn-16px.png"));
    public static final Icon FBMN_32 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-fbmn@0.5x.png"));
//    public static final Icon FBMN_64 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-fbmn.png"));

    public static final Icon REFRESH_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-refresh-16px.png"));
    public static final Icon REFRESH_32 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-refresh@0.5x.png"));

    public static final Icon SIRIUS_32 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-sirius@0.5x.png"));
    public static final Icon SIRIUS_64 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-sirius.png"));

    public static final Icon WORM_32 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-worm@0.5x.png"));
    public static final Icon WORM_64 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-worm.png"));

    public static final Icon LOAD_ALL_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-loadAll-16px.png"));
    public static final Icon LOAD_ALL_24 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-loadAll-24px.png"));
    public static final Icon LOAD_ALL_32 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-loadAll.png"));


    public static final Icon ADD_DOC_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-add-doc-16px.png"));
    public static final Icon BATCH_DOC_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-documents-16px.png"));
    public static final Icon REMOVE_DOC_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-remove-doc-16px.png"));
    public static final Icon EDIT_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-edit-16px.png"));
    public static final Icon EXPORT_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-export-16px.png"));
    public static final Icon EXPORT_24 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-export-24px.png"));
    public static final Icon EXPORT_20 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-export-20px.png"));
    public static final Icon EXPORT_32 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-export@0.5x.png"));
    public static final Icon EXPORT_48 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-export-48px.png"));
    public static final Icon RUN_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-controls-play-16px.png"));
    public static final Icon RUN_32 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-controls-play@0.5x.png"));
    public static final Icon RUN_64 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-controls-play.png"));
    public static final Icon CANCEL_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-no-16px.png"));
    public static final Icon CANCEL_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-no@0.5x.png"));
    public static final Icon LIST_ADD_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-list-add-16px.png"));
    public static final Icon LIST_EDIT_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-list-edit-16px.png"));
    public static final Icon LIST_REMOVE_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-list-remove-16px.png"));
    public static final Icon Zoom_In_24 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-magnify-plus-24px.png"));
    public static final Icon Zoom_In_20 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-magnify-plus-20px.png"));
    public static final Icon Zoom_In_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-magnify-plus-16px.png"));
    public static final Icon Zoom_Out_24 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-magnify-minus-24px.png"));
    public static final Icon Zoom_Out_20 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-magnify-minus-20px.png"));
    public static final Icon Zoom_Out_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-magnify-minus-16px.png"));
    public static final Icon FILTER_UP_24 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-filter-up-24px.png"));
    public static final Icon FILTER_DOWN_24 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-filter-down-24px.png"));
    public static final Icon FINGER_16 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-fingerprint-16px.png"));
    public static final Icon FINGER_32 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-fingerprint@0.5x.png"));
    public static final Icon FINGER_64 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-fingerprint.png"));

//    public static final Icon DRAG_N_DROP_256 = new ImageIcon(Icons.class.getResource("/icons/circular-icons/c-dragndrop@4x.png"));

    public static final Icon SIRIUS_SPLASH = new ImageIcon(AboutDialog.class.getResource("/icons/sirius.png"));
    public static final Icon SIRIUS_APP_ICON = new ImageIcon(Icons.class.getResource("/icons/sirius_icon.png"));
    public static final Image SIRIUS_APP_IMAGE = Toolkit.getDefaultToolkit().createImage(Icons.class.getResource("/icons/sirius_icon.png"));



    public static BufferedImage NO_RESULT_IMAGE_160() {
        try {
            return ImageIO.read(Icons.class.getResource("/icons/no-results-icon-160.png"));
        } catch (IOException e) {
            LoggerFactory.getLogger(Icons.class).error("Could not read image!", e);
            return null;
        }
    }

    public static BufferedImage DRAG_N_DROP_IMAGE_160() {
        try {
            return ImageIO.read(Icons.class.getResource("/icons/circular-icons/c-dragNdrop-160px.png"));
        } catch (IOException e) {
            LoggerFactory.getLogger(Icons.class).error("Could not read image!", e);
            return null;
        }
    }

    public static final Icon MolecularProperty_24 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-molecularPropertyWhite-24px.png"));


    public static final Icon[] TRAFFIC_LIGHT_LARGE = new Icon[]{new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/red_l.png")),
            new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/yellow_l.png")),
            new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/green_l.png"))};

    public static final Icon[] TRAFFIC_LIGHT_TINY = new Icon[]{new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/red_t.png")),
            new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/yellow_t.png")),
            new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/green_t.png"))};


    public static final Icon TRAFFIC_LIGHT_MEDIUM_GRAY = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/gray_m.png"));
    public static final Icon TRAFFIC_LIGHT_SMALL_GRAY = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/gray_s.png"));
    public static final Icon TRAFFIC_LIGHT_TINY_GRAY =  new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/gray_t.png"));
    public static final Icon[] TRAFFIC_LIGHT_MEDIUM = new Icon[]{new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/red_m.png")),
            new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/yellow_m.png")),
            new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/green_m.png"))};

    public static final Icon[] TRAFFIC_LIGHT_SMALL = new Icon[]{new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/red_s.png")),
            new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/yellow_s.png")),
            new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/green_s.png"))};


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

    public static Image scaledInstance(Image image, int longSide) {
        int w = ((BufferedImage) image).getWidth();
        int h = ((BufferedImage) image).getHeight();

        if (w > h) {
            h = (int) (h * ((double) w / (double) longSide));
            w = longSide;
        } else if (w < h) {
            w = (int) (w * ((double) h / (double) longSide));
            h = longSide;
        } else {
            w = longSide;
            h = longSide;
        }
        return scaledInstance(image, w, h);
    }
}
