package de.unijena.bioinf.ms.middleware;

import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiStructureMatch;
import de.unijena.bioinf.ms.persistence.model.sirius.DenovoStructureMatch;
import de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate;
import de.unijena.bioinf.ms.persistence.model.sirius.SpectraMatch;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static de.unijena.bioinf.ChemistryBase.utils.Utils.LARGE_BATCH_SIZE;

public class Pages {
    private static final Map<Class<?>, Function<Sort,Pair<String[], Database.SortOrder[]>>> sortMapper = Map.of(
            AlignedFeatures.class, Pages::sortFeature,
            Compound.class, Pages::sortCompound,
            LCMSRun.class, Pages::sortRun,
            QualityReport.class, Pages::sortFeature,
            SpectraMatch.class, Pages::sortMatch,
            FormulaCandidate.class, Pages::sortFormulaCandidate,
            CsiStructureMatch.class, Pages::sortStructureMatch,
            DenovoStructureMatch.class, Pages::sortStructureMatch,
            FoldChange.AlignedFeaturesFoldChange.class, Pages::sortRun,
            FoldChange.CompoundFoldChange.class, Pages::sortRun
    );

    /**
     *
     * @param clzz to be retrieved from database
     * @throws IllegalArgumentException if there is no sortmapper defined for provided class.
     */
    @NotNull
    public static Function<Sort,Pair<String[], Database.SortOrder[]>> sortMapper(Class<?> clzz) throws IllegalArgumentException{
        Function<Sort, Pair<String[], Database.SortOrder[]>> mapper = sortMapper.get(clzz);
        if (mapper == null)
            throw new IllegalArgumentException("No sort mapper found for class " + clzz);
        return mapper;
    }


    public static <T> void forEach(Function<Pageable, Page<T>> pageProvider, Consumer<Page<T>> pageConsumer){
        forEach(LARGE_BATCH_SIZE, pageProvider, pageConsumer);
    }

    public static <T> void forEach(int pageSize, Function<Pageable, Page<T>> pageProvider, Consumer<Page<T>> pageConsumer){
        forEach(PageRequest.ofSize(pageSize), pageProvider, pageConsumer);
    }

    public static  <T> void forEachFrom(int pageNumberFrom, Function<Pageable, Page<T>> pageProvider, Consumer<Page<T>> pageConsumer){
        forEachFrom(pageNumberFrom, LARGE_BATCH_SIZE, pageProvider, pageConsumer);
    }

    public static  <T> void forEachFrom(int pageNumberFrom, int pageSize, Function<Pageable, Page<T>> pageProvider, Consumer<Page<T>> pageConsumer){
        forEach(PageRequest.of(pageNumberFrom, pageSize), pageProvider, pageConsumer);
    }

    public static  <T> void forEach(@NotNull Pageable startPage, Function<Pageable, Page<T>> pageProvider, Consumer<Page<T>> pageProcessor){
        Pageable pageable = startPage;
        Page<T> page;
        do {
            page = pageProvider.apply(pageable); // Fetch the current page
            pageProcessor.accept(page); // Your custom method to process the page

            pageable = page.hasNext() ? page.nextPageable() : Pageable.unpaged(); // Move to next page

        } while (page.hasNext());
    }

    public static <T> Page<T> findPage(@NotNull final Database<?> storage, Class<T> clz, Pageable pageable, Filter sortFilter) throws IOException {
        return makePage(storage, pageable, findPageStr(storage, clz, pageable, sortFilter).toList(), clz, sortFilter);
    }


    public static <T> Stream<T> findPageStr(@NotNull final Database<?> storage, Class<T> clz, Pageable pageable, Filter sortFilter) throws IOException {
        return findPageStr(storage, clz, pageable, sortFilter, sortMapper(clz));
    }

    public static <T> Page<T> findPage(@NotNull final Database<?> storage, Class<T> clz, Pageable pageable) throws IOException {
        return makePage(storage, pageable, findPageStr(storage, clz, pageable, sortMapper(clz)).toList());
    }

    public static <T> Stream<T> findPageStr(@NotNull final Database<?> storage, Class<T> clz, Pageable pageable) throws IOException {
        return findPageStr(storage, clz, pageable, sortMapper(clz));
    }

    public static <T> Page<T> findPage(@NotNull final Database<?> storage, Class<T> clz, Pageable pageable,
                                            @NotNull Filter sortFilter,
                                            @NotNull Function<Sort, Pair<String[], Database.SortOrder[]>> sortTransformer
    ) throws IOException {
        return makePage(storage, pageable, findPageStr(storage, clz, pageable, sortFilter, sortTransformer).toList(), clz, sortFilter);
    }

    public static <T> Stream<T> findPageStr(@NotNull final Database<?> storage, Class<T> clz, Pageable pageable,
                                            @NotNull Filter sortFilter,
                                            @NotNull Function<Sort, Pair<String[], Database.SortOrder[]>> sortTransformer
    ) throws IOException {
        if (pageable.isUnpaged() && pageable.getSort().isUnsorted())
            return storage.findStr(sortFilter, clz);
        if (pageable.getSort().isUnsorted())
            return storage.findStr(sortFilter, clz, pageable.getOffset(), pageable.getPageSize());

        Pair<String[], Database.SortOrder[]> sort = sortTransformer.apply(pageable.getSort());
        if (pageable.isUnpaged())
            return storage.findStr(sortFilter, clz, sort.getLeft(), sort.getRight());

        return storage.findStr(sortFilter, clz, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight());
    }

