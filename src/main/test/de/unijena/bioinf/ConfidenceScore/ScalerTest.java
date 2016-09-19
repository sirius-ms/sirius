//package de.unijena.bioinf.ConfidenceScore;
//
//import org.junit.Test;
//
//import static org.junit.Assert.*;
//
//public class ScalerTest {
//
//
//    @Test
//    public void testScale() throws Exception {
//        double[][] testMatrix = new double[][]{
//                new double[]{1,2,3},
//                new double[]{4,5,6},
//                new double[]{7,8,9}
//        };
//        double[][] expectedResult = new double[][]{
//                new double[]{-1.22474487, -1.22474487, -1.22474487},
//                new double[]{0, 0, 0},
//                new double[]{1.22474487, 1.22474487, 1.22474487}
//        };
//
//        Scaler scaler = new Scaler.StandardScaler(testMatrix);
//        double[][] result = scaler.scale(testMatrix);
//
//        assertEquals(expectedResult.length, result.length);
//        assertEquals(expectedResult[0].length, result[0].length);
//
//        for (int i = 0; i < result.length; i++) {
//            for (int j = 0; j < result[0].length; j++) {
//                assertEquals(expectedResult[i][j], result[i][j], 1e-8);
//            }
//        }
//    }
//
//}