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
 * 11.10.16.
 */

import de.unijena.bioinf.ms.gui.dialogs.AboutDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class Icons {
    //ICONS


    public static final ImageIcon SPLASH = new ImageIcon(MainFrame.class.getResource("/icons/sirius_splash.gif"));

    public static final Icon FP_LOADER = new ImageIcon(MainFrame.class.getResource("/icons/fp-binary-sirius.gif"));

    public static final Icon NO_MATCH_128 = new ImageIcon(MainFrame.class.getResource("/icons/nothing-found.png"));

    public static final Icon NO_16 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-no-16px.png"));

    public static final Icon YES_16 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-yes-16px.png"));

    public static final Icon FB_LOADER_RUN_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/fb_loader.gif"));
    public static final Icon FB_LOADER_STOP_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/fb_loader.png"));

    public static final Icon NET_NO_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-network-no@0.5x.png"));
    public static final Icon NET_YES_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-network-yes@0.5x.png"));
    public static final Icon NET_WARN_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-network-warn@0.5x.png"));
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
    public static final Icon DB_64 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-db.png"));

    public static final Icon GEAR_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-gear@0.5x.png"));
    public static final Icon GEAR_64 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-gear.png"));

    public static final Icon LOG_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-log@0.5x.png"));
    public static final Icon LOG_64 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-log.png"));

    public static final Icon CLIP_BOARD_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-clipboard@0.5x.png"));

    public static final Icon BUG_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-bug@0.5x.png"));
    public static final Icon BUG_64 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-bug.png"));

    public static final Icon INFO_32 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-information@0.5x.png"));
    public static final Icon CHOOSE_FILE_16 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-folder-file-16px.png"));


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

    public static final Icon SIRIUS_SPLASH = new ImageIcon(AboutDialog.class.getResource("/icons/sirius.jpg"));
    public static final Icon SIRIUS_APP_ICON = new ImageIcon(Icons.class.getResource("/icons/sirius_icon.png"));
    public static final Image SIRIUS_APP_IMAGE = Toolkit.getDefaultToolkit().createImage(Icons.class.getResource("/icons/sirius_icon.png"));

    public static final Icon MolecularProperty_24 = new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-molecularPropertyWhite-24px.png"));

}
