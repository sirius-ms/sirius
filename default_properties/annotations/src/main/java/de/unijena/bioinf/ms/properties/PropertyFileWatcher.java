package de.unijena.bioinf.ms.properties;

import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import org.apache.commons.configuration2.reloading.ReloadingController;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class PropertyFileWatcher extends TinyBackgroundJJob<Object> implements Runnable {
    private AtomicBoolean stop = new AtomicBoolean(false);
    private ReloadingController controller;
    private Path file;

    public PropertyFileWatcher() {
        this(null);
    }

    public PropertyFileWatcher(@Nullable Path fileToWath) {
        this(null, fileToWath);
    }

    public PropertyFileWatcher(@Nullable ReloadingController controllerToNotify, @Nullable Path fileToWath) {
        this.controller = controllerToNotify;
        this.file = fileToWath;
    }

    public void setController(ReloadingController controller) {
        notSubmittedOrThrow();
        this.controller = controller;
    }

    public void setFile(Path file) {
        notSubmittedOrThrow();
        this.file = file;
    }

    public Path getFile() {
        return file;
    }

    public boolean isStopping() {
        return stop.get();
    }

    public void stop() {
        stop.set(true);
    }

    public void notifyController() {
        controller.checkForReloading(null);
    }

    private final LinkedHashSet<PropertyFileListener> listeners = new LinkedHashSet<>();

    public void addListener(PropertyFileListener listener) {
        listeners.add(listener);
    }

    public boolean removePropertyChangeListener(PropertyFileListener listener) {
        return listeners.remove(listener);
    }

    @Override
    public void run() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Path path = file.getParent();
            path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            notifyController();

            while (!isStopping()) {
                Thread.yield();
                final WatchKey key = watcher.poll();

                if (key == null)
                    continue;

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    } else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
                            && filename.toString().equals(file.getFileName().toString())) {
                        notifyController();
                    }

                    if (!key.reset())
                        break;
                }
            }
        } catch (Throwable e) {
            LoggerFactory.getLogger(this.getClass()).error("FileWatcher Error", e);
        }
    }

    @Override
    protected Object compute() throws Exception {
        run();
        return null;
    }
}