package de.unijena.bioinf.fteval;

/**
 * Created by kaidu on 17.07.14.
 */
public enum AlignAlgorithm {

    TREE('t'), SPECTRAL('m'), TANIMOTO('c'), PATHCOUNTING('t'), SUBTREECOUNTING('t');

    private char dataSource; // m=ms, c=compounds, t=tree

    AlignAlgorithm(char dataSource) {
        this.dataSource = dataSource;
    }

    public char getDataSource() {
        return dataSource;
    }
}
