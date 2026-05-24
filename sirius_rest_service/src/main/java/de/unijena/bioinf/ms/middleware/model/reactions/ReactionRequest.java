package de.unijena.bioinf.ms.middleware.model.reactions;

import reactionTool.sirius.model.ReactionSequence;
import java.util.List;

public class ReactionRequest {
    private ReactionSequence sequence;
    private List<String> initialSmiles;
    private String databaseName;
    private String productDatabaseName;

    public ReactionSequence getSequence() {
        return sequence;
    }

    public void setSequence(ReactionSequence sequence) {
        this.sequence = sequence;
    }

    public List<String> getInitialSmiles() {
        return initialSmiles;
    }

    public void setInitialSmiles(List<String> initialSmiles) {
        this.initialSmiles = initialSmiles;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getProductDatabaseName() {
        return productDatabaseName;
    }

    public void setProductDatabaseName(String productDatabaseName) {
        this.productDatabaseName = productDatabaseName;
    }
}
