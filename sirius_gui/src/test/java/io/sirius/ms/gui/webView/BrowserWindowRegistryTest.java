package io.sirius.ms.gui.webView;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the bookkeeping of the open browser windows. Uses windows that only record that they were
 * disposed, so no screen and no browser is needed.
 */
class BrowserWindowRegistryTest {

    private record TestKey(String view, String id) {}

    /**
     * Stands in for a browser window. Disposing is recorded instead of touching a real window, and
     * may run an action, which is how a real window unregisters itself when it is closed.
     */
    private static class RecordingWindow implements BrowserWindow {
        private boolean disposed = false;
        private Runnable onDispose = null;

        @Override
        public Window window() {
            throw new UnsupportedOperationException("no real window in these tests");
        }

        @Override
        public BrowserPanel browserPanel() {
            throw new UnsupportedOperationException("no real web view in these tests");
        }

        @Override
        public void dispose() {
            disposed = true;
            if (onDispose != null)
                onDispose.run();
        }
    }

    private final List<TestKey> registered = new ArrayList<>();

    private RecordingWindow register(TestKey key) {
        RecordingWindow window = new RecordingWindow();
        BrowserWindowRegistry.register(key, window);
        registered.add(key);
        return window;
    }

    @AfterEach
    void clearRegistry() {
        //the registry is global, so it must not leak windows into the next test
        registered.forEach(key -> {
            BrowserWindow open = BrowserWindowRegistry.get(key);
            if (open != null)
                BrowserWindowRegistry.unregister(key, open);
        });
        registered.clear();
    }

    @Test
    void registeredWindowIsFoundByItsKey() {
        TestKey key = new TestKey("content", "db1");
        RecordingWindow window = register(key);

        assertSame(window, BrowserWindowRegistry.get(key));
        //an equal key finds it as well, since the key is a value object
        assertSame(window, BrowserWindowRegistry.get(new TestKey("content", "db1")));
        assertNull(BrowserWindowRegistry.get(new TestKey("content", "db2")));
    }

    @Test
    void unregisterOnlyRemovesTheGivenWindow() {
        TestKey key = new TestKey("content", "db1");
        RecordingWindow window = register(key);

        //a window that has been replaced under the same key must not remove its successor
        BrowserWindowRegistry.unregister(key, new RecordingWindow());
        assertSame(window, BrowserWindowRegistry.get(key));

        BrowserWindowRegistry.unregister(key, window);
        assertNull(BrowserWindowRegistry.get(key));
    }

    @Test
    void disposesOnlyTheWindowsWhoseKeyMatches() {
        RecordingWindow contentOfDb1 = register(new TestKey("content", "db1"));
        RecordingWindow toolOfDb1 = register(new TestKey("reactionTool", "db1"));
        RecordingWindow contentOfDb2 = register(new TestKey("content", "db2"));

        //this is how a deleted database closes the windows showing it
        BrowserWindowRegistry.dispose(key -> key instanceof TestKey testKey && "db1".equals(testKey.id()));

        assertTrue(contentOfDb1.disposed);
        assertTrue(toolOfDb1.disposed);
        assertFalse(contentOfDb2.disposed, "a window of another database must stay open");
    }

    @Test
    void disposeAllDisposesEveryWindow() {
        RecordingWindow first = register(new TestKey("content", "db1"));
        RecordingWindow second = register(new TestKey("reactionTool", "db2"));

        BrowserWindowRegistry.disposeAll();

        assertTrue(first.disposed);
        assertTrue(second.disposed);
    }

    @Test
    void disposingIsSafeWhileTheWindowsUnregisterThemselves() {
        //a real window unregisters itself when it is closed, so disposing modifies the registry
        //while it is being iterated
        TestKey firstKey = new TestKey("content", "db1");
        TestKey secondKey = new TestKey("content", "db2");
        RecordingWindow first = register(firstKey);
        RecordingWindow second = register(secondKey);
        first.onDispose = () -> BrowserWindowRegistry.unregister(firstKey, first);
        second.onDispose = () -> BrowserWindowRegistry.unregister(secondKey, second);

        assertDoesNotThrow(BrowserWindowRegistry::disposeAll);

        assertTrue(first.disposed);
        assertTrue(second.disposed);
        assertNull(BrowserWindowRegistry.get(firstKey));
        assertNull(BrowserWindowRegistry.get(secondKey));
    }

    @Test
    void disposingAWindowThatOpensAnotherOneIsSafe() {
        //closing a window may open another one, which registers while the registry is being iterated
        TestKey key = new TestKey("content", "db1");
        TestKey successorKey = new TestKey("content", "successor");
        RecordingWindow window = register(key);
        window.onDispose = () -> {
            BrowserWindowRegistry.unregister(key, window);
            register(successorKey);
        };

        assertDoesNotThrow(BrowserWindowRegistry::disposeAll);

        assertTrue(window.disposed);
        assertNotNull(BrowserWindowRegistry.get(successorKey), "the newly opened window must stay registered");
    }
}
