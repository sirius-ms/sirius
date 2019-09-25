/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.babelms.projectspace.GuiProjectSpace;
import de.unijena.bioinf.babelms.projectspace.SiriusProjectSpaceIO;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ErrorListDialog;
import de.unijena.bioinf.ms.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.ms.gui.mainframe.BatchImportDialog;
import de.unijena.bioinf.ms.gui.mainframe.FileImportDialog;
import org.jetbrains.annotations.NotNull;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

public class GuiProjectSpaceIO {
    public static final SiriusSaveFileFilter SAVE_FILE_FILTER = new SiriusSaveFileFilter();

    public enum ImportMode {REPLACE, MERGE}


    public static void importFromProjectSpace(@NotNull final ImportMode importMode, @NotNull final File... selFile) {
        importFromProjectSpace(importMode, Arrays.asList(selFile));
    }

    public static void importFromProjectSpace(@NotNull final Collection<File> selFile) {
        importFromProjectSpace(ImportMode.REPLACE, selFile);
    }

    public static void importFromProjectSpace(@NotNull final ImportMode importMode, @NotNull final Collection<File> selFile) {
        Jobs.runInBackroundAndLoad(MF, "Importing into Project-Space", (TinyBackgroundJJob) new TinyBackgroundJJob<Boolean>() {
            @Override
            protected Boolean compute() {
                GuiProjectSpace.PS.projectSpace.load((current, max, mess) -> updateProgress(0, max, current, mess), selFile);
                return true;
            }
        }.asIO());
    }


    public static void exportAsProjectSpace(File file) throws IOException {
        GuiProjectSpace.PS.writeSummary(); //this also writes chaged compounds first
        SiriusProjectSpaceIO.exortToZip(GuiProjectSpace.PS.projectSpace, file);
    }

    private static String escapeFileName(String name) {
        final String n = name.replaceAll("[:\\\\/*\"?|<>']", "");
        if (n.length() > 128) {
            return n.substring(0, 128);
        } else return n;
    }

    public static void importOneExperimentPerFile(File... files) {
        importOneExperimentPerFile(Arrays.asList(files));
    }

    public static void importOneExperimentPerFile(List<File> files) {
        FileImportDialog imp = new FileImportDialog(MF, files);
        importOneExperimentPerFile(imp.getMSFiles(), imp.getMGFFiles());
    }

    public static void importOneExperimentPerFile(List<File> msFiles, List<File> mgfFiles) {
        BatchImportDialog batchDiag = new BatchImportDialog(MF);
        batchDiag.start(msFiles, mgfFiles);

        List<Ms2Experiment> ecs = batchDiag.getResults();
        List<String> errors = batchDiag.getErrors();

        ecs.forEach(ec -> GuiProjectSpace.PS.importCompound((MutableMs2Experiment) ec));

        if (errors != null) {
            if (errors.size() > 1) {
                ErrorListDialog elDiag = new ErrorListDialog(MF, errors);
            } else if (errors.size() == 1) {
                ErrorReportDialog eDiag = new ErrorReportDialog(MF, errors.get(0));
            }

        }
    }

    public static File[] resolveFileList(File[] files) {
        List<File> l = resolveFileList(Arrays.asList(files));
        return l.toArray(new File[l.size()]);
    }

    public static List<File> resolveFileList(List<File> files) {
        final ArrayList<File> filelist = new ArrayList<>();
        for (File f : files) {
            if (f.isDirectory() && !SiriusProjectSpaceIO.isSiriusWorkspaceDirectory(f)) {
                final File[] fl = f.listFiles();
                if (fl != null) {
                    for (File g : fl)
                        if (!g.isDirectory()) filelist.add(g);
                }
            } else {
                filelist.add(f);
            }
        }
        return filelist;
    }


    public static class SiriusSaveFileFilter extends FileFilter {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) return true;
            String name = f.getName();
            return SiriusProjectSpaceIO.isCompressedProjectSpaceName(name);
        }

        @Override
        public String getDescription() {
            return ".sirius, .zip";
        }

    }

}
