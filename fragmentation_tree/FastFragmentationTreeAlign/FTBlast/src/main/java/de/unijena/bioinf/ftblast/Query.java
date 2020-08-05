
package de.unijena.bioinf.ftblast;

public final class Query implements Comparable<Query> {
    public final int row, col;
    public final double score;

    public Query(int row, int col, double score) {
        this.row = row;
        this.col = col;
        this.score = score;
    }

    @Override
    public int compareTo(Query o) {
        return Double.compare(score, o.score);
    }
}
