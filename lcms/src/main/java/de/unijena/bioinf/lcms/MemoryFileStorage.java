package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.model.lcms.Scan;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class MemoryFileStorage implements SpectrumStorage {

    private final TIntIntHashMap offsets;
    private MappedByteBuffer buffer;
    private InMemoryStorage tempStorage;
    protected boolean dirty = false;
    private FileChannel writableChannel;
    private int totalSize;

    public MemoryFileStorage() throws IOException {
        this.offsets = new TIntIntHashMap();
        this.tempStorage = new InMemoryStorage();
    }

    public void keepInMemory() {
        this.tempStorage = new InMemoryStorage();
    }

    public void backOnDisc() throws IOException {
        if (dirty) {
            int totalSize = 0;
            for (SimpleSpectrum s : tempStorage.scan2spectrum.valueCollection()) {
                totalSize += s.size();
            }
            final int bytes = totalSize * 8 + tempStorage.scan2spectrum.size() * 4;
            if (writableChannel!=null) {
                writableChannel.close();
            }
            writableChannel = FileChannel.open(File.createTempFile("sirius_spectrum", ".binary").toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE);
            this.buffer = writableChannel.map(FileChannel.MapMode.READ_WRITE, 0, bytes);
            tempStorage.scan2spectrum.forEachEntry((i, s) -> {
                offsets.put(i, buffer.position());
                buffer.putInt(s.size());
                for (int j = 0; j < s.size(); ++j) {
                    buffer.putFloat((float) s.getMzAt(j));
                }
                for (int j = 0; j < s.size(); ++j) {
                    buffer.putFloat((float) s.getIntensityAt(j));
                }
                return true;
            });
            this.totalSize = buffer.position();
            this.buffer.rewind();
        }
        this.tempStorage = null;
        this.dirty = false;
    }

    public void dropBuffer() {
        buffer.force();
        this.buffer = null;
    }

    @Override
    public void add(Scan scan, SimpleSpectrum spectrum) {
        if (tempStorage==null)
            throw new IllegalStateException();
        dirty = true;
        tempStorage.add(scan, spectrum);
    }

    @Override
    public SimpleSpectrum getScan(Scan scan) {
        if (tempStorage!=null) {
            final SimpleSpectrum scan1 = tempStorage.getScan(scan);
            if (scan1 != null) return scan1;
        }
        return readFromMemory(scan);
    }

    private synchronized SimpleSpectrum readFromMemory(Scan scan) {
        final int offset = offsets.get(scan.getScanNumber());
        if (offset < 0) return null;
        if (buffer==null) {
            try {
                buffer = writableChannel.map(FileChannel.MapMode.PRIVATE, 0, totalSize);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        buffer.position(offset);
        final int size = buffer.getInt();
        final FloatBuffer floats = buffer.asFloatBuffer();
        final float[] mz = new float[size], ints = new float[size];
        floats.get(mz);
        floats.get(ints);
        final SimpleSpectrum spectrum = new SimpleSpectrum(Spectrums.getAlreadyOrderedSpectrum(Spectrums.wrap(mz, ints)));
        if (tempStorage!=null) tempStorage.add(scan, spectrum);
        return spectrum;
    }

    @Override
    public void close() throws IOException {
        writableChannel.close();
        buffer=null;
        writableChannel=null;
    }
}
