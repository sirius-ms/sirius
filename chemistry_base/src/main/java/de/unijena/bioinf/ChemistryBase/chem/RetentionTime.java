package de.unijena.bioinf.ChemistryBase.chem;

public final class RetentionTime {

    private final double start, middle, end;

    public RetentionTime(double start, double end) {
        this(start, end, start + (end-start)/2d);
    }

    public RetentionTime(double start, double end, double maximum) {
        if (!Double.isNaN(start)) {
            if (start < end)
                throw new IllegalArgumentException("No proper interval given: [" + start + ", " + end + "]" );
            if (maximum < start || maximum > end) {
                throw new IllegalArgumentException("Given retention time middle is not in range: " + maximum + " is not in [" + start + ", " + end + "]" );
            }
        }
        this.start = start;
        this.end = end;
        this.middle = maximum;
    }

    public RetentionTime(double timeInSeconds) {
        this(Double.NaN, Double.NaN, timeInSeconds);
    }

    public RetentionTime merge(RetentionTime other) {
        if (isInterval() && other.isInterval())
            return new RetentionTime(Math.min(start, other.start), Math.max(end, other.end));
        else
            return new RetentionTime(Math.min(start, other.start), Math.max(end, other.end), (middle+other.middle)/2);
    }

    public boolean isInterval() {
        return !Double.isNaN(start);
    }

    public double getRetentionTimeInSeconds() {
        return middle;
    }

    public double getStartTime() {
        if (!isInterval()) throw new RuntimeException("No retention time range given");
        return start;
    }

    public double getEndTime() {
        if (!isInterval()) throw new RuntimeException("No retention time range given");
        return end;
    }

    public double getMiddleTime() {
        return middle;
    }

}
