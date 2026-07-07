package de.unijena.bioinf.ms.frontend.subtools.foldchange;

import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.storage.db.nosql.Database;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.DoubleStream;

public abstract class FoldChangeSubToolJJob<FC> extends BasicMasterJJob<List<FC>> {
    protected final AtomicLong total = new AtomicLong(0);
    protected final AtomicLong progress = new AtomicLong(0);

    protected final LongSet leftRuns, rightRuns;
    protected final String leftGroupName, rightGroupName;
    protected final SiriusProjectDocumentDatabase<? extends Database<?>> project;
    protected final EnumSet<QuantMeasure> quantMeasures;
    protected final EnumSet<AggregationType> aggregationTypes;


    public FoldChangeSubToolJJob(SiriusProjectDocumentDatabase<? extends Database<?>> project,
                                 String leftGroupName, LongSet leftRuns,
                                 String rightGroupName, LongSet rightRuns,
                                 QuantMeasure quantMeasure,
                                 AggregationType aggregationType
    ) {
        this(project, leftGroupName, leftRuns, rightGroupName, rightRuns,
                EnumSet.of(quantMeasure), EnumSet.of(aggregationType));
    }

    public FoldChangeSubToolJJob(SiriusProjectDocumentDatabase<? extends Database<?>> project,
                                 String leftGroupName, LongSet leftRuns,
                                 String rightGroupName, LongSet rightRuns,
                                 EnumSet<QuantMeasure> quantMeasures,
                                 EnumSet<AggregationType> aggregationTypes
    ) {
        super(JobType.SCHEDULER);
        this.leftRuns = leftRuns;
        this.leftGroupName = leftGroupName;
        this.rightRuns = rightRuns;
        this.rightGroupName = rightGroupName;
        this.quantMeasures = quantMeasures;
        this.aggregationTypes = aggregationTypes;
        this.project = project;
    }

    protected DoubleStream quantify(Long2ObjectMap<List<Feature>> features, QuantMeasure quantMeasure) {
        return features.values().stream().mapToDouble(featuresPerRun -> switch (quantMeasure) {
            case APEX_INTENSITY -> featuresPerRun.stream().mapToDouble(Feature::getApexIntensity).sum();
            case AREA_UNDER_CURVE -> featuresPerRun.stream().mapToDouble(Feature::getAreaUnderCurve).sum();
        });
    }

    protected double aggregate(DoubleStream values, AggregationType aggregationType) {
        return switch (aggregationType) {
            case AVG -> values.average().orElse(0.0);
            case MIN -> values.min().orElse(0.0);
            case MAX -> values.max().orElse(0.0);
        };
    }
}
