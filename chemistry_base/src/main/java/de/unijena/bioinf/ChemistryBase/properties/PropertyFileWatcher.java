package de.unijena.bioinf.ChemistryBase.properties;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PropertyFileWatcher extends Thread {
    private final Path file;
    private AtomicBoolean stop = new AtomicBoolean(false);
    private final Properties props;

    public PropertyFileWatcher(Path file, Properties propertiesToKeepUpToDate) {
        this.file = file;
        props = propertiesToKeepUpToDate;
    }

    //these constructors create ab instance that adds changes to System.properties
    public PropertyFileWatcher(Path file) {
        this(file, PropertyManager.PROPERTIES);
    }

    public PropertyFileWatcher(String file) {
        this(Paths.get(file));
    }


    public boolean isStopped() {
        return stop.get();
    }

    public void stopThread() {
        stop.set(true);
    }

    private boolean putAll(Properties properties) {
        boolean putted = false;
        for (String key : properties.stringPropertyNames()) {
            Object nu = properties.getProperty(key);
            Object old = props.put(key, nu);
            if (old == null || !old.equals(nu))
                putted = true;
        }
        return putted;
    }

    public void notifyListeners() {
        notifyListeners(false);
    }

    public void notifyListeners(final boolean forceReload) {
        try (final InputStream in = Files.newInputStream(file, StandardOpenOption.READ)) {
            final Properties properties = new Properties();
            properties.load(in);
            if (putAll(properties) || forceReload) {
                for (PropertyFileListener listener : listeners) {
                    listener.propertiesFileChanged(props);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            notifyListeners(true);

            while (!isStopped()) {
                WatchKey key;
                try {
                    key = watcher.poll(25, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    return;
                }
                if (key == null) {
                    Thread.yield();
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        Thread.yield();
                        continue;
                    } else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
                            && filename.toString().equals(file.getFileName().toString())) {
                        notifyListeners();
                    }
                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
                Thread.yield();
            }

        } catch (Throwable e) {
            LoggerFactory.getLogger(this.getClass()).error("FileWatcher Error", e);
        }
    }
}