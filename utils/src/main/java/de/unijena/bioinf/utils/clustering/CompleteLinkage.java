package de.unijena.bioinf.utils.clustering;

public class CompleteLinkage implements DistanceMeasureStrategy {

    @Override
    public Distance calcNewDistance(Distance distance1, Distance distance2, int clusterSize1, int clusterSize2) {
        return new Distance(Math.max(distance1.getDistance(), distance2.getDistance()));
    }
}
