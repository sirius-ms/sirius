package de.unijena.bioinf.ms.gui.utils.loading;

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadablePanel extends JPanel implements Loadable {

    protected final AtomicInteger loadingCounter = new AtomicInteger(0);
    private final CardLayout centerCards = new CardLayout();

    public LoadablePanel(@NotNull JComponent content) {
        this(content, "Loading...");
    }
    public LoadablePanel(@NotNull JComponent content, @Nullable String loadingMessage) {
        this(content, Icons.ATOM_LOADER_200, loadingMessage);
    }
    public LoadablePanel(@NotNull JComponent content, @NotNull ImageIcon filterAnimation, @Nullable String loadingMessage) {
        setLayout(centerCards);
        add("content", content);
        add("load", makeLoadingState(filterAnimation, loadingMessage));
    }

    public void setLoading(boolean loading) {
        boolean l;
        if (loading)
            l = loadingCounter.incrementAndGet() > 0;
        else
            l = loadingCounter.decrementAndGet() > 0;

        //should never happen.
        if (loadingCounter.get() < 0)
            loadingCounter.set(0);

        try {
            Jobs.runEDTAndWait(() -> centerCards.show(this, l ? "load" : "content"));
        } catch (InvocationTargetException | InterruptedException e) {
            LoggerFactory.getLogger("Setting loading state was interrupted unexpectedly");
            try {
                Jobs.runEDTAndWait(() -> centerCards.show(this, l ? "load" : "content"));
            } catch (InvocationTargetException | InterruptedException e2) {
                LoggerFactory.getLogger("Retry Setting loading state was interrupted unexpectedly. Giving up!");
            }
        }
    }

    protected JPanel makeLoadingState() {
        return makeLoadingState("Loading...");
    }
    protected JPanel makeLoadingState(@Nullable String loadingMessage) {
        return makeLoadingState(Icons.ATOM_LOADER_200, loadingMessage);
    }
    protected JPanel makeLoadingState(@NotNull ImageIcon filterAnimation, @Nullable String loadingMessage) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Colors.BACKGROUND);
        //todo transparency would be cool
        panel.setOpaque(true);
        JLabel iconLabel = new JLabel(filterAnimation, SwingUtilities.CENTER);
        JLabel label = loadingMessage != null && !loadingMessage.isBlank() ? new JLabel(loadingMessage) : null;
//        if (filter) {
//            iconLabel = new JLabel(Icons.FILTER_LOADER_160, SwingUtilities.CENTER);
//            Icons.FILTER_LOADER_120.setImageObserver(iconLabel);
//            label = new JLabel("Filtering...");
//        } else {
//            iconLabel = new JLabel(Icons.ATOM_LOADER_200, SwingUtilities.CENTER);
//            Icons.ATOM_LOADER_200.setImageObserver(iconLabel);
//            label = new JLabel("Loading...");
//        }
        panel.add(iconLabel, BorderLayout.CENTER);
        if (label != null)
            panel.add(label, BorderLayout.SOUTH);
        return panel;
    }
}
