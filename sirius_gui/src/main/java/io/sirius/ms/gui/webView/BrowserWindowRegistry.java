package io.sirius.ms.gui.webView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The browser windows that are currently open under a key, see
 * {@link BrowserWindowBuilder#singleton(Object)}.
 * <p>
 * Holds every such window of the application, so that windows can be closed again by whoever
 * invalidates what they show, without having to know them.
 */
public final class BrowserWindowRegistry {

    /**
     * Insertion ordered, so windows are disposed in the order they were opened.
     */
    private static final Map<Object, BrowserWindow> INSTANCES = new LinkedHashMap<>();

    private BrowserWindowRegistry() {
    }

    static synchronized @Nullable BrowserWindow get(@NotNull Object key) {
        return INSTANCES.get(key);
    }

    static synchronized void register(@NotNull Object key, @NotNull BrowserWindow window) {
        INSTANCES.put(key, window);
    }

    static synchronized void unregister(@NotNull Object key, @NotNull BrowserWindow window) {
        INSTANCES.remove(key, window);
    }

    /**
     * Disposes all open windows whose key matches, e.g. because what they show does not exist
     * anymore.
     */
    public static void dispose(@NotNull Predicate<Object> keyMatches) {
        //copy, since disposing removes the window from the registry
        final List<BrowserWindow> toDispose;
        synchronized (BrowserWindowRegistry.class) {
            toDispose = INSTANCES.entrySet().stream()
                    .filter(entry -> keyMatches.test(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .toList();
        }
        toDispose.forEach(BrowserWindow::dispose);
    }

    /**
     * Disposes all open windows. Needs to be called when the shared gui infrastructure (browser
     * panel provider) is shut down, since a window without its browser engine is useless.
     */
    public static void disposeAll() {
        final List<BrowserWindow> toDispose;
        synchronized (BrowserWindowRegistry.class) {
            toDispose = new ArrayList<>(INSTANCES.values());
        }
        toDispose.forEach(BrowserWindow::dispose);
    }
}
