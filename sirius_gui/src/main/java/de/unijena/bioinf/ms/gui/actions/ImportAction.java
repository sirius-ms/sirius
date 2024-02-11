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
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.io.filefilter.MsBatchDataFormatFilter;
import de.unijena.bioinf.ms.gui.io.filefilter.ProjectArchivedFilter;
import de.unijena.bioinf.ms.nightsky.sdk.jjobs.SseProgressJJob;
import de.unijena.bioinf.ms.nightsky.sdk.model.ImportLocalFilesSubmission;
import de.unijena.bioinf.ms.nightsky.sdk.model.Job;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobOptField;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceImporter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
                "  <li>- Multiple compounds (.ms, .mgf)</li>" +
                "  <li>- LC-MS/MS runs (.mzML, .mzXml)</li>" +
                "  <li>- Projects (.sirius, directory)</li>" +
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
        boolean align = Jobs.runInBackgroundAndLoad(popupOwner, "Checking for alignable input...", () ->
                        (input.msInput.msParserfiles.size() > 1 && input.msInput.projects.size() == 0 && input.msInput.msParserfiles.keySet().stream().map(p -> p.getFileName().toString().toLowerCase()).allMatch(n -> n.endsWith(".mzml") || n.endsWith(".mzxml"))))
                .getResult();

        // todo this is hacky we need some real view for that at some stage.
        if (align)
            align = new QuestionDialog(popupOwner, "<html><body> You inserted multiple LC-MS/MS Runs. <br> Do you want to Align them during import?</br></body></html>"/*, DONT_ASK_OPEN_KEY*/).isSuccess();

        ImportLocalFilesSubmission sub = new ImportLocalFilesSubmission() //todo nightsky: add this parameters to import dialog with defaults set...
                .inputPaths(input.msInput.msParserfiles.keySet().stream().map(Path::toAbsolutePath).map(Path::toString).toList())
                .allowMs1OnlyData(PropertyManager.getBoolean("de.unijena.bioinf.sirius.ui.allowMs1Only", true))
                .ignoreFormulas(PropertyManager.getBoolean("de.unijena.bioinf.sirius.ui.ignoreFormulas", false))
                .alignLCMSRuns(align);

        try {
            LoadingBackroundTask<Job> task = gui.applySiriusClient((c, pid) -> {
                Job job = c.jobs().startImportFromPathJob(pid, sub, List.of(JobOptField.PROGRESS));
                return LoadingBackroundTask.runInBackground(gui.getMainFrame(), "Auto-Importing supported Files...", null, new SseProgressJJob(gui.getSiriusClient(), pid, job));
            });

            task.awaitResult();
        } catch (ExecutionException e) {
            new StacktraceDialog(gui.getMainFrame(), "Error when imorting data!", e);
        }
        //todo nightsky implement project space import with compatibility checks
//            if (align) {
//                //todo would be nice to update all at once!
//                final LcmsAlignSubToolJob j = new LcmsAlignSubToolJob(input, this, null, new LcmsAlignOptions());
//                Jobs.runInBackgroundAndLoad(popupOwner, j);
//                INSTANCE_LIST.addAll(j.getImportedCompounds().stream()
//                        .map(this::getInstanceFromCompound)
//                        .toList());
//            } else {

//                Jobs.runInBackgroundAndLoad(popupOwner, "Checking for projects data...", new TinyBackgroundJJob<List<Path>>() {
//                    @Override
//                    protected List<Path> compute() throws Exception {
//                        if (input.msInput.projects.size() == 0)
//                            return List.of();
//                        final List<Path> out = new ArrayList<>(input.msInput.projects.size());
//
//                        for (Path p : input.msInput.projects.keySet()) {
//                            gui.getSiriusClient().projects().openProjectSpace(null, p.normalize().toString()).
//
//                            if (InstanceImporter.checkDataCompatibility(p, GuiProjectSpaceManager.this, this::checkForInterruption) != null)
//                                out.add(p);
//                        }
//                        return out;
//                    }
//                }).getResult();

                /*final List<Path> outdated = Jobs.runInBackgroundAndLoad(popupOwner, "Checking for incompatible data...", new TinyBackgroundJJob<List<Path>>() {
                    @Override
                    protected List<Path> compute() throws Exception {
                        if (input.msInput.projects.size() == 0)
                            return List.of();
                        final List<Path> out = new ArrayList<>(input.msInput.projects.size());
                        for (Path p : input.msInput.projects.keySet()) {
                            if (InstanceImporter.checkDataCompatibility(p, GuiProjectSpaceManager.this, this::checkForInterruption) != null)
                                out.add(p);
                        }
                        return out;
                    }
                }).getResult();

                boolean updateIfNeeded = !outdated.isEmpty() && new QuestionDialog(popupOwner, GuiUtils.formatToolTip(
                        "The following input projects are incompatible with the target", "'" + this.projectSpace().getLocation() + "'", "",
                        outdated.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.joining(",")), "",
                        "Do you wish to import and update the fingerprint data?", "WARNING: All fingerprint related results will be excluded during import (CSI:FingerID, CANOPUS)")).isSuccess();

                InstanceImporter importer = new InstanceImporter(this,
                        x -> {
                            if (x.getPrecursorIonType() != null) {
                                return true;
                            } else {
                                LOG.warn("Skipping `" + x.getName() + "` because of Missing IonType! This is likely to be A empty Measurement.");
                                return false;
                            }
                        },
                        x -> true, false, updateIfNeeded
                );*/

//                List<InstanceBean> imported = Optional.ofNullable(Jobs.runInBackgroundAndLoad(popupOwner, "Auto-Importing supported Files...", importer.makeImportJJob(input))
//                        .getResult()).map(c -> c.stream().map(this::getInstanceFromCompound).collect(Collectors.toList())).orElse(List.of());
//
//            }

    }
}
