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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadablePanel extends JPanel implements Loadable {

    protected final AtomicInteger loadingCounter = new AtomicInteger(0);
    private final SiriusCardLayout centerCards = new SiriusCardLayout();

    public LoadablePanel(@NotNull JComponent content) {
        this(content, "Loading...");
    }

    public LoadablePanel(@NotNull JComponent content, @Nullable String loadingMessage) {
        this(content, Icons.ATOM_LOADER_200, loadingMessage);
    }

    //todo maybe add some error state card?
    public LoadablePanel(@NotNull JComponent content, @NotNull ImageIcon filterAnimation, @Nullable String loadingMessage) {
        setLayout(centerCards);
        add("content", content);
        add("load", makeLoadingState(filterAnimation, loadingMessage));
    }

    public boolean setLoading(boolean loading) {
        AtomicBoolean result = new AtomicBoolean(false);
        try {
            Jobs.runEDTAndWait(() -> {
                result.set(loadingCounter.updateAndGet(current -> Math.max(0, loading ? current + 1 : current - 1)) > 0);
                String cardName = result.get() ? "load" : "content";
                if(!centerCards.isCardActive(cardName))
                    centerCards.show(this, cardName);
            });
        } catch (InvocationTargetException | InterruptedException e) {
            LoggerFactory.getLogger("Retry Setting loading state was interrupted unexpectedly.");
        }
        return result.get();
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
