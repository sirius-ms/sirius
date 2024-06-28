/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.jjobs.LoadingBackroundTask;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.ParameterBinding;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.ImportPeakListDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.LCMSDialog;
import de.unijena.bioinf.ms.gui.io.filefilter.MsBatchDataFormatFilter;
import de.unijena.bioinf.ms.gui.io.filefilter.ProjectArchivedFilter;
import de.unijena.bioinf.ms.nightsky.sdk.jjobs.SseProgressJJob;
import de.unijena.bioinf.ms.nightsky.sdk.model.DataSmoothing;
import de.unijena.bioinf.ms.nightsky.sdk.model.Job;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobOptField;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceImporter;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author Markus Fleischauer
 */
public class ImportAction extends AbstractGuiAction {

    public ImportAction(SiriusGui gui) {
        super("Import", gui);
        putValue(Action.LARGE_ICON_KEY, Icons.DOCS_32);
        putValue(Action.SMALL_ICON, Icons.BATCH_DOC_16);
        putValue(Action.SHORT_DESCRIPTION, "<html>" +
                "<p>Import measurements of:</p>" +
                "<ul style=\"list-style-type:none;\">" +
                "  <li>- Multiple compounds (e.g. .ms, .mgf)</li>" +
                "  <li>- LC-MS/MS runs (.mzML, .mzXml)</li>" +
                "  <li>- Projects (.sirius)</li>" +
                "</ul>" +
                "<p>into the current project-space. (Same as drag and drop)</p>" +
                "</html>");
    }

    //ATTENTION Synchronizing around background tasks that block gui thread is dangerous
    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser(PropertyManager.getFile(SiriusProperties.DEFAULT_LOAD_DIALOG_PATH));
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(true);
        chooser.addChoosableFileFilter(new MsBatchDataFormatFilter());
        chooser.addChoosableFileFilter(new ProjectArchivedFilter());
        chooser.setAcceptAllFileFilterUsed(false);
        int returnVal = chooser.showDialog(mainFrame, "Import");

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            if (files.length > 0) {
                SiriusProperties.
                        setAndStoreInBackground(SiriusProperties.DEFAULT_LOAD_DIALOG_PATH, files[0].getParentFile().getAbsolutePath());

                importOneExperimentPerLocation(List.of(files), mainFrame);
            }
        }
    }

    //ATTENTION Synchronizing around background tasks that block gui thread is dangerous
    public synchronized void importOneExperimentPerLocation(@NotNull final List<File> inputFiles, Window popupOwner) {
        final InputFilesOptions inputF = new InputFilesOptions();
        inputF.msInput = Jobs.runInBackgroundAndLoad(popupOwner, "Analyzing Files...", false,
                InstanceImporter.makeExpandFilesJJob(inputFiles)).getResult();
        importOneExperimentPerLocation(inputF, popupOwner);
    }

    //ATTENTION Synchronizing around background tasks that block gui thread is dangerous
    public synchronized void importOneExperimentPerLocation(@NotNull final InputFilesOptions input, Window popupOwner) {
        Map<Boolean, List<Path>> paths = Jobs.runInBackgroundAndLoad(
                popupOwner, "Analyzing input...",
                () -> input.msInput.msParserfiles.keySet().stream().collect(Collectors.partitioningBy(p -> {
                    String fileName = p.getFileName().toString().toLowerCase();
                    return fileName.endsWith(".mzml") || fileName.endsWith(".mzxml");
                }))
        ).getResult();

        StopWatch watch = new StopWatch();
        watch.start();

        try {
            LoadingBackroundTask<Job> task = null;
            // handle LC/MS files
            if (paths.containsKey(true) && !paths.get(true).isEmpty()) {
                List<Path> lcmsPaths = paths.get(true);
                LCMSDialog dialog = new LCMSDialog(popupOwner, lcmsPaths.size() > 1);
                if (dialog.isSuccess()) {
                    ParameterBinding binding = dialog.getParamterBinging();
                    String runType = binding.getOrDefault("tag", () -> null).get();
                    boolean align = binding.getOrDefault("align", () -> "~true").get().equals("~true");
                    DataSmoothing filter = DataSmoothing.valueOf(binding.getOrDefault("filter", () -> "AUTO").get());
                    double sigma = Double.parseDouble(binding.getOrDefault("sigma", () -> "3.0").get());
                    int scale = Integer.parseInt(binding.getOrDefault("scale", () -> "20").get());
                    double window = Double.parseDouble(binding.getOrDefault("window", () -> "11").get());
                    double noise = Double.parseDouble(binding.getOrDefault("noise", () -> "2.0").get());
                    double persistence = Double.parseDouble(binding.getOrDefault("persistence", () -> "0.1").get());
                    double merge = Double.parseDouble(binding.getOrDefault("merge", () -> "0.8").get());

                    task = gui.applySiriusClient((c, pid) -> {
                        Job job = c.projects().importMsRunDataAsJobLocally(pid,
                                lcmsPaths.stream().map(Path::toAbsolutePath).map(Path::toString).toList(),
                                runType,
                                align && lcmsPaths.size() > 1,
                                PropertyManager.getBoolean("de.unijena.bioinf.sirius.ui.allowMs1Only", false),
                                filter, sigma, scale, window, noise, persistence, merge,
                                List.of(JobOptField.PROGRESS)
                        );
                        return LoadingBackroundTask.runInBackground(gui.getMainFrame(), "Importing LC-MS runs...", null, new SseProgressJJob(gui.getSiriusClient(), pid, job));
                    });
                }
            }

            // handle non-LC/MS files
            if (paths.containsKey(false) && !paths.get(false).isEmpty()) {
                if (new ImportPeakListDialog(gui.getMainFrame()).isSuccess()) {
                    task = gui.applySiriusClient((c, pid) -> {
                        Job job = c.projects().importPreprocessedDataAsJobLocally(pid,
                                paths.get(false).stream().map(Path::toAbsolutePath).map(Path::toString).toList(),
                                PropertyManager.getBoolean("de.unijena.bioinf.sirius.ui.ignoreFormulas", false),
                                PropertyManager.getBoolean("de.unijena.bioinf.sirius.ui.allowMs1Only", true),
                                List.of(JobOptField.PROGRESS)
                        );
                        return LoadingBackroundTask.runInBackground(gui.getMainFrame(), "Importing...", null, new SseProgressJJob(gui.getSiriusClient(), pid, job));
                    });
                }
            }

            if (task != null)
                task.awaitResult();
        } catch (ExecutionException e) {
            new StacktraceDialog(gui.getMainFrame(), "Error when importing data! Cause: " + e.getMessage(), e.getCause());
        }finally {
            System.out.println("=============> Importing took: " + watch);
        }
    }
}
