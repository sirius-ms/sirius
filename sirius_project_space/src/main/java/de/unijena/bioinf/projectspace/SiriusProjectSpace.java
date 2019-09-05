package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import de.unijena.bioinf.projectspace.sirius.SiriusLocations;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

public class SiriusProjectSpace implements Iterable<CompoundContainerId>, AutoCloseable {


    protected final File root;
    protected final ConcurrentHashMap<String, CompoundContainerId> ids;
    protected final ProjectSpaceConfiguration configuration;
    protected final AtomicInteger compoundCounter;
    private final ConcurrentHashMap<Class<? extends ProjectSpaceProperty>, ProjectSpaceProperty> projectSpaceProperties;

    protected ConcurrentLinkedQueue<ProjectSpaceListener> projectSpaceListeners;

    protected SiriusProjectSpace(ProjectSpaceConfiguration configuration, File root) {
        this.configuration = configuration;
        this.ids = new ConcurrentHashMap<>();
        this.root = root;
        this.compoundCounter = new AtomicInteger(-1);
        this.projectSpaceListeners = new ConcurrentLinkedQueue<>();
        this.projectSpaceProperties = new ConcurrentHashMap<>();
    }

    public void addProjectSpaceListener(ProjectSpaceListener listener) {
        projectSpaceListeners.add(listener);
    }

    protected void fireProjectSpaceChange(ProjectSpaceEvent event) {
        for (ProjectSpaceListener listener : this.projectSpaceListeners)
            listener.projectSpaceChanged(event);
    }

    protected synchronized void open() throws IOException {
        ids.clear();
        int maxIndex = 0;
        for (File dir : root.listFiles()) {
            final File expInfo = new File(dir, SiriusLocations.COMPOUND_INFO);
            if (dir.isDirectory() && expInfo.exists()) {
                final Map<String,String> keyValues = FileUtils.readKeyValues(expInfo);
                int index = Integer.parseInt(keyValues.getOrDefault("index","0"));
                String name = keyValues.getOrDefault("name", "");
                String dirName = dir.getName();
                ids.put(dirName, new CompoundContainerId(dirName,name, index));
                maxIndex = Math.max(index,maxIndex);
            }
        }
        ++maxIndex;
        this.compoundCounter.set(maxIndex);
        fireProjectSpaceChange(ProjectSpaceEvent.OPENED);
    }

    public synchronized void close() throws IOException {
        this.ids.clear();
        fireProjectSpaceChange(ProjectSpaceEvent.CLOSED);
    }


    public Optional<CompoundContainerId> findCompound(String dirName) {
        return Optional.ofNullable(ids.get(dirName));
    }

    public Optional<CompoundContainer> newCompoundWithUniqueIndex(String compoundName, IntFunction<String> index2dirName) {
        return newUniqueCompoundIndex(compoundName, index2dirName)
                .map(idd -> {
                    try {
                        return getCompound(idd);
                    } catch (IOException e) {
                        return null;
                    }
                });
    }

    public Optional<CompoundContainerId> newUniqueCompoundIndex(String compoundName, IntFunction<String> index2dirName) {
        int index = compoundCounter.getAndIncrement();
        String dirName = index2dirName.apply(index);
        return tryCreateCompoundContainer(dirName,compoundName,index);
    }

    protected Optional<CompoundContainerId> tryCreateCompoundContainer(String directoryName, String compoundName, int compoundIndex) {
        if (ids.contains(directoryName)) return Optional.empty();
        synchronized (ids) {
            if (new File(root, directoryName).exists())
                return Optional.empty();
            CompoundContainerId id = new CompoundContainerId(directoryName, compoundName, compoundIndex);
            if (ids.put(directoryName, id)!=null)
                return Optional.empty();
            try {
                Files.createDirectory(new File(root,directoryName).toPath());
                try (final BufferedWriter bw = FileUtils.getWriter(new File(new File(root,id.getDirectoryName()), "experiment.info"))) {
                    bw.write("index\t"+id.getCompoundIndex()); bw.newLine();
                    bw.write("name\t"+id.getCompoundName()); bw.newLine();
                }
                fireProjectSpaceChange(ProjectSpaceEvent.INDEX_UPDATED);
                return Optional.of(id);
            } catch (IOException e) {
                LoggerFactory.getLogger(SiriusProjectSpace.class).error("cannot create directory " + directoryName, e);
                ids.remove(id.getDirectoryName());
                return Optional.empty();
            }
        }
    }

    // shorthand methods

