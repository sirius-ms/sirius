package de.unijena.bioinf.sirius.gui.ext;

import de.unijena.bioinf.sirius.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
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
