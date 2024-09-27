/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Martin Engler
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.recal;



/**
 * Linear Regression.<br>
 * Uses updating formulas for means and sums of squares defined in 
 * "Algorithms for Computing the Sample Variance: Analysis and
 * Recommendations", Chan, T.F., Golub, G.H., and LeVeque, R.J. 
 * 1983, American Statistician, vol. 37, pp. 242-247, referenced in
 * Weisberg, S. "Applied Linear Regression". 2nd Ed. 1985
 * 
 * @author Martin Engler
 *
 */
public class OrdinaryLeastSquares {
	
	private int n = 0;
	private double xbar = 0;
	private double ybar = 0;
	private double sumXX = 0;
	private double sumXY = 0;
	private double sumX = 0;
	private double sumY = 0;
	
	public void reset() {
		n = 0;
		xbar = 0;
		ybar = 0;
		sumXX = 0;
		sumXY = 0;
		sumX = 0;
		sumY = 0;
	}
	
	/**
     * Adds the observation (x,y) to the regression data set.
     *
     * @param x independent variable value
     * @param y dependent variable value
     */
	public void addPoint(double x, double y) {
        if (n == 0) {
            xbar = x;
            ybar = y;
        } else {
            double dx = x - xbar;
            double dy = y - ybar;
            sumXX += dx * dx * (double) n / (double) (n + 1.0);
            sumXY += dx * dy * (double) n / (double) (n + 1.0);
            xbar += dx / (double) (n + 1.0);
            ybar += dy / (double) (n + 1.0);
        }
        sumX += x;
        sumY += y;
        n++;
    }
	
	/**
	 * Returns the regression line a+bx with minimal distance to
	 * all previously added observations (x,y).
	 * 
	 * @return array [a,b]
	 */
	public double[] getSolution() {
		if (n == 0)
			throw new IllegalArgumentException("no points added");
		double[] solution = new double[2];
		solution[1] = sumXY/sumXX;
		solution[0] = (sumY - solution[1] * sumX) / ((double) n);
		return solution;
	}
	
	/**
	 * Returns the regression line a+bx with minimal distance to
	 * all observations (x,y).
	 * 
	 * @param x
	 * @param y
	 * @return array [a,b]
	 */
	public double[] solve(double[] x, double[] y) {
		if (x.length != y.length)
			throw new IllegalArgumentException("x != y");
		if (x.length < 2)
			throw new IllegalArgumentException("not enough points");
		reset();
		for (int i = 0; i < x.length; i++) {
			addPoint(x[i], y[i]);
		}
		return getSolution();		
	}

}
