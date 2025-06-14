package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class LoadablePanelDialog extends JDialog {

    public LoadablePanelDialog(Window owner, String title, Supplier<JPanel> panelSupplier) {
        super(owner, title, DEFAULT_MODALITY_TYPE);
        this.setLayout(new BorderLayout());

        JPanel content = new JPanel(new BorderLayout());
        LoadablePanel loadablePanel = new LoadablePanel(content);
        add(loadablePanel, BorderLayout.CENTER);

        loadablePanel.runInBackgroundAndLoad(() -> {
            JPanel panel = panelSupplier.get();
            content.add(panel, BorderLayout.CENTER);
        });
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setPreferredSize(new Dimension(
                    Math.min(screenSize.width, (int) Math.floor(0.8 * getOwner().getWidth())),
                    Math.min(screenSize.height, (int) Math.floor(0.8 * getOwner().getHeight())))
            );
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            pack();
            setLocationRelativeTo(getParent());
            setResizable(false);
        }
        super.setVisible(b);
    }
}
