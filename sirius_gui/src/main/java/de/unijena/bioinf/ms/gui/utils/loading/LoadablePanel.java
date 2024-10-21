package de.unijena.bioinf.ms.gui.utils.loading;

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
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
        this(content, Icons.ECLIPSE_LOADER_THICK_160, loadingMessage);
    }

    //todo maybe add some error state card?
    public LoadablePanel(@NotNull JComponent content, @NotNull ImageIcon filterAnimation, @Nullable String loadingMessage) {
        setLayout(centerCards);
        add("content", content);
        add("load", GuiUtils.newLoadingPanel(filterAnimation, loadingMessage));
    }

    public boolean setLoading(boolean loading, boolean absolute) {
        AtomicBoolean result = new AtomicBoolean(false);
        try {
            Jobs.runEDTAndWait(() -> {
                result.set(loadingCounter.updateAndGet(current -> {
                    if (absolute)
                        return loading ? 1 : 0;
                    else
                        return Math.max(0, loading ? current + 1 : current - 1);
                }) > 0);
                String cardName = result.get() ? "load" : "content";
                if (!centerCards.isCardActive(cardName))
                    centerCards.show(this, cardName);
            });
        } catch (InvocationTargetException | InterruptedException e) {
            LoggerFactory.getLogger("Retry Setting loading state was interrupted unexpectedly.");
        }
        return result.get();
    }

    protected JComponent getContent() {
        return (JComponent) getComponent(0);
    }
}
