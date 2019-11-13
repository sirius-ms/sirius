package de.unijena.bioinf.babelms.filefilter;

import de.unijena.bioinf.projectspace.ProjectSpaceIO;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class SupportedArchivedProjectFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) return true;
        return ProjectSpaceIO.isCompressedProjectSpace(f);
    }

    @Override
    public String getDescription() {
        return ".sirius, .zip";
    }
}