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
/**
 * 
 */
package de.unijena.bioinf.recal;

import de.unijena.bioinf.ChemistryBase.math.MathUtils;
import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.optimization.fitting.PolynomialFitter;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * m/z recalibatrion.
 * Usage
 * <ol>
 * <li>Extract subset of points with {@code maxLinePairStabbing} or
 * {@code maxIntervalStabbing}</li>
 * <li>Compute linear, polynomial or chebychev recalibration function on the
 * subset with the {@code getRecalibration} methods</li>
 * <li>Apply the function to a spectrum with {@code recalibrate}</li>
 * </ol>
 * 
 * @author Martin Engler
 *
 */
public class MzRecalibration {
	
	private final static int SIGN_POS = 1;
	private final static int SIGN_NEG = -1;
	
	public final static double DEFAULT_POLY_COEFF_THRESHOLD = 1E-10;

	private static Comparator<Tuple> COMPARATOR = new Comparator<Tuple>() {
		@Override
		public int compare(Tuple tuple, Tuple tuple2) {
			return Double.compare(tuple.key, tuple2.key);
		}
	};

	private static class Tuple {
		double key;
		int value;

		public Tuple(double key, int value) {
			this.key = key;
			this.value = value;
		}
	}

	/**
	 * Finds the maximal number of points stabbed by a line in an interval epsilon.
	 *
	 * @param observed     observed spectrum
	 * @param reference    reference spectrum
	 * @param epsilon      maps observed mz to interval length
	 * @param <P>
	 * @param <Q>
	 * @return [0]: x points (observed spectrum), [1]: y points (reference spectrum)
	 */
	@Deprecated
	public static <P extends Peak, Q extends Peak> double[][] maxIntervalStabbing(Spectrum<P> observed,
			Spectrum<Q> reference, UnivariateFunction epsilon) {
		return maxIntervalStabbing(observed, reference, epsilon, Double.POSITIVE_INFINITY);
	}

	/**
	 * Finds the maximal number of points stabbed by a line in an interval epsilon.
	 *
	 * @param observed     observed spectrum
	 * @param reference    reference spectrum
	 * @param epsilon      maps observed mz to interval length
	 * @param threshold    for all points (x,y): |x-y| &lt; threshold
	 * @return [0]: x points (observed spectrum), [1]: y points (reference spectrum)
	 */
	public static <P extends Peak, Q extends Peak> double[][] maxIntervalStabbing(Spectrum<P> observed,
		Spectrum<Q> reference, UnivariateFunction epsilon, double threshold) {
		double[] e = new double[observed.size()];
		for (int i = 0; i < observed.size(); i++) {
			e[i] = epsilon.value(observed.getMzAt(i));
		}

		return maxIntervalStabbing(observed, reference, e, threshold);
	}

	/**
	 * Finds the maximal number of points stabbed by a line in an interval epsilon.
	 *
	 * @param observed     observed spectrum
	 * @param reference    reference spectrum
	 * @param epsilon      maps observed mz and intensity to interval length
	 * @param <P>
	 * @param <Q>
	 * @return [0]: x points (observed spectrum), [1]: y points (reference spectrum)
	 */
	@Deprecated
	public static <P extends Peak, Q extends Peak> double[][] maxIntervalStabbing(Spectrum<P> observed,
		Spectrum<Q> reference, BivariateFunction epsilon) {
		return maxIntervalStabbing(observed, reference, epsilon, Double.POSITIVE_INFINITY);
	}

	/**
	 * Finds the maximal number of points stabbed by a line in an interval epsilon.
	 *
	 * @param observed     observed spectrum
	 * @param reference    reference spectrum
	 * @param epsilon      maps observed mz and intensity to interval length
	 * @param threshold    for all points (x,y): |x-y| &lt; threshold
	 * @return [0]: x points (observed spectrum), [1]: y points (reference spectrum)
	 */
	public static <P extends Peak, Q extends Peak> double[][] maxIntervalStabbing(Spectrum<P> observed, Spectrum<Q> reference,
			BivariateFunction epsilon, double threshold) {
		double[] e = new double[observed.size()];
		for (int i = 0; i < observed.size(); i++) {
			e[i] = epsilon.value(observed.getMzAt(i), observed.getIntensityAt(i));
		}
		return maxIntervalStabbing(observed, reference, e, threshold);
	}

	/**
	 * Finds the maximal number of points stabbed by a line in an interval epsilon.
	 *
	 * @param observed     observed spectrum
	 * @param reference    reference spectrum
	 * @param epsilon      interval length for each observed mz
	 * @param <P>
	 * @param <Q>
	 * @return [0]: x points (observed spectrum), [1]: y points (reference spectrum)
	 */
	@Deprecated
	public static <P extends Peak, Q extends Peak> double[][] maxIntervalStabbing(Spectrum<P> observed, Spectrum<Q> reference,
			double[] epsilon) {
		return maxIntervalStabbing(observed, reference, epsilon, Double.POSITIVE_INFINITY);
	}

