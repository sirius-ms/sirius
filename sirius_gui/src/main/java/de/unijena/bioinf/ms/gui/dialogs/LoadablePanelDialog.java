package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class LoadablePanelDialog extends JDialog implements Loadable {

    protected final LoadablePanel loadablePanel;
    protected final JPanel contentContainer;

    public LoadablePanelDialog(Window owner, String title) {
        super(owner, title, DEFAULT_MODALITY_TYPE);
        this.setLayout(new BorderLayout());

        contentContainer = new JPanel(new BorderLayout());
        loadablePanel = new LoadablePanel(contentContainer);
        add(loadablePanel, BorderLayout.CENTER);
    }

    /**
     * Starts loading the panel from the given supplier in the background.
     */
    public void loadPanel(Supplier<JPanel> panelSupplier) {
        loadablePanel.runInBackgroundAndLoad(() -> {
            JPanel panel = panelSupplier.get();
            contentContainer.add(panel, BorderLayout.CENTER);
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