    public static <T> Page<T> findPage(@NotNull final Database<?> storage, Class<T> clz, Pageable pageable,
                                            Function<Sort, Pair<String[], Database.SortOrder[]>> sortTransformer
    ) throws IOException {
        return makePage(storage, pageable, findPageStr(storage, clz, pageable, sortTransformer).toList());
    }


    public static <T> Stream<T> findPageStr(@NotNull final Database<?> storage, Class<T> clz, Pageable pageable,
                                            Function<Sort, Pair<String[], Database.SortOrder[]>> sortTransformer
    ) throws IOException {
        if (pageable.isUnpaged() && pageable.getSort().isUnsorted())
            return storage.findAllStr(clz);
        if (pageable.getSort().isUnsorted())
            return storage.findAllStr(clz, pageable.getOffset(), pageable.getPageSize());

        Pair<String[], Database.SortOrder[]> sort = sortTransformer.apply(pageable.getSort());
        if (pageable.isUnpaged())
            return storage.findAllStr(clz, sort.getLeft(), sort.getRight());

        return storage.findAllStr(clz, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight());
    }

    @SneakyThrows
    public static <C> Page<C> makePage(@NotNull final Database<?> storage, @NotNull Pageable pageable, @NotNull List<C> content) {
        if (content.isEmpty())
            return Page.empty();
        long total = pageable.isUnpaged() ? content.size() : storage.countAll(content.getFirst().getClass());
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Creates a page of objects that have been selected by the given filter. In contrast to
     * {@link #makePage(Database, Pageable, List)} the total number of elements refers to the objects matching the
     * filter and not to all objects of the collection.
     */
    public static <C> Page<C> makePage(@NotNull final Database<?> storage, @NotNull Pageable pageable,
                                       @NotNull List<C> content, @NotNull Class<?> clz, @NotNull Filter filter) throws IOException {
        if (content.isEmpty())
            return Page.empty();
        long total = pageable.isUnpaged() ? content.size() : storage.count(filter, clz);
        return new PageImpl<>(content, pageable, total);
    }



    private static Pair<String[], Database.SortOrder[]> sort(Sort sort, Pair<String, Database.SortOrder> defaults, Function<String, String> translator) {
        if (sort == null || sort.isEmpty() || sort == Sort.unsorted())
            return Pair.of(new String[]{defaults.getLeft()}, new Database.SortOrder[]{defaults.getRight()});

        List<String> properties = new ArrayList<>();
        List<Database.SortOrder> orders = new ArrayList<>();
        sort.stream().forEach(s -> {
            properties.add(translator.apply(s.getProperty()));
            orders.add(s.getDirection().isAscending() ? Database.SortOrder.ASCENDING : Database.SortOrder.DESCENDING);
        });
        return Pair.of(properties.toArray(String[]::new), orders.toArray(Database.SortOrder[]::new));
    }

    public static Pair<String[], Database.SortOrder[]> sortRun(Sort sort) {
        return sort(sort, Pair.of("name", Database.SortOrder.ASCENDING), Function.identity());
    }

    public static Pair<String[], Database.SortOrder[]> sortCompound(Sort sort) {
        return sort(sort, Pair.of("rt.middle", Database.SortOrder.ASCENDING), s -> switch (s) {
            case "rtStartSeconds" -> "rt.start";
            case "rtEndSeconds" -> "rt.end";
            default -> s;
        });
    }

    public static Pair<String[], Database.SortOrder[]> sortFeature(Sort sort) {
        return sort(sort, Pair.of("retentionTime.middle", Database.SortOrder.ASCENDING), s -> switch (s) {
            case "rtStartSeconds" -> "retentionTime.start";
            case "rtEndSeconds" -> "retentionTime.end";
            case "ionMass" -> "averageMass";
            default -> s;
        });
    }

    public static Pair<String[], Database.SortOrder[]> sortMatch(Sort sort) {
        return sort(sort, Pair.of("searchResult.rank", Database.SortOrder.ASCENDING), s -> switch (s) {
            case "rank" -> "searchResult.rank";
            case "similarity" -> "searchResult.similarity.similarity";
            case "sharedPeaks" -> "searchResult.similarity.sharedPeaks";
            default -> s;
        });
    }

    public static Filter spectralMatchFilter(String alignedFeatureId, int minSharedPeaks, double minSimilarity) {
        long longId = Long.parseLong(alignedFeatureId);
        return Filter.and(
                Filter.where("alignedFeatureId").eq(longId),
                Filter.where("searchResult.similarity.sharedPeaks").gte(minSharedPeaks),
                Filter.where("searchResult.similarity.similarity").gte(minSimilarity)
        );
    }

    public static Filter spectralMatchInchiFilter(String alignedFeatureId, String candidateInchi, int minSharedPeaks, double minSimilarity) {
        long longId = Long.parseLong(alignedFeatureId);
        return Filter.and(
                Filter.where("alignedFeatureId").eq(longId),
                Filter.where("searchResult.candidateInChiKey").eq(candidateInchi),
                Filter.where("searchResult.similarity.sharedPeaks").gte(minSharedPeaks),
                Filter.where("searchResult.similarity.similarity").gte(minSimilarity)
        );
    }

    public static Pair<String[], Database.SortOrder[]> sortFormulaCandidate(Sort sort) {
        return sort(sort, Pair.of("formulaRank", Database.SortOrder.ASCENDING), Function.identity());
    }

    public static Pair<String[], Database.SortOrder[]> sortStructureMatch(Sort sort) {
        return sort(sort, Pair.of("structureRank", Database.SortOrder.ASCENDING), Function.identity());
    }
}
