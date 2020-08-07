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

package de.unijena.bioinf.ms.gui.dialogs.input;

import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import org.slf4j.LoggerFactory;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class DragAndDrop {

    public static List<File> getFileListFromDrop(DropTargetDropEvent evt) {
        evt.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

        try {
            DataFlavor easyOne = DataFlavor.javaFileListFlavor; //since java7 this works also on nix
            final List<File> files = (List<File>) (evt.getTransferable().getTransferData(easyOne));
            final File last = files.get(files.size() - 1);
            if (!Files.exists(last.toPath()))//string of all file names can be to long
                throw new ToManyFilesException();
            return files;
        } catch (UnsupportedFlavorException | IOException e) {
            LoggerFactory.getLogger(DragAndDrop.class).error(e.getMessage(), e);
            return Collections.emptyList();
        } catch (ToManyFilesException e) {
            new ExceptionDialog(MainFrame.MF, e.getMessage());
            LoggerFactory.getLogger(DragAndDrop.class).error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private static class ToManyFilesException extends Exception {
        private static final String DEF_MESSAGE = "Too many Files dropped. You cannot import so many files via dag'n'drop. Please use the Batch import File browser instead.";

        public ToManyFilesException() {
            super(DEF_MESSAGE);
        }

        public ToManyFilesException(String message) {
            super(message);
        }
    }
}
