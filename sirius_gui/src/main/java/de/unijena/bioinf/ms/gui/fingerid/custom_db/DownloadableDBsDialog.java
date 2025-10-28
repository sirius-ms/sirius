package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.frontend.subtools.custom_db_downloader.DownloadDatabaseOptions;
import de.unijena.bioinf.ms.frontend.subtools.custom_db_downloader.DownloadableDBsOptions;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.compute.jjobs.LoadingBackroundTask;
import de.unijena.bioinf.ms.gui.table.SiriusListCellRenderer;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import io.sirius.ms.sdk.model.*;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class DownloadableDBsDialog extends JDialog {

    private final SiriusGui gui;
    private final DatabaseDialog databaseDialog;
    private final JList<DownloadableDatabase> databaseList;
    private final JTextPane descriptionPane;
    private final JButton downloadButton;


    public DownloadableDBsDialog(Frame owner, DatabaseDialog databaseDialog, SiriusGui gui) {
        super(owner, "SIRIUS Databases", true);
        this.databaseDialog = databaseDialog;
        this.gui = gui;
        setLayout(new BorderLayout());

        databaseList = new JList<>();
        databaseList.setCellRenderer(new SiriusListCellRenderer(db -> ((DownloadableDatabase) db).getId()));
        databaseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        databaseList.addListSelectionListener(e -> {
            DownloadableDatabase db = databaseList.getSelectedValue();
            if (db != null) {
                updateDescription(db);
            }
        });

        JScrollPane scroll = new JScrollPane(databaseList);
        TextHeaderBoxPanel pane = new TextHeaderBoxPanel("Downloadable Databases", scroll);
        pane.setBorder(BorderFactory.createEmptyBorder(GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP, 0, 0));


        descriptionPane = new JTextPane();
        descriptionPane.setContentType("text/html");
        descriptionPane.setEditable(false);
        descriptionPane.setBorder(null);
        descriptionPane.setOpaque(false);
        descriptionPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionPane);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        downloadButton = new JButton("Download");
        buttonPanel.add(downloadButton);
        downloadButton.addActionListener(e -> download(databaseList.getSelectedValue()));

        loadDatabases();
        databaseList.setSelectedIndex(0);

        add(pane, BorderLayout.WEST);
        add(descriptionScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        GuiUtils.closeOnEscape(this);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(owner);
        setSize(new Dimension(650, 450));
        setVisible(true);
    }

    private void loadDatabases() {
        List<DownloadableDatabase> databases = List.of();
        try {
            databases = Jobs.runInBackgroundAndLoad(getOwner(), "Loading Databases...",
                    () -> gui.applySiriusClient((c, pid) -> c.databases().getDownloadableDatabases())
            ).awaitResult();
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> gui.getSiriusClient().unwrapErrorResponse(ex).ifPresentOrElse(
                    err -> JOptionPane.showMessageDialog(this, err.getDetail(), "Error " + err.getStatus() + ": " + err.getTitle(), JOptionPane.ERROR_MESSAGE),
                    () -> JOptionPane.showMessageDialog(this, ex.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
            ));
            updateDescription(null);
        }
        databaseList.setListData(databases.toArray(DownloadableDatabase[]::new));
    }

    private void updateDescription(DownloadableDatabase db) {
        String text;
        if (db == null) {
            downloadButton.setEnabled(false);
            text = "No database selected.";
        } else {
            downloadButton.setEnabled(true);
            String htmlDescription = Objects.requireNonNullElse(db.getDescription(), "no description").replaceAll("\n", "<br>");
            String path = getDatabasePath(db);
            text = "<html><body>" +
                    "<p>" + htmlDescription + "</p>" +
                    "<p><b>Size: </b>" + (db.getSize() == null ? "unknown" : FileUtils.sizeToReadableString(db.getSize())) + "</p>" +
                    (path != null ? "<p><b>Downloaded to: </b>" + path + "</p>" : "") +
                    "</body></html>";
        }
        descriptionPane.setText(text);
    }

    @Nullable
    private String getDatabasePath(DownloadableDatabase db) {
        try {
            SearchableDatabase localDb = gui.applySiriusClient((c, pid) -> c.databases().getDatabase(db.getId(), false));
            String path = localDb.getLocation();
            if (path != null && db.getSize() != null
                && Files.size(Path.of(path)) == db.getSize()) {
                return path;
            }
        } catch (Exception ignore) {}
        return null;
    }

    private void download(DownloadableDatabase db) {
        try {
            if (getDatabasePath(db) != null) {
                if (JOptionPane.showConfirmDialog(this, "Database " + db.getId() + " is already downloaded to " + getDatabasePath(db) + "\nDownload anyway?", null, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            JFileChooser destinationChooser = new JFileChooser();
            destinationChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            destinationChooser.setDialogTitle("Destination for the database file");

            if (destinationChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            String destination = destinationChooser.getSelectedFile().getAbsolutePath();

            CommandSubmission command = new CommandSubmission();
            command.addCommandItem(PicoUtils.getCommand(DownloadableDBsOptions.class).name());
            command.addCommandItem(PicoUtils.getCommand(DownloadDatabaseOptions.class).name());
            command.addCommandItem("--db=" + db.getId());
            command.addCommandItem("--destination=" + destination);

            gui.applySiriusClient((c, pid) -> {
                Job j = c.jobs().startCommand(pid, command, List.of(JobOptField.PROGRESS));
                return LoadingBackroundTask.runInBackground(gui.getMainFrame(),
                        "Downloading " + db.getId() + "...", null,
                        new io.sirius.ms.sdk.jjobs.SseProgressJJob(gui.getSiriusClient(), pid, j));
            }).awaitResult();

            dispose();
            databaseDialog.whenCustomDbIsAdded(db.getId());
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> gui.getSiriusClient().unwrapErrorResponse(ex).ifPresentOrElse(
                    err -> JOptionPane.showMessageDialog(this, err.getDetail(), "Error " + err.getStatus() + ": " + err.getTitle(), JOptionPane.ERROR_MESSAGE),
                    () -> JOptionPane.showMessageDialog(this, ex.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
            ));
        }
    }
}