	/**
	 * Finds the maximal number of points stabbed by a line in an interval epsilon.
	 *
	 * @param observed     observed spectrum
	 * @param reference    reference spectrum
	 * @param epsilon      interval length for each observed mz
	 * @param threshold    for all points (x,y): |x-y| &lt; threshold
	 * @return [0]: x points (observed spectrum), [1]: y points (reference spectrum)
	 */
	public static <P extends Peak, Q extends Peak> double[][] maxIntervalStabbing(Spectrum<P> observed, Spectrum<Q> reference, double[] epsilon,
			double threshold) {
		if (epsilon.length != observed.size()) {
			throw new IllegalArgumentException("length of epsilon != size of observed spectrum");
		}
		// build all points (x,y) in observed x reference
		TDoubleList x = new TDoubleArrayList();
		TDoubleList y = new TDoubleArrayList();
		TDoubleList e = new TDoubleArrayList();
		for (int i = 0; i < observed.size(); i++) {
			for (int j = 0; j < reference.size(); j++) {
				if (Math.abs(observed.getMzAt(i)-reference.getMzAt(j)) < threshold) {
					x.add(observed.getMzAt(i));
					y.add(reference.getMzAt(j));
					e.add(epsilon[i]);
				}
			}
		}
		
		int maxScore = 0;
		double aStar = 0;
		double bStar = 0;
		// for 1 to n do
		for (int i = 0; i < x.size(); i++) {
			// empty Q
			PriorityQueue<Tuple> Q = new PriorityQueue<Tuple>(2*x.size(), COMPARATOR);
			// for 1 to n with j != i do
			for (int j = 0; j < x.size(); j++) {
				if (i == j) continue;
				double delta = x.get(i)-x.get(j);
				if (delta != 0) {
					int sign = delta < 0 ? SIGN_NEG : SIGN_POS;
					Q.add(new Tuple((y.get(i)-y.get(j))/delta, sign));
					Q.add(new Tuple((y.get(i)-y.get(j)+e.get(j))/delta,-sign));
				}
			}
			int score = 0;
			// for all (a, sign) in Q do
			while (!Q.isEmpty()) {
				Tuple min = Q.poll();
				score += min.value;
				if (score > maxScore) {
					maxScore = score;
					aStar = min.key;
					bStar = min.key * x.get(i)-y.get(i);
				}
			}
		}
		// extract subset of (xi,yi) with |aStar * xi - bStar - yi | <= epsilon
		TDoubleList subsetX = new TDoubleArrayList();
		TDoubleList subsetY = new TDoubleArrayList();
		for (int i = 0; i < x.size(); i++) {
			if (Math.abs(aStar * x.get(i) - bStar - y.get(i)) <= e.get(i)) {
				subsetX.add(x.get(i));
				subsetY.add(y.get(i));
			}
		}
		return new double[][]{subsetX.toArray(),subsetY.toArray()};
	}

	/**
	 * Finds the maximal number of points between two parallel lines with distance epsilon.
	 *
	 * @param observed     observed spectrum
	 * @param reference    reference spectrum
	 * @param epsilon      line distance
	 * @param <P>
	 * @param <Q>
	 * @return [0]: x points (observed spectrum), [1]: y points (reference spectrum)
	 */
	@Deprecated
	public static <P extends Peak, Q extends Peak> double[][] maxLinePairStabbing(Spectrum<P> observed, Spectrum<Q> reference,
	                                                                              double epsilon) {
		return maxLinePairStabbing(observed, reference, epsilon, Double.POSITIVE_INFINITY);
	}

	/**
	 * Finds the maximal number of points between two parallel lines with distance epsilon.
	 *
	 * @param observed     observed spectrum
	 * @param reference    reference spectrum
	 * @param epsilon      line distance
	 * @param threshold    for all points (x,y): |x-y| &lt; threshold
	 * @return [0]: x points (observed spectrum), [1]: y points (reference spectrum)
	 */
	public static <P extends Peak, Q extends Peak> double[][] maxLinePairStabbing(Spectrum<P> observed, Spectrum<Q> reference,
			double epsilon, double threshold) {
		double[] e = new double[observed.size()];
		Arrays.fill(e, epsilon);
		return maxIntervalStabbing(observed, reference, e, threshold);
	}

	/**
	 * Applies a function to the m/z values of a spectrum.
	 *
	 * @param <T>
	 * @param spectrum
	 * @param function
	 * @return
	 */
	public static <T extends Peak> MutableSpectrum<T> recalibrate(MutableSpectrum<T> spectrum, UnivariateFunction function) {
		for (int i = 0; i < spectrum.size(); i++) {
			spectrum.setMzAt(i, function.value(spectrum.getMzAt(i)));
		}
		return spectrum;
	}
	
