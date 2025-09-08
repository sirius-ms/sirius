package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.table.SiriusListCellRenderer;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import io.sirius.ms.sdk.model.LibraryInfo;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class LibrariesDialog extends JDialog {

    private final SiriusGui gui;
    private final JList<LibraryInfo> libraryList;
    private final JTextPane descriptionPane;
    private final JButton downloadButton;


    public LibrariesDialog(Frame owner, SiriusGui gui) {
        super(owner, "SIRIUS Libraries", true);
        this.gui = gui;
        setLayout(new BorderLayout());

        libraryList = new JList<>();
        libraryList.setCellRenderer(new SiriusListCellRenderer(lib -> ((LibraryInfo) lib).getId()));
        libraryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        libraryList.addListSelectionListener(e -> {
            LibraryInfo lib = libraryList.getSelectedValue();
            if (lib != null) {
                updateDescription(lib);
            }
        });

        JScrollPane scroll = new JScrollPane(libraryList);
        TextHeaderBoxPanel pane = new TextHeaderBoxPanel("Libraries", scroll);
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

        loadLibraries();
        libraryList.setSelectedIndex(0);

        add(pane, BorderLayout.WEST);
        add(descriptionScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        GuiUtils.closeOnEscape(this);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(owner);
        setSize(new Dimension(650, 450));
        setVisible(true);
    }

    private void loadLibraries() {
        List<LibraryInfo> libraries = List.of();
        try {
            libraries = Jobs.runInBackgroundAndLoad(getOwner(), "Loading Libraries...",
                    () -> gui.applySiriusClient((c, pid) -> c.getLibraries().getLibraries())
            ).awaitResult();
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> gui.getSiriusClient().unwrapErrorResponse(ex).ifPresentOrElse(
                    err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Error " + err.getStatus() + ": " + err.getError(), JOptionPane.ERROR_MESSAGE),
                    () -> JOptionPane.showMessageDialog(this, ex.getCause().getMessage(), "Unexpected Error", JOptionPane.ERROR_MESSAGE)
            ));
            updateDescription(null);
        }
        libraryList.setListData(libraries.toArray(LibraryInfo[]::new));
    }

    private void updateDescription(LibraryInfo lib) {
        String text;
        if (lib == null) {
            downloadButton.setEnabled(false);
            text = "No library selected.";
        } else {
            downloadButton.setEnabled(true);
            String htmlDescription = Objects.requireNonNullElse(lib.getDescription(), "no description").replaceAll("\n", "<br>");
            String path = getDatabasePath(lib);
            text = "<html><body>" +
                    "<p>" + htmlDescription + "</p><hr>" +
                    "<p><b>Size: </b>" + (lib.getSize() == null ? "unknown" : FileUtils.sizeToReadableString(lib.getSize())) + "</p>" +
                    (path != null ? "<p><b>Downloaded to: </b>" + path + "</p>" : "") +
                    "</body></html>";
        }
        descriptionPane.setText(text);
    }

    @Nullable
    private String getDatabasePath(LibraryInfo lib) {
        try {
            SearchableDatabase db = gui.applySiriusClient((c, pid) -> c.databases().getDatabase(lib.getId(), false));
            String path = db.getLocation();
            if (path != null && lib.getSize() != null
                && Files.size(Path.of(path)) == lib.getSize()) {
                return path;
            }
        } catch (Exception ignore) {}
        return null;
    }
}
