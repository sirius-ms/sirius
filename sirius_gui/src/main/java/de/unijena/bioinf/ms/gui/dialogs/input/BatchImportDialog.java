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

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.ms.frontend.io.DataFormat;
import de.unijena.bioinf.ms.frontend.io.DataFormatIdentifier;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BatchImportDialog extends JDialog implements ActionListener {

    private JProgressBar progBar;
    ImportExperimentsThread importThread;
    private JButton abort;
    private JLabel progressl;
    private ReturnValue rv;
    private AnalyseFileTypesThread analyseThread;
    private List<String> errors;

    public BatchImportDialog(Dialog owner) {
        super(owner, true);
        init();
    }

    public BatchImportDialog(JFrame owner) {
        super(owner, true);
        init();
    }

    private void init() {
        this.setTitle("Batch Import");
        this.rv = ReturnValue.Abort;
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEtchedBorder());
        this.add(centerPanel, BorderLayout.CENTER);

        progBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        progBar.setValue(0);

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.add(progBar, BorderLayout.NORTH);
        progressl = new JLabel("");
        JPanel progressLabelPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        progressLabelPanel.add(progressl);
        progressPanel.add(progressLabelPanel, BorderLayout.SOUTH);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        centerPanel.add(progressPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        abort = new JButton("Abort");
        abort.addActionListener(this);
        buttonPanel.add(abort);
        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void start(List<File> msFiles, List<File> mgfFiles) {
        errors = new ArrayList<>();
        importThread = new ImportExperimentsThread(msFiles, mgfFiles, this, errors);
        Thread t = new Thread(importThread);
        t.start();
        this.setSize(new Dimension(300, 125));
        setLocationRelativeTo(getParent());
        this.setVisible(true);
    }

    public List<Ms2Experiment> getResults() {
        if (this.rv == ReturnValue.Abort) return Collections.emptyList();
        else return this.importThread.getResults();
    }

    public List<String> getErrors() {
        return this.errors;
    }

    public ReturnValue wasSucessful() {
        return this.rv;
    }

    /////// File import ///////////

    void fileImportInit(int maxVal) {
        progBar.setMaximum(maxVal);
        progBar.setMinimum(0);
        progBar.setValue(0);
        progressl.setText("");
    }

    void fileImportUpdate(int currentIndex, String currentFileName) {
        progBar.setValue(currentIndex);
        progressl.setText("import \"" + currentFileName + "\"");
    }

    void fileImportFinished() {
        this.rv = ReturnValue.Success;
        this.dispose();
    }

    void fileImportAborted() {
        this.rv = ReturnValue.Abort;
        this.dispose();
    }

    ///////////// fuer DF Analyse ////////////////////

    void fileAnalysisInit(int maxVal) {
        progBar.setMaximum(maxVal);
        progBar.setMinimum(0);
        progBar.setValue(0);
        progressl.setText("Analysing data formats ...");
    }


    void FileAnaysisUpdate(int currentIndex) {
        progBar.setValue(currentIndex);
    }

    void fileAnalysisAborted() {
        this.rv = ReturnValue.Abort;
        this.dispose();
    }

    void fileAnalysisFinished() {
        List<File> msFiles = this.analyseThread.getMSFiles();
        List<File> mgfFiles = this.analyseThread.getMGFFiles();

        this.importThread = new ImportExperimentsThread(msFiles, mgfFiles, this, this.errors);
        Thread t = new Thread(importThread);
        t.start();
    }


    //// Action Listener ////

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.abort) {
            if (analyseThread != null) analyseThread.abortProgress();
            if (importThread != null) importThread.abortImport();
            this.dispose();
        }

    }

}

class AnalyseFileTypesThread implements Runnable {

    private File[] files;
    private List<File> msFiles, mgfFiles;
    private volatile boolean stop;
    private BatchImportDialog bid;
    private List<String> errors;


    public AnalyseFileTypesThread(File[] files, BatchImportDialog bid, List<String> errors) {
        this.files = files;
        this.bid = bid;
        stop = false;
        msFiles = new ArrayList<>();
        mgfFiles = new ArrayList<>();
        this.errors = errors;
    }

    void abortProgress() {
        this.stop = true;
    }

    List<File> getMSFiles() {
        return this.msFiles;
    }

    List<File> getMGFFiles() {
        return this.mgfFiles;
    }

    @Override
    public void run() {
        DataFormatIdentifier dfi = new DataFormatIdentifier();
        int counter = 0;
        this.bid.fileAnalysisInit(files.length);
        for (File f : files) {
            if (this.stop) {
                Jobs.runEDTLater(() -> bid.fileAnalysisAborted());
                return;
            }
            final int currentCounter = counter;
            Jobs.runEDTLater(() -> bid.FileAnaysisUpdate(currentCounter));
            DataFormat df = dfi.identifyFormat(f);
            if (df == DataFormat.JenaMS) {
                msFiles.add(f);
            } else if (df == DataFormat.MGF) {
                mgfFiles.add(f);
            } else if (df == DataFormat.NotSupported) {
                this.errors.add(f.getName() + ": unsupported file format.");
            }
            counter++;
        }

        Jobs.runEDTLater(() -> bid.fileAnalysisFinished());
    }

}

class ImportExperimentsThread implements Runnable {

    private List<File> msFiles, mgfFiles;
    private List<Ms2Experiment> results;
    private volatile boolean stop;
    private List<String> errors;
    private BatchImportDialog bid;

    ImportExperimentsThread(List<File> msFiles, List<File> mgfFiles, BatchImportDialog bid, List<String> errors) {
        this.msFiles = msFiles;
        this.mgfFiles = mgfFiles;
        this.results = new ArrayList<>();
        this.errors = errors;
        this.stop = false;
        this.bid = bid;
    }

    void abortImport() {
        this.stop = true;
    }

    @Override
    public void run() {
        final MsExperimentParser parser = new MsExperimentParser();

        final int size = msFiles.size() + mgfFiles.size();
        this.results = new ArrayList<>(size);
        Jobs.runEDTLater(() -> bid.fileImportInit(size));
        int counter = 0;


        for (final File f : msFiles) {
            final int currentCounter = counter;
            Jobs.runEDTLater(() -> bid.fileImportUpdate(currentCounter, f.getName()));

            try (final CloseableIterator<Ms2Experiment> experiments = parser.getParser(f).parseFromFileIterator(f)) {
                addToResults(experiments);
            } catch (RuntimeException | IOException e) {
                errors.add(f.getName() + ": Invalid file format.");
                errors.add(e.getMessage());
            }
            counter++;
        }

        for (final File f : mgfFiles) {
            final int currentCounter = counter;
            Jobs.runEDTLater(() -> bid.fileImportUpdate(currentCounter, f.getName()));

            try (final CloseableIterator<Ms2Experiment> experiments = parser.getParser(f).parseFromFileIterator(f)) {
                addToResults(experiments);
            } catch (IOException e) {
                errors.add(f.getName() + ": Invalid file format.");
            }
            counter++;
        }
        Jobs.runEDTLater(() -> bid.fileImportFinished());
    }

    private void addToResults(final CloseableIterator<Ms2Experiment> experiments) {
        while (experiments.hasNext()) {
            final Ms2Experiment exp = experiments.next();
//            final ExperimentContainer ec = new ExperimentContainer(exp);
            results.add(exp);
            if (stop) {
                Jobs.runEDTLater(() -> bid.fileImportAborted());
                return;
            }
        }
    }

    List<Ms2Experiment> getResults() {
        return this.results;
    }


}
