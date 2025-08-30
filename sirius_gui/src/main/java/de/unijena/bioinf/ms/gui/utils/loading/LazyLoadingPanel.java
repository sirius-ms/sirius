package de.unijena.bioinf.ms.gui.utils.loading;

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

@Slf4j
public class LazyLoadingPanel<T extends JComponent> extends LoadablePanel {
    ReadWriteLock lock = new ReentrantReadWriteLock();

    public LazyLoadingPanel(Supplier<T> contentInitializer) {
        super();
        Jobs.runInBackground(() -> {
            lock.writeLock().lock();
            try {
                setLoading(true, true);
                setAndGetContentPanel(contentInitializer.get());
            } finally {
                lock.writeLock().unlock();
                setLoading(false, false);
            }
        });
    }

    @Override
    public <C extends JComponent> C setAndGetContentPanel(@NotNull C content) {
        lock.writeLock().lock();
        try {
            return super.setAndGetContentPanel(content);
        } finally {
            lock.writeLock().unlock();

        }
    }

    public Optional<T> getContentPanelIfReady() {
        boolean locked = lock.readLock().tryLock();
        if (locked) {
            try {
                return Optional.ofNullable((T) super.getContent());
            } catch (Exception e) {
                log.error("Error getting content panel", e);
                return Optional.empty();
            } finally {
                lock.readLock().unlock();
            }
        }
        return Optional.empty();
    }

    @Override
    protected T getContent() {
        lock.readLock().lock();
        try {
            return (T) super.getContent();
        } finally {
            lock.readLock().unlock();
        }
    }
}
