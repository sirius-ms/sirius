package de.unijena.bioinf.ChemistryBase.ms.lcms;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Optional;

/**
 * Stores all chromatographic information about a certain compound.
 *
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class LCMSPeakInformation implements DataAnnotation, Ms2ExperimentAnnotation {

    private final static LCMSPeakInformation _empty_ = new LCMSPeakInformation(new CoelutingTraceSet[0]);

    @JsonIgnore
    public static LCMSPeakInformation empty() {
        return _empty_;
    }

    @JsonProperty(value = "traceSets",access = JsonProperty.Access.READ_ONLY)
    protected final CoelutingTraceSet[] traceSet;

    @JsonProperty(value = "sampleNames", access = JsonProperty.Access.READ_ONLY)
    protected final String[] sampleNames;

    @JsonProperty(value = "sourceReferences", access = JsonProperty.Access.READ_ONLY)
    protected final MsDataSourceReference[] sourceReferences;

    @JsonIgnore protected final QuantificationTable quantificationTable;
    @JsonIgnore protected final TObjectIntHashMap<String> name2index;


    @JsonIgnore
    public LCMSPeakInformation(QuantificationTable table) {
        this.traceSet = new CoelutingTraceSet[table.length()];
        this.sampleNames = new String[table.length()];
        this.sourceReferences = new MsDataSourceReference[table.length()];
        this.quantificationTable = table;
        this.name2index = new TObjectIntHashMap<>(table.length(), 0.75f, -1);
        for (int k=0, n=table.length(); k < n; ++k) {
            name2index.put(table.getName(k),k);
            sampleNames[k] = table.getName(k);
        }
    }

    @JsonIgnore
    public LCMSPeakInformation(CoelutingTraceSet[] traceSets) {
        this.traceSet = traceSets;
        this.sampleNames = new String[traceSets.length];
        this.sourceReferences = new MsDataSourceReference[traceSets.length];
        this.name2index = new TObjectIntHashMap<>(traceSets.length, 0.75f, -1);
        for (int i=0; i < traceSets.length; ++i) {
            sampleNames[i] = traceSets[i].getSampleName();
            sourceReferences[i] = traceSets[i].getSampleRef();
            name2index.put(sampleNames[i],i);
        }
        this.quantificationTable = quantificationTableFromTraceSet();
    }

    @JsonIgnore // wtf this is so annoying
    public boolean isEmpty() {
        return traceSet.length==0;
    }

    public int length() {
        return traceSet.length;
    }

    @JsonIgnore
    public Optional<CoelutingTraceSet> getTracesFor(String sampleName) {
        int index = name2index.get(sampleName);
        if (index>=0) return getTracesFor(index);
        else return Optional.empty();
    }

    public double getIntensityOf(String name) {
        return getIntensityOf(getIndexOf(name));
    }
    public double getIntensityOf(int index) {
        if (index<0) return 0;
        if (quantificationTable!=null) {
            return quantificationTable.getAbundance(index);
        } else {
            return getTracesFor(index).map(x->x.getIonTrace().getMonoisotopicPeak().getApexIntensity()).orElse(0f).doubleValue();
        }
    }

    @JsonIgnore
    public Optional<CoelutingTraceSet> getTracesFor(int index) {
        return Optional.ofNullable(traceSet[index]);
    }

    @JsonIgnore
    public Optional<MsDataSourceReference> getSourceReferenceFor(int index) {
        return Optional.ofNullable(sourceReferences[index]);
    }

    @JsonIgnore
    public Optional<MsDataSourceReference> getSourceReferenceFor(String name) {
        int index = name2index.get(name);
        if (index>=0) return getSourceReferenceFor(index);
        else return Optional.empty();
    }

    @JsonIgnore
    public String getNameFor(int index) {
        return sampleNames[index];
    }

    @JsonIgnore
    public int getIndexOf(String name) {
        return name2index.get(name);
    }


    public QuantificationTable getQuantificationTable() {
        return quantificationTable;
    }

    private QuantificationTable quantificationTableFromTraceSet() {
        final double[] vector = new double[traceSet.length];
        for (int k=0; k < traceSet.length; ++k) {
            vector[k] = traceSet[k].getIonTrace().getMonoisotopicPeak().getApexIntensity();
        }
        return new QuantificationTableImpl(vector,QuantificationMeasure.APEX);
    }

    private class QuantificationTableImpl implements QuantificationTable {
        private final double[] vector;
        private final QuantificationMeasure measure;

        public QuantificationTableImpl(QuantificationTable table) {
            this(table.getAsVector(), table.getMeasure());
        }
        public QuantificationTableImpl(double[] vector, QuantificationMeasure measure) {
            this.vector = vector;
            this.measure = measure;
        }

        @Override
        public String getName(int i) {
            return sampleNames[i];
        }

        @Override
        public double getAbundance(int i) {
            return vector[i];
        }

        @Override
        public double getAbundance(String name) {
            int i = name2index.get(name);
            return i>=0 ? vector[i] : 0d;
        }

        @Override
        public Optional<Double> mayGetAbundance(String name) {
            int i = name2index.get(name);
            return i>=0 ? Optional.of(vector[i]) : Optional.empty();
        }

        @Override
        public int length() {
            return vector.length;
        }

        @Override
        public QuantificationMeasure getMeasure() {
            return measure;
        }
    }

    //////
    ////// jackson
    //////


    /*
        Jackson constructor
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public LCMSPeakInformation(
            @JsonProperty(value = "traceSets", access = JsonProperty.Access.WRITE_ONLY) CoelutingTraceSet[] traceSets,
            @JsonProperty(value = "sampleNames", access = JsonProperty.Access.WRITE_ONLY) String[] sampleNames,
            @JsonProperty(value = "sourceReferences", access = JsonProperty.Access.WRITE_ONLY) MsDataSourceReference[] sourceReferences,
            @JsonProperty(value = "abundance", access = JsonProperty.Access.WRITE_ONLY) double[] abundance,
            @JsonProperty(value = "measure", access = JsonProperty.Access.WRITE_ONLY)  QuantificationMeasure measure) {
        this.traceSet = substituteNames(traceSets, sampleNames,sourceReferences);
        this.sampleNames = sampleNames;
        this.sourceReferences = sourceReferences;
        this.name2index = new TObjectIntHashMap<>(sampleNames.length, 0.75f, -1);
        for (int k=0; k < sampleNames.length; ++k) name2index.put(sampleNames[k],k);
        this.quantificationTable = new QuantificationTableImpl(abundance, measure);
    }



    private CoelutingTraceSet[] substituteNames(CoelutingTraceSet[] traceSets,String[] names, MsDataSourceReference[] refs) {
        final CoelutingTraceSet[] re = new CoelutingTraceSet[traceSets.length];
        for (int k=0; k < re.length; ++k) {
            if (traceSets[k]!=null) {
                re[k] = new CoelutingTraceSet(names[k], refs[k], traceSets[k].ionTrace, traceSets[k].retentionTimes, traceSets[k].scanIds, traceSets[k].noiseLevels, traceSets[k].ms2ScanIds, traceSets[k].ms2RetentionTimes, traceSets[k].reports);
            }
        }
        return re;
    }

    @JsonProperty(value = "abundance", access = JsonProperty.Access.READ_ONLY)
    protected double[] getQuantificationVector() {
        return quantificationTable.getAsVector();
    }

    @JsonProperty(value = "measure", access = JsonProperty.Access.READ_ONLY)
    protected QuantificationMeasure getQuantificationMeasure() {
        return quantificationTable.getMeasure();
    }



}