	/**
	 * Linear regression by calculating the median slope and median y-intersect.
	 * 
	 * @param <P>
	 * @param <Q>
	 * @param x
	 * @param y
	 * @return
	 */
	public static <P extends Peak, Q extends Peak> PolynomialFunction getMedianLinearRecalibration(double[] x, double[] y) {
		checkArgs(x, y);
		TDoubleList slopes = new TDoubleArrayList();
		for (int i = 0; i < x.length-1; i++) {
			for (int j = i+1; j < x.length; j++) {
				slopes.add((y[j]-y[i])/(x[j]-x[i]));
			}
		}
		double slope = MathUtils.median(slopes.toArray());
		TDoubleList intersects = new TDoubleArrayList();
		for (int i = 0; i < y.length; i++) {
			intersects.add(y[i]-slope*x[i]);
		}
		double intersect = MathUtils.median(intersects.toArray());
		return new PolynomialFunction(new double[]{intersect, slope});
	}
	
	/**
	 * Linear regression by ordinary least squares.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static PolynomialFunction getLinearRecalibration(double[] x, double[] y) {
		checkArgs(x, y);
		double[] coefficients = new OrdinaryLeastSquares().solve(x,y);
		return new PolynomialFunction(coefficients);
	}
	
	/**
	 * Polynomial regression.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static PolynomialFunction getPolynomialRecalibration(double[] x, double[] y) {
		return getPolynomialRecalibration(x, y, DEFAULT_POLY_COEFF_THRESHOLD);
	}
	
	/**
	 * Polynomial regression.
	 * 
	 * @param x
	 * @param y
	 * @param threshold coefficients smaller than the threshold are discarded
	 * @return
	 */
	public static PolynomialFunction getPolynomialRecalibration(double[] x, double[] y, double threshold) {
		checkArgs(x, y);
		for (int degree = 1;; degree++) {
			PolynomialFitter fit = new PolynomialFitter(degree, new LevenbergMarquardtOptimizer());
			for (int i = 0; i < x.length; i++) {
				fit.addObservedPoint(x[i], y[i]);
			}
			double[] coefficients = fit.fit();
			if (Math.abs(coefficients[coefficients.length-1]) < threshold) {
				return new PolynomialFunction(coefficients);
			}
		}
	}
	
	public static PolynomialFunction getPolynomialRecalibrationWithDegree(double[] x, double[] y, int degree) {
		checkArgs(x, y);
		PolynomialFitter fit = new PolynomialFitter(degree, new LevenbergMarquardtOptimizer());
		for (int i = 0; i < x.length; i++) {
			fit.addObservedPoint(x[i], y[i]);
		}
		return new PolynomialFunction(fit.fit());
	}
	
	/**
	 * Spline interpolation.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private static PolynomialSplineFunction getPolynomialSplineRecalibration(double[] x, double[] y) {
		SplineInterpolator interpolator = new SplineInterpolator();
		return interpolator.interpolate(x, y);
	}
		
	/**
	 * Regression with Chebychev polynomial. See
	 * {@see <a href= http://mathworld.wolfram.com/ChebyshevApproximationFormula.html</a>}.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static ChebychevPolynomialFunction getFirstOrderChebyshevRecalibration(double[] x, double[] y) {
		checkArgs(x, y);
		double[] xR = new double[x.length];
		System.arraycopy(x, 0, xR, 0, x.length);
		
		// rescale x values of points to [-1,1]
		double xmin = xR[0];
		double xmax = xR[xR.length-1];
		double scale = 2d/(xmax-xmin);
		for (int i = 0; i < xR.length; i++) {
			xR[i] = (xR[i]-xmin)*scale-1;
		}
		
		// build spline
		PolynomialSplineFunction function = getPolynomialSplineRecalibration(xR, y);
		// approximate spline with chebychev function
		double N = ChebychevPolynomialFunction.getN()+1;
		double[] coefficients = new double[(int) N];
		double twoDivN = 2d/N;
		double piDivN = Math.PI/N;
		for (int j = 0; j < N; j++) {
			for (int k = 1; k <= N; k++) {
				double kTimesPiDivN = (k-0.5)*piDivN;
				coefficients[j] += Math.cos(j*kTimesPiDivN)*function.value(Math.cos(kTimesPiDivN));
			}
			coefficients[j] *= twoDivN;
		}
		return new ChebychevPolynomialFunction(coefficients, xmin, xmax);
	}
	
	/**
	 * Catches illegal arguments and throws IllegalArgumentException.
	 * 
	 * @param x
	 * @param y
	 */
	private static void checkArgs(double[] x, double[] y) {
		if (x.length != y.length)
			throw new IllegalArgumentException("x != y");
		if (x.length == 0)
			throw new IllegalArgumentException("no points");
	}
	
}
