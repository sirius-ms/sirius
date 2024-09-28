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
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.io.filefilter.NoSQLProjectFileFilter;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.utils.ErrorReportingDocumentListener;
import de.unijena.bioinf.ms.gui.utils.PlaceholderTextField;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import io.sirius.ms.sdk.model.ProjectInfoOptField;
import de.unijena.bioinf.ms.properties.PropertyManager;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import static de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX;


/**
 * @author Markus Fleischauer
 */
public class ProjectCreateAction extends ProjectOpenAction {


    public static final String DONT_ASK_NEW_WINDOW_CREATE_KEY = "de.unijena.bioinf.sirius.dragdrop.newWindowCreate.dontAskAgain";

    public static final Pattern PROJECT_ID_VALIDATOR = Pattern.compile("[a-zA-Z0-9_-]+", Pattern.CASE_INSENSITIVE);

    public ProjectCreateAction(SiriusGui gui) {
        super("New", gui);
        putValue(Action.LARGE_ICON_KEY, Icons.ADD_DOC_32);
        putValue(Action.SHORT_DESCRIPTION, "Create a new empty project at the given location.");
        setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        openDialog(mainFrame, "Create", this::createProject);
    }

    public static void openDialog(MainFrame mainFrame, String buttonText, BiConsumer<String, Path> onSuccess) {
        JFileChooser jfc = new JFileChooser();

        jfc.setCurrentDirectory(PropertyManager.getFile(SiriusProperties.DEFAULT_SAVE_DIR_PATH));
        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        jfc.removeChoosableFileFilter(jfc.getAcceptAllFileFilter());
        jfc.addChoosableFileFilter(new NoSQLProjectFileFilter());

        //region jfilechooser hack

        final Properties props = SiriusProperties.SIRIUS_PROPERTIES_FILE().asProperties();
        final String theme = props.getProperty("de.unijena.bioinf.sirius.ui.theme", "Light");

        JPanel chooserSouthComponent;
        if (theme.equals("Classic")) {
            chooserSouthComponent = (JPanel) ((BorderLayout) jfc.getLayout()).getLayoutComponent(jfc, BorderLayout.SOUTH);
        } else {
            JPanel central = (JPanel) ((BorderLayout) jfc.getLayout()).getLayoutComponent(BorderLayout.CENTER);
            chooserSouthComponent = (JPanel) ((BorderLayout) central.getLayout()).getLayoutComponent(jfc, BorderLayout.SOUTH);
        }

        PlaceholderTextField projectNameField = new PlaceholderTextField("");

        JButton cButton = (JButton) ((JPanel) chooserSouthComponent.getComponent(3)).getComponent(0);
        cButton.setEnabled(false);

        projectNameField.addFocusListener(new FocusAdapter() {
            @SneakyThrows
            @Override
            public void focusLost(FocusEvent e) {
                String name = projectNameField.getText();

                if (!name.endsWith(SIRIUS_PROJECT_SUFFIX)) {
                    Document doc = projectNameField.getDocument();
                    doc.insertString(name.length(), SIRIUS_PROJECT_SUFFIX, null);
                }
            }
        });

        projectNameField.setInputVerifier(new ErrorReportingDocumentListener(projectNameField) {
            @Override
            public String getErrorMessage(JComponent input) {
                String name = ((JTextField) input).getText();
                if (name.endsWith(SIRIUS_PROJECT_SUFFIX)) {
                    name = name.substring(0, name.length() - SIRIUS_PROJECT_SUFFIX.length());
                }

                if (jfc.getCurrentDirectory() == null) {
                    SwingUtilities.invokeLater(() -> cButton.setEnabled(false));
                    return "No directory selected.";
                }

                File pfile = jfc.getCurrentDirectory().toPath().resolve(name + SIRIUS_PROJECT_SUFFIX).toFile();

                String error = null;
                if (name.isEmpty()) {
                    error =  "Project name must not be empty.";
                } else if (name.isBlank()) {
                    error =  "Project name must not contain only white spaces.";
                } else if (!PROJECT_ID_VALIDATOR.matcher(name).matches()) {
                    error =  "Project name must be valid: \"([a-zA-Z0-9_-]+).sirius\"";
                } else if (pfile.exists()) {
                    error =  "Project already exists";
                }

                if (error != null) {
                    SwingUtilities.invokeLater(() -> cButton.setEnabled(false));
                } else {
                    jfc.setSelectedFile(pfile);
                    SwingUtilities.invokeLater(() -> cButton.setEnabled(true));
                }
                return error;
            }

        });

        TwoColumnPanel textComponent = new TwoColumnPanel();
        textComponent.addNamed("Project name:", projectNameField);

        chooserSouthComponent.remove(2);
        chooserSouthComponent.remove(1);
        chooserSouthComponent.remove(0);
        chooserSouthComponent.add(textComponent, 0);

        jfc.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, (e) -> {
            if (e.getNewValue() instanceof File file) {
                final Path f = file.toPath();
                if (e.getOldValue() instanceof File old) {
                    if (old.toPath().equals(f)) return;
                }

                if (Files.isRegularFile(f)) {
                    SwingUtilities.invokeLater(() -> projectNameField.setText(ensureUniqueProjectId(f)));
                }
            }
        });

