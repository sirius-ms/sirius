import org.junit.Assert;
import org.junit.Test;
import matching.utils.HungarianAlgorithm;

import static org.junit.Assert.assertEquals;

public class HungarianAlgorithmTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIrregularCostMatrix(){
        double[][] costMatrix = new double[][]{{2.5,3,1.5,1},{2,5,9},{9,11,13,15}};
        HungarianAlgorithm alg = new HungarianAlgorithm(costMatrix);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInfiniteCosts(){
        double inf = 1 / (double) 0;
        double[][] costMatrix = new double[][]{{1.5,2.5,3.5},{inf, 9.0,2.0},{3.5,2.0,1.0}};
        HungarianAlgorithm alg = new HungarianAlgorithm(costMatrix);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNaNCosts(){
        double nan = (0 / (double) 0);
        double[][] costMatrix = new double[][]{{1.5,2.5,3.5},{nan, 9.0,2.0},{3.5,2.0,1.0}};
        HungarianAlgorithm alg = new HungarianAlgorithm(costMatrix);
    }

    @Test
    public void testNormalAssignment(){
        double[][] costMatrix = new double[][]{{8,7,9,9},{5,2,7,8},{6,1,4,9},{2,3,2,6}};
        HungarianAlgorithm alg = new HungarianAlgorithm(costMatrix);
        int[] actualOptAssignment = alg.execute();
        int[][] expectedOptAssignments = new int[][]{{3,0,1,2},{3,1,2,0}};

        boolean bool = false;

        for(int i = 0; i < expectedOptAssignments.length; i++){
            if(this.isEquals(actualOptAssignment, expectedOptAssignments[i])){
                bool = true;
                break;
            }
        }

        assertEquals(true, bool);
    }


    @Test
    public void testNormalAssignmentCost(){
        double[][] costMatrix = new double[][]{{8,7,9,9},{5,2,7,8},{6,1,4,9},{2,3,2,6}};
        HungarianAlgorithm alg = new HungarianAlgorithm(costMatrix);
        int[] actualOptAssignment = alg.execute();

        double cost = 0;
        for(int i = 0; i < actualOptAssignment.length; i++){
            cost = cost + costMatrix[i][actualOptAssignment[i]];
        }

        assertEquals(17, cost, 0);
    }

    @Test
    public void testMoreWorkersThanJobs(){
        double[][] costMatrix = new double[][]{{2,5,9},{3,4,6},{8,12,1},{0,8,3}};
        HungarianAlgorithm alg = new HungarianAlgorithm(costMatrix);
        int[] actualOptAssignment = alg.execute();
        int[] expectedOptAssignment = new int[]{-1, 1, 2, 0};

        Assert.assertArrayEquals(expectedOptAssignment, actualOptAssignment);
    }

    @Test
    public void testMoreJobsThanWorkers(){
        double[][] costMatrix = new double[][]{{2,3,8,0},{5,4,12,8},{9,6,1,3}};
        HungarianAlgorithm alg = new HungarianAlgorithm(costMatrix);
        int[] actualOptAssignment = alg.execute();
        int[] expectedOptAssignment = new int[]{3,1,2};

        Assert.assertArrayEquals(expectedOptAssignment, actualOptAssignment);
    }

    private boolean isEquals(int[] a, int[] b){
        if(a.length == b.length){
            boolean bool = true;

            for(int i = 0; i < a.length; i++){
                if(a[i] != b[i]){
                    bool = false;
                    break;
                }
            }

            return bool;
        }else{
            return false;
        }
    }

}
