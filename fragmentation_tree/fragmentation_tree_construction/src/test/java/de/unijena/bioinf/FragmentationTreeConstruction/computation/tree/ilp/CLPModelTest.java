package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CLPModelTest {
    @Test
    public void testCLPModel() {
        CLPModel model = new CLPModel(2, CLPModel.ObjectiveSense.MAXIMIZE);
        double[] objective = new double[] { 6, 7 };
        model.setObjective(objective);
        double infty = model.getInfinity();
        double[] col_lb = new double[] { 0.0, 0.0 };
        double[] col_ub = new double[] { infty, infty };
        model.setColBounds(col_lb, col_ub);
        // add rows
        double[] row1 = new double[] { 4.0, 5.0 };
        model.addSparseRow(row1, new int[] { 0, 1 }, -infty, 20.0);
        double[] row2 = new double[] { 10.0, 7.0 };
        model.addSparseRow(row2, new int[] { 0, 1 }, -infty, 35.0);
        double[] row3 = new double[] { 3.0, 4.0 };
        model.addSparseRow(row3, new int[] { 0, 1 }, 6.0, infty);
        // solve
        model.setColStart(new double[]{1d, 0d});
        assertEquals(CLPModel.ReturnStatus.OPTIMAL, model.solve());
        assertEquals(28d, model.getScore(), 0.001);
        double[] colSolution = model.getColSolution();
        // for (int i = 0; i < 2; ++i)
        // System.out.println("Column " + i + ": " + colSolution[i]);
        assertEquals(0d, colSolution[0], 0.001);
        assertEquals(4d, colSolution[1], 0.001);
        model.dispose();
    }
}
