package de.unijena.bioinf.sirius.gui.mainframe;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 27.01.17.
 */

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.configs.ConfigStorage;
import de.unijena.bioinf.sirius.gui.dialogs.*;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedExportCSVFormatsFilter;
import de.unijena.bioinf.sirius.gui.fingerid.CSVExporter;
import de.unijena.bioinf.sirius.gui.fingerid.FingerIdData;
import de.unijena.bioinf.sirius.gui.io.SiriusDataConverter;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class Workspace {
    //todo remove all the dialogs from here!!!
    public static final ConfigStorage CONFIG_STORAGE = new ConfigStorage();//todo generic file path remembering
    public static final SiriusSaveFileFilter SAVE_FILE_FILTER = new SiriusSaveFileFilter();
    public static final BasicEventList<ExperimentContainer> COMPOUNT_LIST = new BasicEventList<>();
    private static final HashSet<String> NAMES = new HashSet<>();


    public static void clearWorkspace() {
        NAMES.clear();
        COMPOUNT_LIST.clear();
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
        jfc.setCurrentDirectory(CONFIG_STORAGE.getCsvExportPath());
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
                CONFIG_STORAGE.setCsvExportPath((selFile.exists() && selFile.isDirectory()) ? selFile : selFile.getParentFile());

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
                for (ExperimentContainer ec : COMPOUNT_LIST) {
                    if (ec.isComputed() && ec.getResults().size() > 0) {
                        IdentificationResult.writeIdentifications(fw, SiriusDataConverter.experimentContainerToSiriusExperiment(ec), ec.getRawResults());
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
        for (ExperimentContainer container : COMPOUNT_LIST) {
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
                new CSVExporter().exportToFile(resultFile, datas);
            }
        }
    }

    private static String escapeFileName(String name) {
        final String n = name.replaceAll("[:\\\\/*\"?|<>']", "");
        if (n.length() > 128) {
            return n.substring(0, 128);
        } else return n;
    }

    public static void importOneExperimentPerFile(List<File> msFiles, List<File> mgfFiles) {
        BatchImportDialog batchDiag = new BatchImportDialog(MF);
        batchDiag.start(msFiles, mgfFiles);

        List<ExperimentContainer> ecs = batchDiag.getResults();
        List<String> errors = batchDiag.getErrors();
        importOneExperimentPerFileStep2(ecs, errors);
    }

    public static void importOneExperimentPerFile(File[] files) {
        BatchImportDialog batchDiag = new BatchImportDialog(MF);
        batchDiag.start(resolveFileList(files));

        List<ExperimentContainer> ecs = batchDiag.getResults();
        List<String> errors = batchDiag.getErrors();
        importOneExperimentPerFileStep2(ecs, errors);
    }

    public static File[] resolveFileList(File[] files) {
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
        return filelist.toArray(new File[filelist.size()]);
    }

    public static void importOneExperimentPerFileStep2(List<ExperimentContainer> ecs, List<String> errors) {
        if (ecs != null) {
            for (ExperimentContainer ec : ecs) {
                if (ec == null) {
                    continue;
                } else {
                    importCompound(ec);
                }
            }
        }


        if (errors != null) {
            if (errors.size() > 1) {
                ErrorListDialog elDiag = new ErrorListDialog(MF, errors);
            } else if (errors.size() == 1) {
                ErrorReportDialog eDiag = new ErrorReportDialog(MF, errors.get(0));
            }

        }
    }

    public static void importCompound(final ExperimentContainer ec) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                resolveCompundNameConflict(ec);
                COMPOUNT_LIST.add(ec);
                if (ec.getResults().size() > 0) ec.setComputeState(ComputingStatus.COMPUTED);
            }
        });
    }

    public static void resolveCompundNameConflict(ExperimentContainer ec) {
        while (true) {
            if (ec.getGUIName() != null && !ec.getGUIName().isEmpty()) {
                if (NAMES.contains(ec.getGUIName())) {
                    ec.setSuffix(ec.getSuffix() + 1);
                } else {
                    NAMES.add(ec.getGUIName());
                    break;
                }
            } else {
                ec.setName("Unknown");
                ec.setSuffix(1);
            }
        }
    }

    public static void removeAll(List<ExperimentContainer> containers) {
        for (ExperimentContainer container : containers) {
            NAMES.remove(container.getGUIName());
        }
        COMPOUNT_LIST.removeAll(containers);
    }


    public static class SiriusSaveFileFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) return true;
            String name = f.getName();
            if (name.endsWith(".sirius")) {
                return true;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return ".sirius";
        }

    }
}
