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

package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.lcms.MsDataSourceReference;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import org.apache.commons.lang3.Range;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class LCMSRun implements Annotated<DataAnnotation>, Iterable<Scan>  {

    protected DataSource source;
    protected String identifier;
    protected MsInstrumentation instrument;
    protected EnumSet<MsDataProcessing> processings;
    protected TreeMap<Integer, Scan> scans;

    protected MsDataSourceReference reference;

    protected Annotated.Annotations<DataAnnotation> annotations;

    public LCMSRun(DataSource source) {
        this.source = source;
        this.instrument = MsInstrumentation.Unknown;
        this.processings = EnumSet.noneOf(MsDataProcessing.class);
        this.scans = new TreeMap<>();
        this.annotations = new Annotated.Annotations();
        this.identifier = source.getName();
    }

    public MsDataSourceReference getReference() {
        return reference;
    }

    public void setReference(MsDataSourceReference reference) {
        this.reference = reference;
    }

    public Range<Integer> scanRange() {
        return Range.of(scans.firstKey(), scans.lastKey());
    }

    @Deprecated
    public DataSource getSource() {
        return source;
    }

    public Collection<Scan> getScans() {
        return scans.values();
    }

    public NavigableMap<Integer, Scan> getScansFrom(int from) {
        return scans.headMap(from,true).descendingMap();
    }
    public NavigableMap<Integer, Scan> getScansAfter(int after) {
        return scans.tailMap(after,false);
    }
    public NavigableMap<Integer, Scan> getScansBefore(int to) {
        return scans.headMap(to,false).descendingMap();
    }
    public NavigableMap<Integer,Scan> getScans(int from, int to) {
        return scans.subMap(from, true, to, true);
    }
    public NavigableMap<Integer,Scan> getScans(int from, int to, boolean toInclusive) {
        return scans.subMap(from, true, to, toInclusive );
    }

    @Override
    public Annotations<DataAnnotation> annotations() {
        return annotations;
    }

    public MsInstrumentation getInstrument() {
        return instrument;
    }

    public void setInstrument(MsInstrumentation instrument) {
        this.instrument = instrument;
    }

    public EnumSet<MsDataProcessing> getProcessings() {
        return processings;
    }

    public void addScan(Scan scan) {
        scans.put(scan.getIndex(), scan);
    }


    public Optional<Scan> getScanByNumber(int scanNumber) {
        Scan scan = scans.get(scanNumber);
        return scan==null ? Optional.empty() : Optional.of(scan);
    }

    public String getIdentifier() {
        return identifier;
    }

    @NotNull
    @Override
    public Iterator<Scan> iterator() {
        return scans.values().iterator();
    }

    public Range<Long> retentionTimeRange() {
        return Range.of(scans.firstEntry().getValue().getRetentionTime(), scans.lastEntry().getValue().getRetentionTime());
    }
}
