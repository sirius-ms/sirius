package de.unijena.bioinf.ms.gui.utils.loading;

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicInteger;

import static de.unijena.bioinf.ms.gui.utils.loading.ProgressPanel.DEFAULT_PROGRESS_STRING;

public class LoadablePanel extends JPanel implements Loadable {

    protected final AtomicInteger loadingCounter = new AtomicInteger(0);
    private final SiriusCardLayout centerCards = new SiriusCardLayout();
    //todo transparent overlay instead of card layout would be cool
    //todo maybe add some error state card?
    //todo the spinner progress panel supports progress support. Connect to ProgressJJob and RunInBackgroundAndLoad logic.
    public LoadablePanel() {
        this((JComponent) null);
    }

    public LoadablePanel(@Nullable String loadingText) {
        this(null, loadingText);
    }

    public LoadablePanel(@Nullable JComponent content) {
        this(content, DEFAULT_PROGRESS_STRING);
    }

    public LoadablePanel(@Nullable JComponent content, @Nullable String loadingText) {
        setLayout(centerCards);
        setOpaque(false);
        if (content != null)
            add("content", content);
        add("load", new SpinnerProgressPanel(loadingText));
    }

    public <C extends JComponent> C setAndGetContentPanel(@NotNull C content) {
        for (int i = 0; i < getComponents().length; i++) {
            if ("content".equals(getComponents()[i].getName())) {
                remove(i);
                break;
            }
        }
        add("content", content);
        return content;
    }

    public boolean setLoading(boolean loading, boolean absolute) {
        boolean result = loadingCounter.updateAndGet(current -> {
            if (absolute)
                return loading ? 1 : 0;
            else
                return Math.max(0, loading ? current + 1 : current - 1);
        }) > 0;
        final String cardName = result ? "load" : "content";
        Jobs.runEDTLater(() -> {
            if (!centerCards.isCardActive(cardName))
                centerCards.show(this, cardName);
        });
        return result;
    }

    protected JComponent getContent() {
        return (JComponent) getComponent(0);
    }
}