    public FormulaResult getFormulaResult(FormulaResultId id, Class<?>... components) throws IOException {
        CompoundContainerId parentId = id.getParentId();
        parentId.containerLock.lock();
        try {
            return getContainer(FormulaResult.class, id, components);
        } finally {
            parentId.containerLock.unlock();;
        }
    }

    public void updateFormulaResult(FormulaResult result, Class<?>... components) throws IOException {
        CompoundContainerId parentId = result.getId().getParentId();
        parentId.containerLock.lock();
        try {
            updateContainer(FormulaResult.class,result,components);
        } finally {
            parentId.containerLock.unlock();
        }
    }

    public void deleteFormulaResult(FormulaResultId resultId) throws IOException {
        CompoundContainerId parentId = resultId.getParentId();
        parentId.containerLock.lock();
        try {
            deleteContainer(FormulaResult.class,resultId);
        } finally {
            parentId.containerLock.unlock();
        }
    }

    public CompoundContainer getCompound(CompoundContainerId id, Class<?>... components) throws IOException {
        id.containerLock.lock();
        try {
            return getContainer(CompoundContainer.class, id, components);
        } finally {
            id.containerLock.unlock();
        }
    }

    public void updateCompound(CompoundContainer result, Class<?>... components) throws IOException {
        final CompoundContainerId id = result.getId();
        id.containerLock.lock();
        try {
            updateContainer(CompoundContainer.class,result,components);
        } finally {
            id.containerLock.unlock();
        }
    }

    public void deleteCompound(CompoundContainerId resultId) throws IOException {
        resultId.containerLock.lock();
        try {
            deleteContainer(CompoundContainer.class,resultId);
            fireProjectSpaceChange(ProjectSpaceEvent.INDEX_UPDATED);
        } finally {
            resultId.containerLock.unlock();
        }
    }

    public boolean renameCompound(CompoundContainerId oldId, String newDirName) throws IOException {
        oldId.containerLock.lock();
        try {
            synchronized (ids) {
                if (ids.containsKey(newDirName))
                    return false;
                File file = new File(root, newDirName);
                if (file.exists()) {
                    return false;
                }
                try {
                    Files.move(new File(root, oldId.getDirectoryName()).toPath(), file.toPath());
                } catch (IOException e) {
                    LoggerFactory.getLogger(SiriusProjectSpace.class).error("cannot move directory", e);
                    return false;
                }
                ids.remove(oldId.getDirectoryName());
                oldId.rename(newDirName);
                ids.put(oldId.getDirectoryName(), oldId);
            }
        } finally {
            oldId.containerLock.unlock();
        }
        return true;
    }


    // generic methods


    <Id extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<Id>>
    Container getContainer(Class<Container> klass, Id id, Class<?>... components) throws IOException {
        // read container
        final Container container = configuration.getContainerSerializer(klass).readFromProjectSpace(new FileBasedProjectSpaceReader(root), (r,c, f)->{
            // read components
            for (Class k : components) {
                configuration.getComponentSerializer(klass, k).read(r, id, c);
            }
        }, id);
        return container;
    }

    <Id extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<Id>>
    void updateContainer(Class<Container> klass, Container container, Class<?>... components) throws IOException {
        // write container
        configuration.getContainerSerializer(klass).writeToProjectSpace(new FileBasedProjectSpaceWriter(root), (r,c, f)->{
            // write components
            for (Class k : components) {
                configuration.getComponentSerializer(klass, k).write(r, container.getId(), container, c);
            }
        },container.getId(),container);
    }

    <Id extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<Id>>
    void deleteContainer(Class<Container> klass, Id containerId) throws IOException {
        configuration.getContainerSerializer(klass).deleteFromProjectSpace(new FileBasedProjectSpaceWriter(root), (r,id)->{
            // write components
            for (Class k : configuration.getAllComponentsForContainer(klass)) {
                configuration.getComponentSerializer(klass, k).delete(r, id);
            }
        },containerId);
    }

    @NotNull
    @Override
    public Iterator<CompoundContainerId> iterator() {
        return ids.values().iterator();
    }

    public int size() {
        return compoundCounter.get();
    }

    public <T extends ProjectSpaceProperty> T getProjectSpaceProperty(Class<T> key) {
        return (T) projectSpaceProperties.get(key);
    }

    public <T extends ProjectSpaceProperty> T setProjectSpaceProperty(Class<T> key, T value) {
        return (T) projectSpaceProperties.put(key, value);
    }
}
