package de.unijena.bioinf.ms.persistence.model.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class QualityReport {
    @Id
    @Getter
    @Setter
    private long alignedFeatureId;

    @Setter
    @Getter
    private DataQuality overallQuality;

    @Getter
    private LinkedHashMap<String, Category> categories;

    public static QualityReport withDefaultCategories() {
        return withDefaultCategories(true);
    }

    public static QualityReport withDefaultCategories(boolean includeAlignment) {
        QualityReport r = new QualityReport();
        for (DefaultQualityCategory d : DefaultQualityCategory.values())
            if (includeAlignment || d != DefaultQualityCategory.ALIGNMENT_QUALITY)
                r.addCategory(new Category(d));
        return r;
    }

    public QualityReport() {
        this(new LinkedHashMap<>(), DataQuality.LOWEST);
    }

    public QualityReport(LinkedHashMap<String, Category> categories, DataQuality overallQuality) {
        this.categories = categories;
        this.overallQuality = overallQuality;
    }

    public void addCategory(Category category) {
        if (categories.containsKey(category.categoryId)) {
            categories.get(category.categoryId).merge(category);
        } else categories.put(category.categoryId, category);
    }





    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class Category {

        private String categoryId;

        public String getCategoryId() {
            if (categoryId == null || categoryId.isBlank()){
                return Arrays.stream(DefaultQualityCategory.values())
                        .filter(dc -> dc.getDisplayName().equals(categoryName)).findAny()
                        .map(DefaultQualityCategory::name).orElseGet(() -> Utils.toScreamingSnakeCase(categoryName));
            }
            return categoryId;
        }

        @Getter
        private String categoryName;
        @Setter
        @Getter
        private DataQuality overallQuality;
        @Getter
        private List<Item> items;

        // just for Jackson
        private Category() {
            this(null,"");
        }

        public Category(@NotNull DefaultQualityCategory defaultCategory) {
            this(defaultCategory.name(), defaultCategory.getDisplayName());
        }

        public Category(@NotNull String categoryId, @NotNull String categoryName) {
            this(categoryId, categoryName, DataQuality.LOWEST, new ArrayList<>());
        }

        public Category(String categoryId, String categoryName, DataQuality overallQuality, List<Item> items) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.overallQuality = overallQuality;
            this.items = items;
        }

        public void merge(Category same) {
            this.items.addAll(same.items);
        }

    }

    @Schema(name = "QualityWeight")
    public enum Weight {
        MINOR, MAJOR, CRITICAL;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Setter
    @Schema(name = "QualityItem")
    public static class Item {

        private String description;
        private DataQuality quality;

        private Weight weight;

        public Item() {
            this("", DataQuality.LOWEST, Weight.MINOR);
        }

        public Item(String description, DataQuality quality, Weight weight) {
            this.description = description;
            this.quality = quality;
            this.weight = weight;
        }
    }
}
