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

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.ParameterBinding;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.compute.jjobs.LoadingBackroundTask;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.ImportMSDataDialog;
import de.unijena.bioinf.ms.gui.io.filefilter.MsBatchDataFormatFilter;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import io.sirius.ms.sdk.jjobs.SseProgressJJob;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobOptField;
import io.sirius.ms.sdk.model.LcmsSubmissionParameters;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceImporter;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Markus Fleischauer
 */
public class ImportAction extends AbstractGuiAction {

    public ImportAction(SiriusGui gui) {
        super("Import", gui);
        putValue(Action.LARGE_ICON_KEY, Icons.DOCS.derive(32,32));
        putValue(Action.SMALL_ICON, Icons.DOCS.derive(16,16));
        putValue(Action.SHORT_DESCRIPTION, "<html>" +
                "<p>Import measurements of:</p>" +
                "<ul style=\"list-style-type:none;\">" +
                "  <li>- Multiple compounds (e.g. .ms, .mgf)</li>" +
                "  <li>- LC-MS/MS runs (.mzML, .mzXml)</li>" +
                "</ul>" +
                "<p>into the current project-space. (Same as drag and drop)</p>" +
                "</html>");
    }

    //ATTENTION Synchronizing around background tasks that block gui thread is dangerous
    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser(PropertyManager.getFile(SiriusProperties.DEFAULT_LOAD_DIALOG_PATH));
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(true);
        chooser.addChoosableFileFilter(new MsBatchDataFormatFilter());
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
    public void importOneExperimentPerLocation(@NotNull final List<File> inputFiles, Window popupOwner) {
        final InputFilesOptions inputF = new InputFilesOptions();
        inputF.msInput = Jobs.runInBackgroundAndLoad(popupOwner, "Analyzing Files...", false,
                InstanceImporter.makeExpandFilesJJob(inputFiles)).getResult();
        importOneExperimentPerLocation(inputF, popupOwner);
    }

    //ATTENTION Synchronizing around background tasks that block gui thread is dangerous
    public void importOneExperimentPerLocation(@NotNull final InputFilesOptions input, Window popupOwner) {
        StopWatch watch = new StopWatch();
        watch.start();

        try {
            boolean hasLCMS = !input.msInput.lcmsFiles.isEmpty();
            boolean hasPeakLists = !input.msInput.msParserfiles.isEmpty();
            boolean alignAllowed = input.msInput.lcmsFiles.size() > 1;

            if (!hasLCMS && !hasPeakLists)
                return;

            // LC/MS default parameters
            LcmsSubmissionParameters parameters = new LcmsSubmissionParameters();
            if (hasLCMS)
                parameters.setAlignLCMSRuns(false);

            // show dialog
            if (hasPeakLists || alignAllowed) {
                ImportMSDataDialog dialog = new ImportMSDataDialog(popupOwner, hasLCMS, alignAllowed, hasPeakLists);
                if (!dialog.isSuccess())
                    return;

                if (hasLCMS) {
                    ParameterBinding binding = dialog.getParamterBinding();
                    binding.getOptBoolean("align").ifPresent(parameters::setAlignLCMSRuns);
                }
            }

            // handle LC/MS files
            if (hasLCMS) {
                LoadingBackroundTask<Job> task = gui.applySiriusClient((c, pid) -> {
                    Job job = c.projects().importMsRunDataAsJobLocally(pid,
                            parameters,
                            input.msInput.lcmsFiles.keySet().stream().map(Path::toAbsolutePath).map(Path::toString).toList(),
                            List.of(JobOptField.PROGRESS)
                    );
                    return Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Import, find & align...", new SseProgressJJob(gui.getSiriusClient(), pid, job));
                });

                task.awaitResult();
            }

            // handle non-LC/MS files
            if (hasPeakLists) {
                LoadingBackroundTask<Job> task = gui.applySiriusClient((c, pid) -> {
                    Job job = c.projects().importPreprocessedDataAsJobLocally(pid,
                            input.msInput.msParserfiles.keySet().stream().map(Path::toAbsolutePath).map(Path::toString).toList(),
                            PropertyManager.getBoolean("de.unijena.bioinf.sirius.ui.ignoreFormulas", false),
                            true,
                            List.of(JobOptField.PROGRESS)
                    );
                    return Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Import MS data...", new SseProgressJJob(gui.getSiriusClient(), pid, job));
                });
                task.awaitResult();
            }

        } catch (Exception e) {
            String m = Objects.requireNonNullElse(e.getMessage(), "");
            Stream.of("ProjectTypeException:", "ProjectStateException:").filter(m::contains).findFirst().ifPresentOrElse(
                    extText -> Jobs.runEDTLater(() -> new WarningDialog(gui.getMainFrame(), extText, GuiUtils.formatAndStripToolTip(m.substring(m.lastIndexOf(extText) + extText.length()).split(" \\| ")[0]), null)),
                    () -> Jobs.runEDTLater(() -> new StacktraceDialog(gui.getMainFrame(), e.getMessage(), e.getCause()))
            );
        }
    }
}
