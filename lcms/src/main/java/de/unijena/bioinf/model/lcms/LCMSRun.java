package de.unijena.bioinf.model.lcms;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class LCMSRun implements Annotated<DataAnnotation>, Iterable<Scan>  {

    protected DataSource source;
    protected String identifier;
    protected MsInstrumentation instrument;
    protected EnumSet<MsDataProcessing> processings;
    protected TreeMap<Integer, Scan> scans;

    protected Annotated.Annotations<DataAnnotation> annotations;

    public LCMSRun(DataSource source) {
        this.source = source;
        this.instrument = MsInstrumentation.Unknown;
        this.processings = EnumSet.noneOf(MsDataProcessing.class);
        this.scans = new TreeMap<>();
        this.annotations = new Annotated.Annotations();
        String name = new File(source.getUrl().getFile()).getName();
        int li = name.lastIndexOf('.');
        if (li>=0) name = name.substring(0,li);
        this.identifier = name;
    }


    public Range<Integer> scanRange() {
        return Range.closed(scans.firstKey(), scans.lastKey());
    }

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
        return scans.subMap(from, true, to, false);
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
        scans.put(scan.getScanNumber(), scan);
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
        return Range.closed(scans.firstEntry().getValue().getRetentionTime(), scans.lastEntry().getValue().getRetentionTime());
    }
}
