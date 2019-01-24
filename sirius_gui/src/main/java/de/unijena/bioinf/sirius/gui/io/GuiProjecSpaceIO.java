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

package de.unijena.bioinf.sirius.gui.io;

import com.google.common.collect.Lists;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.fingerid.FingerIdData;
import de.unijena.bioinf.ms.projectspace.GuiProjectSpace;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.core.SiriusProperties;
import de.unijena.bioinf.sirius.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.sirius.gui.dialogs.*;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedExportCSVFormatsFilter;
import de.unijena.bioinf.sirius.gui.mainframe.BatchImportDialog;
import de.unijena.bioinf.sirius.gui.mainframe.FileImportDialog;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

public class GuiProjecSpaceIO {
    public static final SiriusSaveFileFilter SAVE_FILE_FILTER = new SiriusSaveFileFilter();



    public static Queue<ExperimentContainer> newLoad(File file, Queue<ExperimentContainer> queue) throws IOException {
        final DirectoryReader.ReadingEnvironment env;
        if (file.isDirectory()) {
            env = new SiriusFileReader(file);
        } else {
            env = new SiriusWorkspaceReader(file);
        }
        final FingerIdResultReader reader = new FingerIdResultReader(env);

        while (reader.hasNext()) {
            final ExperimentResult result = reader.next();
            queue.add(new ExperimentContainer(result.getExperiment(), result.getResults()));
        }
        return queue;
    }


    public static void importProjectSpace(List<File> selFile) {
        ImportWorkspaceDialog workspaceDialog = new ImportWorkspaceDialog(MF);
        final WorkspaceWorker worker = new WorkspaceWorker(workspaceDialog, selFile);
        worker.execute();
        workspaceDialog.start();
        worker.flushBuffer();
        try {
            worker.get();
        } catch (InterruptedException | ExecutionException e1) {
            LoggerFactory.getLogger(GuiProjecSpaceIO.class).error(e1.getMessage(), e1);
        }
        worker.flushBuffer();
        if (worker.hasErrorMessage()) {
            new ErrorReportDialog(MF, worker.getErrorMessage());
        }
    }

    public static void exportProjectSpace(List<ExperimentContainer> containers, File file) throws IOException {

        final DirectoryWriter.WritingEnvironment env;
        if (file.isDirectory()) {
            env = new SiriusFileWriter(file);
        } else {
            env = new SiriusWorkspaceWriter(file);
        }
        final FingerIdResultWriter w = new FingerIdResultWriter(env, new StandardMSFilenameFormatter());
        for (ExperimentContainer c : containers) {
            final Ms2Experiment exp = c.getMs2Experiment();
            w.writeExperiment(new ExperimentResult(exp, Lists.newArrayList(c.getRawResults())));
        }
        w.close();
    }

    public static void exportProjectToCSV() {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(PropertyManager.getFile(SiriusProperties.CSV_EXPORT_PATH));
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(new SupportedExportCSVFormatsFilter());

        final ExporterAccessory accessory = new ExporterAccessory(jfc);
        jfc.setAccessory(accessory);

        File selectedFile = null;

        while (selectedFile == null) {
            int returnval = jfc.showSaveDialog(MF);
            if (returnval == JFileChooser.APPROVE_OPTION) {
                File selFile = jfc.getSelectedFile();
                if (selFile == null) continue;

                {
                    final String path = (selFile.exists() && selFile.isDirectory()) ? selFile.getAbsolutePath() : selFile.getParentFile().getAbsolutePath();
                    Jobs.runInBackround(() ->
                            SiriusProperties.SIRIUS_PROPERTIES_FILE().
                                    setAndStoreProperty(SiriusProperties.DEFAULT_TREE_EXPORT_PATH, path)
                    );
                }

                if (accessory.isSingleFile()) {
                    String name = selFile.getName();
                    if (!name.endsWith(".csv") && !name.endsWith(".tsv")) {
                        selFile = new File(selFile.getAbsolutePath() + ".csv");
                    }

                    if (selFile.exists()) {
                        FilePresentDialog fpd = new FilePresentDialog(MF, selFile.getName());
                        ReturnValue rv = fpd.getReturnValue();
                        if (rv == ReturnValue.Success) {
                            selectedFile = selFile;
                        }
                    } else {
                        selectedFile = selFile;
                    }

                } else {
                    if (!selFile.exists()) {
                        selFile.mkdirs();
                    }
                }
                selectedFile = selFile;
                break;
            } else {
                break;
            }
        }

        if (selectedFile == null) return;
        if (accessory.isSingleFile()) {
            try (final BufferedWriter fw = new BufferedWriter(new FileWriter(selectedFile))) {

                for (ExperimentContainer ec : GuiProjectSpace.PS.COMPOUNT_LIST) {
                    if (ec.isComputed() && ec.getResults().size() > 0) {
                        writeIdentificationResults(fw, ec.getExperimentResult());
                    }
                }
            } catch (IOException e) {
                new ErrorReportDialog(MF, e.toString());
            }
        } else {
            try {
                writeMultiFiles(selectedFile, accessory.isExportingSirius(), accessory.isExportingFingerId());
            } catch (IOException e) {
                new ErrorReportDialog(MF, e.toString());
            }
        }
    }

