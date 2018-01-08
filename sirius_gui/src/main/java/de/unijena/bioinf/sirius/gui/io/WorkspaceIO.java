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

import com.google.common.base.Function;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.fingerid.FingerIdData;
import de.unijena.bioinf.fingerid.FingerIdDataCSVExporter;
import de.unijena.bioinf.fingerid.FingerIdResultReader;
import de.unijena.bioinf.fingerid.FingerIdResultWriter;
import de.unijena.bioinf.fingerid.storage.ConfigStorage;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.dialogs.*;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedExportCSVFormatsFilter;
import de.unijena.bioinf.sirius.gui.mainframe.BatchImportDialog;
import de.unijena.bioinf.sirius.gui.mainframe.FileImportDialog;
import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.projectspace.*;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

public class WorkspaceIO {
    public static final SiriusSaveFileFilter SAVE_FILE_FILTER = new SiriusSaveFileFilter();

    public void newStore(List<ExperimentContainer> containers, File file) throws IOException {
        final DirectoryWriter.WritingEnvironment env;
        if (file.isDirectory()) {
            env = new SiriusFileWriter(file);
        } else {
            env = new SiriusWorkspaceWriter(file);
        }
        System.out.println("new io");
        final FingerIdResultWriter w = new FingerIdResultWriter(env);
        for (ExperimentContainer c : containers) {
            final Ms2Experiment exp = c.getMs2Experiment();
            w.writeExperiment(new ExperimentResult(exp, c.getRawResults()));
        }
        w.close();
    }

    public Queue<ExperimentContainer> newLoad(File file, Queue<ExperimentContainer> queue) throws IOException {
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

    public List<ExperimentContainer> load(File file) throws IOException {
        final ArrayDeque<ExperimentContainer> queue = new ArrayDeque<>();
        load(file, queue);
        return new ArrayList<>(queue);
    }

    public void load(File file, Queue<ExperimentContainer> queue) throws IOException {
        try (final ZipInputStream zin = new ZipInputStream(new FileInputStream(file))) {

            ZipEntry entry;
            final TreeMap<Integer, IdentificationResult> results = new TreeMap<>();
            Ms2Experiment currentExperiment = null;
            int currentExpId = -1;
            while ((entry = zin.getNextEntry()) != null) {
                final String name = entry.getName();
                if (name.endsWith("/")) {
                    if (currentExpId >= 0 && currentExperiment != null)
                        if (!queue.offer(new ExperimentContainer(currentExperiment, new ArrayList<>(results.values()))))
                            return;
                    currentExpId = Integer.parseInt(name.substring(0, name.length() - 1));
                    currentExperiment = null;
                    results.clear();
                } else if (name.endsWith(".ms")) {
                    currentExperiment = readZip(new JenaMsParser(), zin);
                } else if (name.endsWith(".json")) {
                    final int rank = Integer.parseInt(name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.')));
                    final FTree tree = readZip(new FTJsonReader(), zin);
                    final IdentificationResult idr = new IdentificationResult(tree, rank);
                    results.put(rank, idr);
                }
            }
            if (currentExpId >= 0 && currentExperiment != null)
                if (!queue.offer(new ExperimentContainer(currentExperiment, new ArrayList<>(results.values()))))
                    return;
        }
    }

    public void store(List<ExperimentContainer> containers, File file) throws IOException {
        try (final ZipOutputStream stream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            int k = 0;
            for (ExperimentContainer c : containers) {
                storeContainer(c, ++k, stream);
            }
        }
    }

    private void storeContainer(ExperimentContainer c, int i, ZipOutputStream stream) throws IOException {
        final String prefix = i + "/";
        final ZipEntry dir = new ZipEntry(prefix);
        stream.putNextEntry(dir);
        // write INPUT data
        final ZipEntry msfile = new ZipEntry(prefix + "experiment.ms");
        stream.putNextEntry(msfile);
        final Ms2Experiment exp = c.getMs2Experiment();
        stream.write(buffer(new Function<BufferedWriter, Void>() {
            @Override
            public Void apply(BufferedWriter input) {
                final JenaMsWriter writer = new JenaMsWriter();
                try {
                    writer.write(input, exp);
                } catch (IOException e) {
                    throw new RuntimeException();
                }
                return null;
            }
        }).getBytes(Charset.forName("UTF-8")));
        // if results available, write trees
        if (c.getRawResults() != null && !c.getRawResults().isEmpty()) {
            //final List<IdentificationResult> irs = c.getRawResults();
            for (final SiriusResultElement ir : c.getResults()) {
                final ZipEntry tree = new ZipEntry(prefix + ir.getRank() + ".json");
                stream.putNextEntry(tree);
                stream.write(buffer(new Function<BufferedWriter, Void>() {
                    @Override
                    public Void apply(BufferedWriter input) {
                        try {
                            new FTJsonWriter().writeTree(input, ir.getResult().getRawTree());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
                }).getBytes(Charset.forName("UTF-8")));
            }
        }
    }

    private static <T> T readZip(Parser<T> parser, ZipInputStream zin) throws IOException {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream(4096);
        final byte[] buf = new byte[4096];
        int c = 0;
        while ((c = zin.read(buf)) > 0) {
            bout.write(buf, 0, c);
        }
        return parser.parse(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bout.toByteArray()))), null);
    }

    private static String buffer(Function<BufferedWriter, Void> f) {
        try {
            final StringWriter sw = new StringWriter(1024);
            final BufferedWriter bw = new BufferedWriter(sw);
            f.apply(bw);
            bw.close();
            return sw.toString();
        } catch (IOException e) {
            assert false; // StringIO should not raise IO exceptions
            throw new RuntimeException(e);
        }
    }


    public static void importWorkspace(List<File> selFile) {
        ImportWorkspaceDialog workspaceDialog = new ImportWorkspaceDialog(MF);
        final WorkspaceWorker worker = new WorkspaceWorker(workspaceDialog, selFile);
        worker.execute();
        workspaceDialog.start();
        worker.flushBuffer();
        try {
            worker.get();
        } catch (InterruptedException | ExecutionException e1) {
            LoggerFactory.getLogger(Workspace.class).error(e1.getMessage(), e1);
        }
        worker.flushBuffer();
        if (worker.hasErrorMessage()) {
            new ErrorReportDialog(MF, worker.getErrorMessage());
        }
    }

    public static void exportResults() {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(ConfigStorage.CONFIG_STORAGE.getCsvExportPath());
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
                ConfigStorage.CONFIG_STORAGE.setCsvExportPath((selFile.exists() && selFile.isDirectory()) ? selFile : selFile.getParentFile());

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
                for (ExperimentContainer ec : Workspace.COMPOUNT_LIST) {
                    if (ec.isComputed() && ec.getResults().size() > 0) {
                        IdentificationResult.writeIdentifications(fw, ec.getMs2Experiment(), ec.getRawResults());
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

    private static void writeMultiFiles(File selectedFile, boolean withSirius, boolean withFingerid) throws IOException {
        final HashSet<String> names = new HashSet<>();
        for (ExperimentContainer container : Workspace.COMPOUNT_LIST) {
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
                    for (IdentificationResult result : container.getRawResults()) {
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

        Workspace.importCompounds(Workspace.toExperimentContainer(ecs));

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
            if (f.isDirectory()) {
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
            return name.endsWith(".sirius");
        }

        @Override
        public String getDescription() {
            return ".sirius";
        }

    }

}
