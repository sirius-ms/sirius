package de.unijena.bioinf.ms.persistence.model.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class QualityReport {

    public static final String PEAK_QUALITY = "Peak Quality", ALIGNMENT_QUALITY = "Alignment Quality",
                                ISOTOPE_QUALITY = "Isotope Pattern Quality", MS2_QUALITY = "Fragmentation Pattern Quality",
                                ADDUCT_QUALITY = "Adduct Assignment Quality";

    public static final String[] DEFAULT_CATEGORIES = {
        PEAK_QUALITY, ALIGNMENT_QUALITY, ISOTOPE_QUALITY, MS2_QUALITY
    };

    private LinkedHashMap<String, Category> categories;

    @Id
    @Getter
    @Setter
    private long alignedFeatureId;

    private DataQuality overallQuality;

    public static QualityReport withDefaultCategories() {
        return withDefaultCategories(true);
    }
    public static QualityReport withDefaultCategories(boolean includeAlignment) {
        QualityReport r = new QualityReport();
        for (String d : DEFAULT_CATEGORIES)
            if (includeAlignment || !Objects.equals(ALIGNMENT_QUALITY, d))
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
        if (categories.containsKey(category.categoryName)) {
            categories.get(category.categoryName).merge(category);
        } else categories.put(category.categoryName, category);
    }

    public LinkedHashMap<String, Category> getCategories() {
        return categories;
    }

    public DataQuality getOverallQuality() {
        return overallQuality;
    }

    public void setOverallQuality(DataQuality overallQuality) {
        this.overallQuality = overallQuality;
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class Category {

        private String categoryName;
        private DataQuality overallQuality;
        private List<Item> items;

        public Category() {
            this("");
        }

        public Category(String categoryName) {
            this(categoryName, DataQuality.LOWEST, new ArrayList<>());
        }

        public Category(String categoryName, DataQuality overallQuality, List<Item> items) {
            this.categoryName = categoryName;
            this.overallQuality = overallQuality;
            this.items = items;
        }

        public void merge(Category same) {
            this.items.addAll(same.items);
        }

        public String getCategoryName() {
            return categoryName;
        }

        public DataQuality getOverallQuality() {
            return overallQuality;
        }

        public List<Item> getItems() {
            return items;
        }

        public void setOverallQuality(DataQuality overallQuality) {
            this.overallQuality = overallQuality;
        }
    }

    public static enum Weight {
        MINOR, MAJOR, CRITICAL;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Setter
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
