package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ms.persistence.model.core.feature.AbstractAlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import de.unijena.bioinf.ms.persistence.storage.MsProjectDocumentDatabase;
import de.unijena.bioinf.storage.db.nosql.Filter;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class ProjectSpaceTraceProvider implements TraceProvider {

    protected final MsProjectDocumentDatabase<?> storage;

    public ProjectSpaceTraceProvider(MsProjectDocumentDatabase<?> storage) {
        this.storage = storage;
    }

    @Override
    public Optional<MergedTrace> getMergeTrace(AbstractAlignedFeatures feature) {
        return feature.getTraceReference().map(id-> {
            Iterator<MergedTrace> iter = null;
            try {
                iter = storage.getStorage().find(Filter.where("mergedTraceId").eq(id.getTraceId()), MergedTrace.class).iterator();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (iter.hasNext()) {
                return iter.next();
            } else return null;
        });
    }

    @Override
    public Long2ObjectMap<SourceTrace> getSourceTraces(AbstractAlignedFeatures features) {
        Long2ObjectOpenHashMap<SourceTrace> traces = new Long2ObjectOpenHashMap<>();
        for (Feature f : getFeatures(features)) {
            if (f.getTraceReference().isPresent()) {
                TraceRef traceRef = f.getTraceReference().get();
                Iterator<SourceTrace> iter = null;
                try {
                    iter = storage.getStorage().find(Filter.where("sourceTraceId").eq(traceRef.getTraceId()), SourceTrace.class).iterator();
                    if (iter.hasNext()) {
                        traces.put(f.getRunId(), iter.next());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return traces;
    }

    @Override
    public Optional<Pair<TraceRef, SourceTrace>> getSourceTrace(AbstractAlignedFeatures features, long runId) {
        Long2ObjectOpenHashMap<SourceTrace> traces = new Long2ObjectOpenHashMap<>();
        for (Feature f : getFeatures(features)) {
            if (f.getTraceReference().isPresent() && f.getRunId()==runId) {
                TraceRef traceRef = f.getTraceReference().get();
                Iterator<SourceTrace> iter = null;
                try {
                    iter = storage.getStorage().find(Filter.where("sourceTraceId").eq(traceRef.getTraceId()), SourceTrace.class).iterator();
                    if (iter.hasNext()) {
                        return Optional.of(Pair.of(traceRef, iter.next()));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Long2DoubleMap getIntensities(AbstractAlignedFeatures features) {
        Long2DoubleMap map = new Long2DoubleOpenHashMap();
        for (Feature f : getFeatures(features)) {
            map.put(f.getRunId(), f.getApexIntensity());
        }
        return map;
    }

    private List<Feature> getFeatures(AbstractAlignedFeatures feature)  {
        if (feature.getFeatures().isEmpty()) {
            try {
                return storage.getStorage().findStr(Filter.where("alignedFeatureId").eq(feature.databaseId()), Feature.class).toList();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else return feature.getFeatures().get();
    }

    @Override
    public List<MergedMSnSpectrum> getMs2SpectraOf(AbstractAlignedFeatures features) {
        try {
            return storage.getStorage().findStr(Filter.where("alignedFeatureId").eq(features.databaseId()), MSData.class).filter(x->x.getMsnSpectra()!=null).flatMap(x->x.getMsnSpectra().stream()).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
