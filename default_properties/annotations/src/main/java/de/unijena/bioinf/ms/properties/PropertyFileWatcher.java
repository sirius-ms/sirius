/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

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