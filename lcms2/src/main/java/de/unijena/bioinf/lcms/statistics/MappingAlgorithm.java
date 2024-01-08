package de.unijena.bioinf.lcms.statistics;
import java.util.Arrays;

public class MappingAlgorithm {

    public static double[] findMapping(int[] timeLeft, int[] timeRight) {
        int m = timeLeft.length;
        int n = timeRight.length;
        double[] mapping = new double[m];

        int leftPointer = 0;
        int rightPointer = 0;

        while (leftPointer < m) {
            if (rightPointer < n && timeRight[rightPointer] == timeLeft[leftPointer]) {
                // Exact match found
                mapping[leftPointer] = rightPointer;
                leftPointer++;
            } else if (rightPointer < n && timeRight[rightPointer] < timeLeft[leftPointer]) {
                // Move rightPointer to the next element in timeRight
                rightPointer++;
            } else {
                // Perform linear interpolation
                int floorIndex = Math.max(0, rightPointer - 1);
                int ceilIndex = Math.min(n - 1, rightPointer);

                double alpha = (double) (timeLeft[leftPointer] - timeRight[floorIndex]) / (timeRight[ceilIndex] - timeRight[floorIndex]);

                mapping[leftPointer] = floorIndex + alpha;
                leftPointer++;
            }
        }

        return mapping;
    }

    public static void main(String[] args) {
        // Example usage
        int[] timeLeft = {1, 3, 5, 7, 9};
        int[] timeRight = {1, 2,3,4, 8};

        double[] mapping = findMapping(timeLeft, timeRight);

        System.out.println("Mapping: " + Arrays.toString(mapping));
    }
}
