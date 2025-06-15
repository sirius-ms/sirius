package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class LoadablePanelDialog extends JDialog implements Loadable {

    private final LoadablePanel loadablePanel;

    public LoadablePanelDialog(Window owner, String title, Supplier<JPanel> panelSupplier) {
        super(owner, title, DEFAULT_MODALITY_TYPE);
        this.setLayout(new BorderLayout());

        JPanel content = new JPanel(new BorderLayout());
        loadablePanel = new LoadablePanel(content);
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
            pack();
            setLocationRelativeTo(getParent());
            setResizable(false);
        }
        super.setVisible(b);
    }

    @Override
    public boolean setLoading(boolean loading, boolean absolute) {
        return loadablePanel.setLoading(loading, absolute);
    }
}
