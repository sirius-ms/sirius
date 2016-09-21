package de.unijena.bioinf.sirius.gui.ext;

import org.slf4j.LoggerFactory;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DragAndDrop {

    public static List<File> getFileListFromDrop(DropTargetDropEvent evt) {
        evt.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        DataFlavor easyOne = DataFlavor.javaFileListFlavor;
        final List<File> files;
        try {
        if (evt.isDataFlavorSupported(easyOne)) {
            files = (List<File>)(evt.getTransferable().getTransferData(easyOne));
        } else {
            files = new ArrayList<>();
            DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
            String droppedFiles = (String)
                    evt.getTransferable().getTransferData(nixFileDataFlavor);
            for (String s : droppedFiles.split("\n")) {
                if (!s.isEmpty() && !s.startsWith("#")) files.add(new File(URI.create(s).toURL().getFile()));
            }
        }
            return files;
        } catch (UnsupportedFlavorException | IOException | ClassNotFoundException e) {
            LoggerFactory.getLogger(DragAndDrop.class).error(e.getMessage(),e);
            return Collections.emptyList();
        }
    }

}
