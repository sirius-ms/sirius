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

package de.unijena.bioinf.babelms.binary;

import de.unijena.bioinf.ChemistryBase.ms.lcms.*;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
public class MassTraceIo {

    private static int SCHEMA_ID = 1;

    public CoelutingTraceSet[] readAll(InputStream stream) throws IOException {
        DataInputStream in = new DataInputStream(stream);
        final int version = in.readInt();
        if (version > SCHEMA_ID) {
            LoggerFactory.getLogger(MassTraceIo.class).warn("binary file is of schema id " + version + " but SIRIUS uses still schema id  " + SCHEMA_ID + " for reading chromatographic cache information.");
        }
        final int nsamples = in.readInt();
        final CoelutingTraceSet[] traceSets = new CoelutingTraceSet[nsamples];
        final String[] names = new String[nsamples];
        final MsDataSourceReference[] refs = new MsDataSourceReference[nsamples];
        // read libraries
        {
            String[] fileNames = new String[nsamples],
                    runids = new String[nsamples], mzmlIds = new String[nsamples];
            URI[] sourceLoc = new URI[nsamples];
            for (int k=0; k < refs.length; ++k) {
                names[k] = in.readUTF();
            }
            for (int k=0; k < refs.length; ++k) {
                String s = in.readUTF();
                if (!s.isEmpty()) {
                    sourceLoc[k] = URI.create(s);
                }
            }
            for (int k=0; k < refs.length; ++k) {
                String s = in.readUTF();
                if (!s.isEmpty()) {
                    fileNames[k] = s;
                }
            }
            for (int k=0; k < refs.length; ++k) {
                String s = in.readUTF();
                if (!s.isEmpty()) {
                    mzmlIds[k] = s;
                }
            }
            for (int k=0; k < refs.length; ++k) {
                String s = in.readUTF();
                if (!s.isEmpty()) {
                    runids[k] = s;
                }
            }
            for (int k=0; k < refs.length; ++k) {
                refs[k] = new MsDataSourceReference(
                  sourceLoc[k], fileNames[k], runids[k], mzmlIds[k]
                );
            }
        }
        // read traces
        for (int k=0; k < traceSets.length; ++k) {
            traceSets[k] = readTrace(in, names[k], refs[k]);
        }
        return traceSets;
    }

    public void writeAll(OutputStream stream, CoelutingTraceSet[] traceSets) throws IOException {
        DataOutputStream out = new DataOutputStream(stream);
        // write version
        out.writeInt(SCHEMA_ID);
        // write libraries
        out.writeInt(traceSets.length);
        {

            for (CoelutingTraceSet set : traceSets) {
                out.writeUTF(set.getSampleName());
            }
            for (CoelutingTraceSet set : traceSets) {
                out.writeUTF(set.getSampleRef().getSourceLocation().map(URI::toString).orElse(""));
            }
            for (CoelutingTraceSet set : traceSets) {
                out.writeUTF(set.getSampleRef().getFileName().orElse(""));
            }
            for (CoelutingTraceSet set : traceSets) {
                out.writeUTF(set.getSampleRef().getMzmlId().orElse(""));
            }
            for (CoelutingTraceSet set : traceSets) {
                out.writeUTF(set.getSampleRef().getRunId().orElse(""));
            }
            out.flush();
        }
        // now write tracesets
        for (CoelutingTraceSet t : traceSets)
            writeTrace(out,t);
    }

    public void writeTrace(DataOutputStream stream, CoelutingTraceSet traceset) throws IOException {
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

        // ms2 scans
        stream.writeInt(traceset.getMs2RetentionTimes().length);
        for (int id : traceset.getMs2ScanIds())
            stream.writeInt(id);
        for (long time : traceset.getMs2RetentionTimes())
            stream.writeLong(time);

        // metadata
        stream.writeByte(5); // metadata version information
        stream.writeInt(0); // metadata skip bytes

        // reports
        stream.writeByte(5); // report version information
        final ObjectOutputStream obj = new ObjectOutputStream(stream);
        obj.writeObject(traceset.getReports());
        obj.flush();
    }

    private CoelutingTraceSet readTrace(DataInputStream stream, String name, MsDataSourceReference ref) throws IOException {

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
            adducts[i] = new IonTrace(readIon(stream), null);
        }

        if (stream.readByte() != 41)
            throw new IOException("Invalid stream");

        int numberOfInsources = stream.readInt();
        final IonTrace[] insources = new IonTrace[numberOfInsources];
        for (int i=0; i < numberOfInsources; ++i) {
            insources[i] = new IonTrace(readIon(stream), null);
        }

        // read ms2 scan ids
        int numberOfMs2Scans = stream.readInt();
        final int[] ms2ScanIds = new int[numberOfMs2Scans];
        final long[] ms2RetentionTimes = new long[numberOfMs2Scans];
        for (int i=0; i < numberOfMs2Scans; ++i) {
            ms2ScanIds[i] = stream.readInt();
        }
        for (int i=0; i < numberOfMs2Scans; ++i) {
            ms2RetentionTimes[i] = stream.readLong();
        }

        // skip metadata
        byte metaDataVersion = stream.readByte();
        if (metaDataVersion!=5) {
            throw new IOException("Unknown metadata version '" + metaDataVersion + "'");
        }
        int metadataSize = stream.readInt();
        if (metadataSize>0) stream.skipBytes(metadataSize);

        byte reportVersion = stream.readByte();
        if (reportVersion!=5) {
            throw new IOException("Unknown report version '" + reportVersion + "'");
        }
        final ObjectInputStream obj = new ObjectInputStream(stream);
        try {
            final CompoundReport[] reports = (CompoundReport[]) obj.readObject();

            return new CoelutingTraceSet(
                    name,ref,new CompoundTrace(mainIonTraces,null,adducts,insources),retentionTimes,scanIds,noiseLevels, ms2ScanIds, ms2RetentionTimes, reports
            );
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }

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
