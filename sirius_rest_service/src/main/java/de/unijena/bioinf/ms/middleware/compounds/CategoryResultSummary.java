package de.unijena.bioinf.ms.middleware.compounds;

public class CategoryResultSummary {
    protected String categoryName = null;
    protected Double categoryProbability = null;


    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Double getCategoryProbability() {
        return categoryProbability;
    }

    public void setCategoryProbability(Double categoryProbability) {
        this.categoryProbability = categoryProbability;
    }
}
