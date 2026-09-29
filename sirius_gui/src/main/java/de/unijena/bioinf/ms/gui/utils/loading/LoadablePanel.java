package de.unijena.bioinf.ms.gui.utils.loading;

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Colors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static de.unijena.bioinf.ms.gui.utils.loading.ProgressPanel.DEFAULT_PROGRESS_STRING;

/**
 * Shows a loading animation in place of its content. Content that contains a web view has to use
 * {@link #overlaying} instead, which never hides it.
 */
public class LoadablePanel extends JPanel implements Loadable {

    protected final AtomicInteger loadingCounter = new AtomicInteger(0);
    /** Shows either the content or the loading animation; null for a panel made by {@link #overlaying}. */
    private final @Nullable SiriusCardLayout centerCards;
    /** Covers the content while loading; null unless the panel was made by {@link #overlaying}. */
    private final @Nullable JComponent overlay;
    private JComponent content;
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
        this(content, loadingText, false);
    }

    private LoadablePanel(@Nullable JComponent content, @Nullable String loadingText, boolean overlaying) {
        setOpaque(false);
        if (overlaying) {
            centerCards = null;
            setLayout(new StackLayout());
            overlay = new LoadingOverlay(new SpinnerProgressPanel(loadingText));
            overlay.setVisible(false);
            add(overlay); // first child = painted last = on top of the content
            if (content != null) {
                this.content = content;
                add(content);
            }
        } else {
            overlay = null;
            centerCards = new SiriusCardLayout();
            setLayout(centerCards);
            if (content != null) {
                this.content = content;
                add("content", content);
            }
            add("load", new SpinnerProgressPanel(loadingText));
        }
    }

    /**
     * A panel that shows the loading animation on top of its content instead of in its place, so the content is
     * never hidden. Content that contains a web view needs this: hiding a JxBrowser off-screen view stops its
     * keyboard hook, which deadlocks the EDT on Linux when a key event is being processed at that moment
     * (JxBrowser 9.3.1).
     */
    public static LoadablePanel overlaying(@NotNull JComponent content) {
        return new LoadablePanel(content, DEFAULT_PROGRESS_STRING, true);
    }

    public <C extends JComponent> C setAndGetContentPanel(@NotNull C content) {
        if (overlay != null) {
            if (this.content != null)
                remove(this.content);
            this.content = content;
            add(content); // after the overlay, so it stays beneath it
            return content;
        }
        for (int i = 0; i < getComponents().length; i++) {
            if ("content".equals(getComponents()[i].getName())) {
                remove(i);
                break;
            }
        }
        this.content = content;
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
        try {
            Jobs.runEDTAndWait(() -> {
                if (overlay != null) {
                    if (overlay.isVisible() != result)
                        overlay.setVisible(result);
                } else if (!centerCards.isCardActive(cardName)) {
                    centerCards.show(this, cardName);
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            //ignore;
        }
        return result;
    }

    /** The overlay and the content overlap, so swing has to paint them back to front. */
    @Override
    public boolean isOptimizedDrawingEnabled() {
        return overlay == null && super.isOptimizedDrawingEnabled();
    }

    protected JComponent getContent() {
        return content;
    }

    /**
     * Veils the content beneath it, shows the loading animation and takes the mouse input meant for the content,
     * which is as unavailable while loading as it is when the loading animation replaces it.
     */
    private static final class LoadingOverlay extends JPanel {
        LoadingOverlay(@NotNull JComponent loadingAnimation) {
            super(new BorderLayout());
            setOpaque(false);
            add(loadingAnimation, BorderLayout.CENTER);
            MouseAdapter swallow = new MouseAdapter() {
            };
            addMouseListener(swallow);
            addMouseMotionListener(swallow);
            addMouseWheelListener(swallow);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Colors.withTransparency(getBackground(), 0.6));
            g.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
        }
    }

    /** Gives every child the whole panel, so the overlay covers exactly the content. */
    private static final class StackLayout implements LayoutManager {
        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return largest(parent, Component::getPreferredSize);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return largest(parent, Component::getMinimumSize);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            int width = parent.getWidth() - insets.left - insets.right;
            int height = parent.getHeight() - insets.top - insets.bottom;
            for (Component child : parent.getComponents())
                child.setBounds(insets.left, insets.top, width, height);
        }

        private static Dimension largest(Container parent, Function<Component, Dimension> sizeOf) {
            Insets insets = parent.getInsets();
            int width = 0, height = 0;
            for (Component child : parent.getComponents()) {
                Dimension size = sizeOf.apply(child);
                width = Math.max(width, size.width);
                height = Math.max(height, size.height);
            }
            return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
        }
    }
}
