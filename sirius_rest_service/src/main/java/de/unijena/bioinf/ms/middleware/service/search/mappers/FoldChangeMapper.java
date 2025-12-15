package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.QuantRowType;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChange;
import de.unijena.bioinf.ms.middleware.model.statistics.Statistics;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.SortField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils.getDocumentIdFieldName;

public abstract class FoldChangeMapper implements FieldMapper<Collection<Statistics>> {

    protected abstract String getObjectIdFieldName();
    protected abstract QuantRowType getQuantRowType();


    @Override
    public Iterable<IndexableField> toIndexableFields(@NotNull String rootFieldName, @Nullable Collection<Statistics> stats) {
        List<IndexableField> indexableFields = new ArrayList<>();

        if (stats != null) {
            for (Statistics stat : stats) {
                StringBuilder nameBuilder = new StringBuilder(rootFieldName);
                if (stat instanceof FoldChange foldChange) {
                    nameBuilder.append(".foldChange");
                } else {
                    throw new IllegalArgumentException("Unsupported statistics type: " + stat.getClass());
                }

                nameBuilder.append(".").append(stat.getLeftGroup())
                        .append(".").append(stat.getRightGroup())
                        .append(".").append(stat.getQuantification())
                        .append(".").append(stat.getAggregation());

                indexableFields.add(new DoubleField(nameBuilder.toString(), foldChange.getFoldChange(), Field.Store.YES));
                //todo we might want sortable field here?
            }
        }
        return indexableFields;
    }

    @Override
    public @Nullable List<Statistics> toPojo(@NotNull String rootFieldName, @NotNull Iterable<IndexableField> document) {
        String prefix = rootFieldName + ".foldChange";


        String objectId = null;
        for (IndexableField storedField : document)
            if (storedField.name().equals(getObjectIdFieldName())){
                objectId = storedField.stringValue();
                break;
            }

        if (objectId == null)
            throw new IllegalStateException("Could not find objectId field: " + getObjectIdFieldName());

        List<Statistics> stats = null;
        for (IndexableField storedField : document) {
            String name = storedField.name();
            if (name.startsWith(prefix)) {
                if (stats == null)
                    stats = new ArrayList<>();
                // fold change only case add more if needed
                String[] split = name.split("[.]");

                FoldChange fc = FoldChange.builder()
                        .objectId(objectId)
                        .quantType(getQuantRowType())
                        .aggregation(AggregationType.valueOf(split[split.length - 1]))
                        .quantification(QuantMeasure.valueOf(split[split.length - 2]))
                        .rightGroup(split[split.length - 3])
                        .leftGroup(split[split.length - 4])
                        .foldChange(storedField.numericValue().doubleValue())
                        .build();

                stats.add(fc);
            }
        }
        return stats;
    }

    @Override
    public void applyAnalyzersAndPointConfigs(@NotNull String rootFieldName, @NotNull Map<String, PointsConfig> pointsConfigMap, @NotNull Map<String, Analyzer> analyzerMap, @NotNull List<CharSequence> defaultSearchFields, @NotNull Map<String, SortField.Type> sortTypes) {
        // add pointsconfig for foldchange
        pointsConfigMap.put(rootFieldName + ".foldChange.*", LuceneMappingUtils.getPointsConfigForType(Double.class));
        //add others if needed
    }

    public static class AlignedFeatureFoldChange extends FoldChangeMapper {
        final static String OBJECT_ID_FIELD_NAME = getDocumentIdFieldName(AlignedFeature.class).orElseThrow();;

        @Override
        protected String getObjectIdFieldName() {
            return OBJECT_ID_FIELD_NAME;
        }

        @Override
        protected QuantRowType getQuantRowType() {
            return QuantRowType.FEATURES;
        }
    }

    public static class CompoundFoldChange extends FoldChangeMapper {
        final static String OBJECT_ID_FIELD_NAME = getDocumentIdFieldName(Compound.class).orElseThrow();

        @Override
        protected String getObjectIdFieldName() {
            return OBJECT_ID_FIELD_NAME;
        }

        @Override
        protected QuantRowType getQuantRowType() {
            return QuantRowType.COMPOUNDS;
        }
    }
}