    private static void writeIdentificationResults(Writer writer, ExperimentResult expResult) throws IOException {
        final StringBuilder buffer = new StringBuilder();
        final List<IdentificationResult> results = expResult.getResults();
        String name = expResult.getExperimentName();
        // escape quotation marks
        {
            if (name.indexOf('"') >= 0) {
                name = "\"" + name.replaceAll("\"", "\"\"") + "\"";
            } else if (name.indexOf('\t') >= 0 || name.indexOf('\n') >= 0) {
                name = "\"" + name + "\"";
            }
        }
        buffer.append(name);
        buffer.append('\t');
        buffer.append(expResult.getExperiment().getIonMass());
        buffer.append('\t');
        buffer.append(expResult.getExperiment().getPrecursorIonType().toString());
        for (IdentificationResult r : results) {
            buffer.append('\t');
            buffer.append(r.getMolecularFormula().toString());
            buffer.append('\t');
            buffer.append(r.getScore());
        }
        buffer.append('\n');
        writer.write(buffer.toString());
    }

    private static void writeMultiFiles(File selectedFile, boolean withSirius, boolean withFingerid) throws IOException {
        final HashSet<String> names = new HashSet<>();
        for (ExperimentContainer container : GuiProjectSpace.PS.COMPOUNT_LIST) {
            if (container.getResults() == null || container.getResults().size() == 0) continue;
            final String name;
            {
                String origName = escapeFileName(container.getName());
                String aname = origName;
                int i = 1;
                while (names.contains(aname)) {
                    aname = origName + "(" + (++i) + ")";
                }
                name = aname;
                names.add(name);
            }

            if (withSirius) {

                final File resultFile = new File(selectedFile, name + "_formula_candidates.csv");
                try (final BufferedWriter bw = Files.newBufferedWriter(resultFile.toPath(), Charset.defaultCharset())) {
                    bw.write("formula\trank\tscore\ttreeScore\tisoScore\texplainedPeaks\texplainedIntensity\n");
                    for (IdentificationResult result : container.getExperimentResult().getResults()) {
                        bw.write(result.getMolecularFormula().toString());
                        bw.write('\t');
                        bw.write(String.valueOf(result.getRank()));
                        bw.write('\t');
                        bw.write(String.valueOf(result.getScore()));
                        bw.write('\t');
                        bw.write(String.valueOf(result.getTreeScore()));
                        bw.write('\t');
                        bw.write(String.valueOf(result.getIsotopeScore()));
                        bw.write('\t');
                        final TreeScoring scoring = result.getResolvedTree().getAnnotationOrNull(TreeScoring.class);
                        bw.write(String.valueOf(result.getResolvedTree().numberOfVertices()));
                        bw.write('\t');
                        bw.write(scoring == null ? "\"\"" : String.valueOf(scoring.getExplainedIntensity()));
                        bw.write('\n');
                    }
                }
            }
            if (withFingerid) {
                final ArrayList<FingerIdData> datas = new ArrayList<>();
                for (SiriusResultElement elem : container.getResults()) {
                    if (elem.getFingerIdData() == null) continue;
                    datas.add(elem.getFingerIdData());
                }
                final File resultFile = new File(selectedFile, name + ".csv");
                new FingerIdDataCSVExporter().exportToFile(resultFile, datas);
            }
        }
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
            if (f.isDirectory() && !isSiriusWorkspaceDirectory(f)) {
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

    public static boolean isSiriusWorkspaceDirectory(File f) {
        final File fv = new File(f, "version.txt");
        if (!fv.exists()) return false;
        try (final BufferedReader br = new BufferedReader(new FileReader(fv), 512)) {
            String line = br.readLine();
            if (line == null) return false;
            line = line.toUpperCase();
            if (line.startsWith("SIRIUS")) return true;
            else return false;
        } catch (IOException e) {
            // not critical: if file cannot be read, it is not a valid workspace
            LoggerFactory.getLogger(GuiProjecSpaceIO.class).error(e.getMessage(), e);
            return false;
        }
    }

    public static class SiriusSaveFileFilter extends FileFilter {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) return true;
            String name = f.getName();
            return name.endsWith(".sirius");
        }

        @Override
        public String getDescription() {
            return ".sirius";
        }

    }

}
