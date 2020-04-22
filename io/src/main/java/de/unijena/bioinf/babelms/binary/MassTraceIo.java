package de.unijena.bioinf.babelms.binary;

import de.unijena.bioinf.ChemistryBase.ms.lcms.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Optional;

public class MassTraceIo {

    public CoelutingTraceSet[] readAll(InputStream stream, SampleLibrary library) throws IOException {
        final ArrayList<CoelutingTraceSet> traceSets = new ArrayList<>();
        CoelutingTraceSet set;
        while ((set=read(stream,library))!=null) {
            traceSets.add(set);
        }
        return traceSets.toArray(CoelutingTraceSet[]::new);
    }

    public void writeAll(OutputStream stream, SampleLibrary library, CoelutingTraceSet[] traceSets) throws IOException {
        for (CoelutingTraceSet t : traceSets)
            write(stream,library,t);
    }

    public void writeAll(OutputStream stream, SampleLibrary library, Iterable<CoelutingTraceSet> traceSets) throws IOException {
        for (CoelutingTraceSet t : traceSets)
            write(stream,library,t);
    }

    public CoelutingTraceSet read(InputStream stream, SampleLibrary library) throws IOException {
        byte[] bytes = new byte[4];
        int read = stream.read(bytes);
        if (read < 4) return null;
        int length = ((bytes[0] << 24) + (bytes[1] << 16) + (bytes[2] << 8) + (bytes[3]));
        bytes = new byte[length];
        return fromBytes(bytes,library);
    }

    public void write(OutputStream stream, SampleLibrary library, CoelutingTraceSet traceset) throws IOException {
        final byte[] bytes = toBytes(library, traceset);
        // write number of bytes
        {
            final int v = bytes.length;
            stream.write((v >>> 24) & 0xFF);
            stream.write((v >>> 16) & 0xFF);
            stream.write((v >>> 8) & 0xFF);
            stream.write((v) & 0xFF);
        }
        // write bytes
        stream.write(bytes);
    }

    private CoelutingTraceSet fromBytes(byte[] bytes, SampleLibrary library) throws IOException {
        final DataInputStream stream = new DataInputStream(new ByteArrayInputStream(bytes));

        final int ID = stream.readInt();
        final String name = library.getNameAt(ID);
        final MsDataSourceReference ref = library.getReferenceAt(ID);

        final int numberOfscanIds = stream.readInt();
        final int[] scanIds = new int[numberOfscanIds];
        final long[] retentionTimes = new long[numberOfscanIds];
        final float[] noiseLevels = new float[numberOfscanIds];

        for (int i=0; i < numberOfscanIds; ++i) {
            scanIds[i] = stream.readInt();
        }
        for (int i=0; i < numberOfscanIds; ++i) {
            retentionTimes[i] = stream.readLong();
        }
        for (int i=0; i < numberOfscanIds; ++i) {
            noiseLevels[i] = stream.readFloat();
        }

        final Trace[] mainIonTraces = readIon(stream);

        int numberOfAdducts = stream.readInt();
        final IonTrace[] adducts = new IonTrace[numberOfAdducts];
        for (int i=0; i < numberOfAdducts; ++i) {
            adducts[i] = new IonTrace(readIon(stream));
        }

        if (stream.readByte() != 41)
            throw new IOException("Invalid stream");

        int numberOfInsources = stream.readInt();
        final IonTrace[] insources = new IonTrace[numberOfAdducts];
        for (int i=0; i < numberOfAdducts; ++i) {
            insources[i] = new IonTrace(readIon(stream));
        }

        // skip metadata
        int metadataSize = stream.readInt();
        if (metadataSize>0) stream.read(new byte[metadataSize]);

        return new CoelutingTraceSet(
          name,ref,new CompoundTrace(mainIonTraces,adducts,insources),retentionTimes,scanIds,noiseLevels
        );

    }

    private byte[] toBytes(SampleLibrary library, CoelutingTraceSet traceset) throws IOException {

        final Optional<Integer> ID = library.getIndexFor(traceset.getSampleRef());
        if (ID.isEmpty()) throw new IOException("Unknown sample id " + traceset.getSampleRef());

        // we first write into a byte channel
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(2048);
        final DataOutputStream stream = new DataOutputStream(buffer);
        stream.writeInt(ID.get());

        stream.writeInt(traceset.getScanIds().length);
        for (int i : traceset.getScanIds())
            stream.writeInt(i);
        for (long i : traceset.getRetentionTimes())
            stream.writeLong(i);
        for (float f : traceset.getNoiseLevels())
            stream.writeFloat(f);
        writeIon(stream, traceset.getIonTrace().getIsotopes());
        stream.writeInt(traceset.getIonTrace().getAdducts().length);
        for (IonTrace adduct : traceset.getIonTrace().getAdducts())
            writeIon(stream, adduct.getIsotopes());

        stream.writeByte(41); // alignment check

        stream.writeInt(traceset.getIonTrace().getInSourceFragments().length);
        for (IonTrace insource : traceset.getIonTrace().getInSourceFragments())
            writeIon(stream, insource.getIsotopes());

        // metadata
        stream.writeInt(0);

        stream.close();
        return buffer.toByteArray();
    }

    private Trace[] readIon(DataInputStream stream) throws IOException {
        final int n = stream.readInt();
        final Trace[] traces = new Trace[n];
        for (int i=0; i < n; ++i) {
            int io = stream.readInt();
            int fo = stream.readInt();
            int fl = stream.readInt();
            int il = stream.readInt();
            final double[] masses = new double[il];
            final float[] intensities = new float[il];
            for (int j=0; j < il; ++j) {
                masses[j] = stream.readDouble();
            }
            for (int j=0; j < il; ++j) {
                intensities[j] = stream.readFloat();
            }
            traces[i] = new Trace(io,fo,fl,masses,intensities);
        }

        // skip metadata
        int metadataSize = stream.readInt();
        if (metadataSize>0) stream.read(new byte[metadataSize]);

        return traces;
    }

    private void writeIon(DataOutputStream stream, Trace[] traces) throws IOException {
        stream.writeInt(traces.length);
        for (Trace  t : traces) {
            stream.writeInt(t.getIndexOffset());
            stream.writeInt(t.getDetectedFeatureOffset());
            stream.writeInt(t.getDetectedFeatureLength());
            stream.writeInt(t.getIntensities().length);
            for (double m : t.getMasses())
                stream.writeDouble(m);
            for (float m : t.getIntensities())
                stream.writeFloat(m);
        }

        // metadata
        stream.writeInt(0);
    }

}
