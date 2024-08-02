package de.unijena.bioinf.utils.clustering;

import org.junit.Test;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class HierarchicalClusteringTest {

    @Test
    public void cluster() {
        String[] elements = new String[]{"A", "B", "C", "D", "E", "F", "G"};
        double[][] distances = new double[][]{
                new double[]{0, 1, 4, 2, 5, 6, 7},
                new double[]{1, 0, 5, 3, 8, 9, 9},
                new double[]{4, 5, 0, 5, 3, 2, 2},
                new double[]{2, 3, 5, 0, 4, 5, 3},
                new double[]{5, 8, 3, 4, 0, 6, 7},
                new double[]{6, 9, 2, 5, 6, 0, 2},
                new double[]{7, 9, 2, 3, 7, 2, 0}
        };

        double threshold = 3.0;
        List<List<String>> clusters = clusterAndSort(elements, distances, threshold);

        assertEquals(3, clusters.size());
        assertArrayEquals(new String[]{"A","B","D"}, clusters.get(0).toArray(new String[0]));
        assertArrayEquals(new String[]{"C","F","G"}, clusters.get(1).toArray(new String[0]));
        assertArrayEquals(new String[]{"E"}, clusters.get(2).toArray(new String[0]));

        threshold = 2.0;
        clusters = clusterAndSort(elements, distances, threshold);

        assertEquals(4, clusters.size());
        assertArrayEquals(new String[]{"A","B"}, clusters.get(0).toArray(new String[0]));
        assertArrayEquals(new String[]{"C","F","G"}, clusters.get(1).toArray(new String[0]));
        assertArrayEquals(new String[]{"D"}, clusters.get(2).toArray(new String[0]));
        assertArrayEquals(new String[]{"E"}, clusters.get(3).toArray(new String[0]));


    }

    private List<List<String>> clusterAndSort(String[] elements, double[][] distances, double threshold){
        HierarchicalClustering<String> clustering = new HierarchicalClustering<>(new CompleteLinkage());
        clustering.cluster(elements, distances, threshold);

        List<List<String>> clusters = clustering.getClusters();

        for (List<String> strings : clusters) {
            Collections.sort(strings);
        }

        Collections.sort(clusters, new AlphabeticComparator());
        return clusters;
    }

    private class AlphabeticComparator implements Comparator<List<String>> {

        @Override
        public int compare(List<String> o1, List<String> o2) {
            for (int i = 0; i < Math.min(o1.size(), o2.size()); i++) {
                int c = o1.get(i).compareTo(o2.get(i));
                if (c!=0) return c;
            }
            if (o1.size()<o2.size()) return -1;
            else if (o2.size()>o1.size()) return 1;
            return 0;
        }
    }
}