        // endregion

        int returnval = jfc.showDialog(mainFrame, buttonText);
        if (returnval == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = jfc.getSelectedFile();

                if (selectedFile == null) {
                    throw new IllegalArgumentException("No file selected.");
                }
                if (selectedFile.exists()) {
                    throw new FileAlreadyExistsException(selectedFile.toString());
                }
                if (!new File(selectedFile.getParent()).isDirectory()) {
                    throw new FileNotFoundException(selectedFile.getParent());
                }
                if (!selectedFile.getName().endsWith(SIRIUS_PROJECT_SUFFIX)) {
                    throw new IllegalArgumentException("File name must end with " + SIRIUS_PROJECT_SUFFIX);
                }
                String projectName = selectedFile.getName().substring(0, selectedFile.getName().length() - SIRIUS_PROJECT_SUFFIX.length());
                if (projectName.isEmpty()) {
                    throw new IllegalArgumentException("Project name must not be empty.");
                }
                if (projectName.isBlank()) {
                    throw new IllegalArgumentException("Project name must not contain only white spaces.");
                }
                if (!PROJECT_ID_VALIDATOR.matcher(projectName).matches()) {
                    throw new IllegalArgumentException("File name must be valid: \"([a-zA-Z0-9_-]+).sirius\"");
                }

                Jobs.runInBackground(() ->
                        SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty(SiriusProperties.DEFAULT_SAVE_DIR_PATH, selectedFile.getParent())
                );
                onSuccess.accept(projectName, selectedFile.toPath());

            } catch (Exception e) {
                new StacktraceDialog(mainFrame, e.getMessage(), e);
            }
        }
    }

    private static String ensureUniqueProjectId(Path nameSuggestion) {
        if (!Files.exists(nameSuggestion))
            return nameSuggestion.getFileName().toString();
        int app = 2;
        String base = nameSuggestion.getFileName().toString();
        if (base.endsWith(SIRIUS_PROJECT_SUFFIX)) {
            base = base.substring(0, base.length() - SIRIUS_PROJECT_SUFFIX.length());
        }
        base = base.replaceAll("_[0-9]+$", "");
        while (true) {
            Path n = nameSuggestion.getParent().resolve(base + "_" + app++ + SIRIUS_PROJECT_SUFFIX);
            if (!Files.exists(n))
                return n.getFileName().toString();
        }
    }

    public void createProject(@NotNull String projectId, @NotNull Path projectPath) {
        createProject(projectId, projectPath, null);
    }

    public void createProject(@NotNull String projectId, @NotNull Path projectPath, @Nullable Boolean closeCurrent) {
        try {
            String pid = Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Creating Project...", () ->
                    gui.getSiriusClient().projects()
                            .createProjectSpace(projectId, projectPath.toAbsolutePath().toString(), List.of(ProjectInfoOptField.NONE))
                            .getProjectId()

            ).awaitResult();

            openProjectByID(pid, closeCurrent);
        } catch (ExecutionException e) {
            LoggerFactory.getLogger(getClass()).error("Error when creating new project!", e);
            Jobs.runEDTLater(() -> new StacktraceDialog(gui.getMainFrame(), "Error when creating new project!", e));
        }
    }

    @Override
    protected String dontAskKey() {
        return DONT_ASK_NEW_WINDOW_CREATE_KEY;
    }
}
