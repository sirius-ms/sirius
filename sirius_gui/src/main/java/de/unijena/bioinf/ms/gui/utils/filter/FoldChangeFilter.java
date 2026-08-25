package de.unijena.bioinf.ms.gui.utils.filter;

import lombok.Getter;
import lombok.Setter;


@Getter
public class FoldChangeFilter {
    private final double minFoldChange;
    @Setter
    private double currentMinFoldChange;
    @Setter
    private boolean enabled;

    public FoldChangeFilter(double minFoldChange) {
        this(minFoldChange, false);
    }

    public FoldChangeFilter(double minFoldChange, boolean enabled) {
        this.minFoldChange = minFoldChange;
        this.currentMinFoldChange = minFoldChange;
        this.enabled = enabled;
    }

    public void reset() {
        currentMinFoldChange = minFoldChange;
        enabled = false;
    }
}
