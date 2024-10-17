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

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.util.MathUtils;

/**
 * Chebychev polynomial function of first order.
 * 
 * @author Martin Engler
 *
 */
public class ChebychevPolynomialFunction implements UnivariateFunction {

	private static final long serialVersionUID = -3649121243067210554L;
	
	protected final static PolynomialFunction[] firstOrderCoefficients = new PolynomialFunction[10];
	
	static {{
		firstOrderCoefficients[0] = new PolynomialFunction(new double[]{1});
		firstOrderCoefficients[1] = new PolynomialFunction(new double[]{0, 1});
		firstOrderCoefficients[2] = new PolynomialFunction(new double[]{-1, 0, 2});
		firstOrderCoefficients[3] = new PolynomialFunction(new double[]{0, -3, 0, 4});
		firstOrderCoefficients[4] = new PolynomialFunction(new double[]{1, 0, -8, 0, 8});
		firstOrderCoefficients[5] = new PolynomialFunction(new double[]{0, 5, 0, -20, 0, 16});
		firstOrderCoefficients[6] = new PolynomialFunction(new double[]{-1, 0, 18, 0, -48, 0, 36});
		firstOrderCoefficients[7] = new PolynomialFunction(new double[]{0, -7, 0, 56, 0, -112, 0, 64});
		firstOrderCoefficients[8] = new PolynomialFunction(new double[]{1, 0, -32, 0, 160, 0, -256, 0, 128});
		firstOrderCoefficients[9] = new PolynomialFunction(new double[]{0, 9, 0, -120, 0, 432, 0, -576, 0, 256});
		
//		secondOrderCoefficients[0] = new PolynomialFunction(new double[]{1});
//		secondOrderCoefficients[1] = new PolynomialFunction(new double[]{0, 2});
//		secondOrderCoefficients[2] = new PolynomialFunction(new double[]{-1, 0, 4});
//		secondOrderCoefficients[3] = new PolynomialFunction(new double[]{0, -4, 0, 8});
//		secondOrderCoefficients[4] = new PolynomialFunction(new double[]{1, 0, -12, 0, 16});
//		secondOrderCoefficients[5] = new PolynomialFunction(new double[]{0, 6, 0, -32, 0, 32});
//		secondOrderCoefficients[6] = new PolynomialFunction(new double[]{-1, 0, 24, 0, -80, 0, 64});
//		secondOrderCoefficients[7] = new PolynomialFunction(new double[]{0, -8, 0, 80, 0, -192, 0, 128});
//		secondOrderCoefficients[8] = new PolynomialFunction(new double[]{1, 0, -40, 0, 240, 0, -448, 0, 256});
//		secondOrderCoefficients[9] = new PolynomialFunction(new double[]{0, 10, 0, -160, 0, 672, 0, -1024, 0, 512});
	}}
	
	protected double[] coefficients;
	protected double xmin;
	protected double scale;
	
	public ChebychevPolynomialFunction(double[] c, double xmin, double xmax)
			throws NullArgumentException, NoDataException {
		if (c.length > 10) {
			throw new IllegalArgumentException("too many coefficients");
		}
		this.coefficients = c;
		this.xmin = xmin;
		this.scale = 2d/(xmax-xmin);
	}
	
	public static int getN() {
		return 9;
	}
	
	protected static PolynomialFunction[] getChebychevCoefficients() {
		return firstOrderCoefficients;
	}
	
	protected static double evaluate(double[] coefficients, double argument) {
		MathUtils.checkNotNull(coefficients);
		int n = coefficients.length;
		if (n == 0) {
		    throw new NoDataException(LocalizedFormats.EMPTY_POLYNOMIALS_COEFFICIENTS_ARRAY);
		}
		PolynomialFunction[] chebychevCoefficients = getChebychevCoefficients();
		double result = 0;
		for (int i = 0; i < n; i++) {
		    result += coefficients[i]*chebychevCoefficients[i].value(argument);
		}
		return result-0.5*coefficients[0];
	}
	    
	@Override
	public double value(double x) {
		return evaluate(coefficients, (x-xmin)*scale-1);
	}

	@Override
	public String toString() {
		return toString(coefficients);
	}
	
	private static String toString(double[] coefficients) {
        StringBuilder s = new StringBuilder();
        if (coefficients[0] == 0.0) {
            if (coefficients.length == 1) {
                return "0";
            }
        } else {
            s.append(toString(coefficients[0]));
        }

        for (int i = 1; i < coefficients.length; ++i) {
            if (coefficients[i] != 0) {
                if (coefficients[i] < 0) {
                    s.append(" - ");
                } else {
                    s.append(" + ");
                }
                s.append(toString(Math.abs(coefficients[i]))+" ");
                s.append("("+getChebychevCoefficients()[i].toString()+")");
            }
        }
        if (coefficients[0] != 0d) {
	        if (coefficients[0] > 0) {
	            s.append(" - ");
	        } else {
	            s.append(" + ");
	        }
	        s.append(" 0.5 "+ toString(Math.abs(coefficients[0])));
        }
        return s.toString();
	}

    private static String toString(double coeff) {
        final String c = Double.toString(coeff);
        if (c.endsWith(".0")) {
            return c.substring(0, c.length() - 2);
        } else {
            return c;
        }
    }
		
}
