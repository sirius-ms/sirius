package de.unijena.bioinf.ms.gui.utils.loading;

import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadablePanel extends JPanel implements Loadable {

    protected final AtomicInteger loadingCounter = new AtomicInteger(0);
    private final SiriusCardLayout centerCards = new SiriusCardLayout();
    //todo transparent overlay instead of card layout would be cool
    //todo maybe add some error state card?
    //todo the spinner progress panel supports progress support. Connect to ProgressJJob and RunInBackgroundAndLoad logic.
    public LoadablePanel() {
        this(null);
    }
    public LoadablePanel(@Nullable JComponent content) {
        setLayout(centerCards);
        setOpaque(false);
        if (content != null)
            add("content", content);
        add("load", GuiUtils.newSpinnerProgressPanel());
    }

    public void setContentPanel(@NotNull JComponent content){
        for (int i = 0; i < getComponents().length; i++) {
             if ("content".equals(getComponents()[i].getName())){
                 remove(i);
                 break;
             }
        }
        add("content", content);
    }

    public boolean setLoading(boolean loading, boolean absolute) {
        AtomicBoolean result = new AtomicBoolean(false);
        Jobs.runEDTLater(() -> {
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
        return result.get();
    }

    protected JComponent getContent() {
        return (JComponent) getComponent(0);
    }


    public TinyBackgroundJJob<Boolean> runInBackgroundAndLoad(final Runnable task) {
        return Jobs.runInBackground(() -> {
            setLoading(true, true);
            try {
                task.run();
            }finally {
                setLoading(false, true);
            }
        });
    }

    public <T> TinyBackgroundJJob<T> runInBackgroundAndLoad(final Callable<T> task) {
        return Jobs.runInBackground(() -> {
            setLoading(true, true);
            try {
                return task.call();
            }finally {
                setLoading(false, true);
            }
        });
    }
}
