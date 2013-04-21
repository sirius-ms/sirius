package de.unijena.bioinf.ChemistryBase.ms.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.primitives.DoubleList;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

public class Spectrums {
	
	public final static double DELTA = 1e-8;
	
	public static <P extends Peak, S extends Spectrum<P>>
	SimpleSpectrum neutralMassSpectrum(final S spectrum, final Ionization ionization) {
		return map(spectrum, new Transformation<P,Peak>() {
			@Override
			public Peak transform(P input) {
				return new Peak(ionization.subtractFromMass(input.getMass()), input.getIntensity());
			}
		});
	}

    public static <P extends Peak, S extends Spectrum<P>>
    SimpleSpectrum mergeSpectra(final S... spectra) {
        final SimpleMutableSpectrum ms = new SimpleMutableSpectrum();
        for (S s : spectra) {
            for (Peak p : s) {
                ms.addPeak(p); // TODO: improve performance by concatenating arrays
            }
        }
        return new SimpleSpectrum(ms);
    }

    public static <P extends Peak, S extends MutableSpectrum<P>, P2 extends Peak, S2 extends Spectrum<P2>>
    void addOffset(S s, double mzOffset, double intensityOffset) {
        for (int i=0; i < s.size(); ++i) {
            s.setMzAt(i, s.getMzAt(i)+mzOffset);
            s.setIntensityAt(i, s.getIntensityAt(i)+intensityOffset);
        }
    }

    public static <P extends Peak, S extends MutableSpectrum<P>, P2 extends Peak, S2 extends Spectrum<P2>>
    void scale(S s, double mzScale, double intensityScale) {
        for (int i=0; i < s.size(); ++i) {
            s.setMzAt(i, s.getMzAt(i)*mzScale);
            s.setIntensityAt(i, s.getIntensityAt(i)*intensityScale);
        }
    }

	public static <P extends Peak, S extends Spectrum<P>, P2 extends Peak, S2 extends Spectrum<P2>>
	boolean haveEqualPeaks(S a, S2 b) {
		if (a == b) return true;
		final int n = a.size();
		if (n != b.size()) return false;
		for (int i=0; i < n; ++i) {
			if (Math.abs(a.getMzAt(i) - b.getMzAt(i)) > DELTA || Math.abs(a.getIntensityAt(i) - b.getIntensityAt(i)) > DELTA) {
				return false;
			}
		}
		return true;
	}
	
	public static <P extends Peak, S extends MutableSpectrum<P>>
	S subtractAdductsFromSpectrum(S spectrum, Ionization ionization) {
		final int n = spectrum.size();
		for (int i=0; i < n; ++i) {
			spectrum.setMzAt(i, ionization.subtractFromMass(spectrum.getMzAt(i)));
		}
		return spectrum;
	}
	
	public static <P1 extends Peak, S extends Spectrum<P1>>
	SimpleSpectrum map(S spectrum, Transformation<P1,Peak> t) {
		final int n = spectrum.size();
		final double[] mzs = new double[n];
		final double[] intensities = new double[n];
		for (int i=0; i < n; ++i) {
			final Peak p = t.transform(spectrum.getPeakAt(i));
			mzs[i] = p.getMass();
			intensities[i] = p.getIntensity();
		}
		return new SimpleSpectrum(mzs, intensities);
	}
	
	public static <P extends Peak, S extends MutableSpectrum<P>> 
	S transform(S spectrum, Transformation<P,P> t) {
		final int n = spectrum.size();
		for (int i=0; i < n; ++i) {
			spectrum.setPeakAt(i, t.transform(spectrum.getPeakAt(i)));
		}
		return spectrum;
	}
	
	public static interface Transformation<P1 extends Peak, P2 extends Peak> {
		public P2 transform(P1 input);
	}
	
	public static SimpleSpectrum from(Collection<Peak> peaks) {
		final double[] mzs = new double[peaks.size()];
		final double[] intensities = new double[peaks.size()];
		int k=0;
		for (Peak p : peaks) {
			mzs[k] = p.getMass();
			intensities[k++] = p.getIntensity();
		}
		return new SimpleSpectrum(mzs, intensities);
	}
	
	public static SimpleSpectrum from(List<Number> mzsL, List<Number> intensitiesL) {
		if (mzsL.size() != intensitiesL.size())
			throw new IllegalArgumentException("size of masses and intensities differ");
		final double[] mzs = new double[mzsL.size()];
		final double[] intensities = new double[intensitiesL.size()];
		for (int i=0; i < mzsL.size(); ++i) {
			mzs[i] = mzsL.get(i).doubleValue();
			intensities[i] = intensitiesL.get(i).doubleValue();
		}
		return new SimpleSpectrum(mzs, intensities);
	}
	
	public static SimpleSpectrum from(DoubleList mzsL, DoubleList intensitiesL) {
		if (mzsL.size() != intensitiesL.size())
			throw new IllegalArgumentException("size of masses and intensities differ");
		return new SimpleSpectrum(mzsL.toArray(), intensitiesL.toArray());
	}
	
	public static <P extends Peak, S extends Spectrum<P>> List<P> extractPeakList(S spectrum) {
		final int n = spectrum.size();
		final ArrayList<P> peaks = new ArrayList<P>(n);
		for (int i=0; i < n; ++i) {
			peaks.add(spectrum.getPeakAt(i));
		}
		return peaks;
	}

	public static <P extends Peak, S extends Spectrum<P>> double getMinimalIntensity(S spectrum) {
		final int n = spectrum.size();
		double min = Double.MAX_VALUE;
		for (int i=0; i < n; ++i) {
			min = Math.min(min, spectrum.getIntensityAt(i));
		}
		return min;
	}
	public static <P extends Peak, S extends Spectrum<P>> double getMaximalIntensity(S spectrum) {
		final int n = spectrum.size();
		double max = 0d;
		for (int i=0; i < n; ++i) {
			max = Math.max(max, spectrum.getIntensityAt(i));
		}
		return max;
	}

    public static <P extends Peak, S extends Spectrum<P>> int getIndexOfPeakWithMinimalIntensity(S spectrum) {
        final int n = spectrum.size();
        double min = Double.POSITIVE_INFINITY;
        int minIndex = 0;
        for (int i=0; i < n; ++i) {
            if (spectrum.getIntensityAt(i) < min) {
                minIndex = i;
                min = spectrum.getIntensityAt(i);
            }
        }
        return minIndex;
    }

    public static <P extends Peak, S extends Spectrum<P>> int getIndexOfPeakWithMaximalIntensity(S spectrum) {
        final int n = spectrum.size();
        double max = Double.NEGATIVE_INFINITY;
        int maxIndex = 0;
        for (int i=0; i < n; ++i) {
            if (spectrum.getIntensityAt(i) > max) {
                maxIndex = i;
                max = spectrum.getIntensityAt(i);
            }
        }
        return maxIndex;
    }

	public static <P extends Peak, S extends Spectrum<P>> int getIndexOfPeakWithMinimalMass(S spectrum) {
		if (spectrum instanceof OrderedSpectrum) return 0;
		final int n = spectrum.size();
		double min = Double.POSITIVE_INFINITY;
		int minIndex = 0;
		for (int i=0; i < n; ++i) {
			if (spectrum.getMzAt(i) < min) {
				minIndex = i;
				min = spectrum.getMzAt(i);
			}
		}
		return minIndex;
	}
    public static <P extends Peak, S extends Spectrum<P>> int getIndexOfPeakWithMaximalMass(S spectrum) {
        if (spectrum instanceof OrderedSpectrum) return spectrum.size();
        final int n = spectrum.size();
        double max = Double.NEGATIVE_INFINITY;
        int maxIndex = 0;
        for (int i=0; i < n; ++i) {
            if (spectrum.getMzAt(i) > max) {
                maxIndex = i;
                max = spectrum.getMzAt(i);
            }
        }
        return maxIndex;
    }
	
	public static <P extends Peak, S extends Spectrum<P>> List<Peak> copyPeakList(S spectrum) {
		final int n = spectrum.size();
		final ArrayList<Peak> peaks = new ArrayList<Peak>(n);
		for (int i=0; i < n; ++i) {
			peaks.add(new Peak(spectrum.getMzAt(i), spectrum.getIntensityAt(i)));
		}
		return peaks;
	}
	
	public static <P extends Peak, S extends Spectrum<P>> double[] copyIntensities(S spectrum, double[] buffer, int offset) {
		final int n = spectrum.size();
        if (spectrum instanceof BasicSpectrum) {
            System.arraycopy(((BasicSpectrum) spectrum).intensities, 0, buffer, offset, n);
        } else {
            for (int i=0; i < n; ++i) {
                buffer[i + offset] = spectrum.getIntensityAt(i);
            }
        }
		return buffer;
	}
	
	public static <P extends Peak, S extends Spectrum<P>> double[] copyIntensities(S spectrum, double[] buffer) {
		return copyIntensities(spectrum, buffer, 0);
	}
	
	public static <P extends Peak, S extends Spectrum<P>> double[] copyIntensities(S spectrum) {
		return copyIntensities(spectrum, new double[spectrum.size()], 0);
	}
	
	public static <P extends Peak, S extends Spectrum<P>> double[] copyMasses(S spectrum, double[] buffer, int offset) {
        final int n = spectrum.size();
        if (spectrum instanceof BasicSpectrum) {
            System.arraycopy(((BasicSpectrum) spectrum).masses, 0, buffer, offset, n);
        } else {
            for (int i=0; i < n; ++i) {
                buffer[i + offset] = spectrum.getMzAt(i);
            }
        }
		return buffer;
	}
	
	public static <P extends Peak, S extends Spectrum<P>> double[] copyMasses(S spectrum, double[] buffer) {
		return copyMasses(spectrum, buffer, 0);
	}
	
	public static <P extends Peak, S extends Spectrum<P>> double[] copyMasses(S spectrum) {
		return copyMasses(spectrum, new double[spectrum.size()], 0);
	}
	
	
	public static <P extends Peak, S extends MutableSpectrum<P>> void normalize(S spectrum, Normalization norm) {
		switch (norm.getMode()) {
		case MAX: normalizeToMax(spectrum, norm.getBase()); return;
		case SUM: normalizeToSum(spectrum, norm.getBase()); return;
		}
	}

    public static <P extends Peak, S extends Spectrum<P>> SimpleSpectrum getNormalizedSpectrum(S spectrum, Normalization norm) {
        final SimpleMutableSpectrum s = new SimpleMutableSpectrum(spectrum);
        normalize(s, norm);
        return new SimpleSpectrum(s);
    }
	
	public static <P extends Peak, S extends MutableSpectrum<P>> void normalizeToMax(S spectrum, double norm) {
		final int n = spectrum.size();
		double maxIntensity = 0d;
		for (int i=0; i < n; ++i) {
			final double intensity = spectrum.getIntensityAt(i);
			if (maxIntensity < intensity) {
				maxIntensity = intensity;
			}
		}
		final double scale = norm / maxIntensity;
		for (int i=0; i < n; ++i) {
			spectrum.setIntensityAt(i, spectrum.getIntensityAt(i) * scale);
		}
	}
	
	public static <P extends Peak, S extends MutableSpectrum<P>> void normalizeToSum(S spectrum, double norm) {
		final int n = spectrum.size();
		double sumIntensity = 0d;
		for (int i=0; i < n; ++i) {
			sumIntensity += spectrum.getIntensityAt(i);
		}
		final double scale = norm / sumIntensity;
		for (int i=0; i < n; ++i) {
			spectrum.setIntensityAt(i, spectrum.getIntensityAt(i) * scale);
		}
	}
	
	
	/**
	 * search for the given peak using {@link Object#equals(Object)}. If the spectrum implements
	 * OrderedSpectrum, this search is done using binary search. Otherwise, a linear search is
	 * used.
	 * @param spectrum
	 * @param peak
	 * @return negative number if peak is not contained in the spectrum, otherwise index of the peak in
	 * 			the spectrum.
	 */
	public static <S extends Spectrum<P>, P extends Peak> int indexOfPeak(S spectrum, P peak) {
		final int pos = (spectrum instanceof OrderedSpectrum) 	? binarySearch(spectrum, peak.getMass())
																: linearSearch(spectrum, peak.getMass());
		if (pos < 0) return -1;
		return (peak.equals(spectrum.getPeakAt(pos))) ? pos : -1;
	}
	
	/**
	 * search for a peak with the lowest distance to the given mz value which respects the given
	 * mass deviation. If the spectrum implements
	 * OrderedSpectrum, this search is done using binary search. Otherwise, a linear search is
	 * used.
	 * @param spectrum
	 * @param mz
	 * @param d allowed deviation (relative and absolute) from the mz value
	 * @return negative number if peak is not contained in the spectrum, otherwise index of the peak in
	 * 			the spectrum.
	 */
	public static <S extends Spectrum<P>, P extends Peak> int search(S spectrum, double mz, Deviation d) {
		return (spectrum instanceof OrderedSpectrum) 	? binarySearch(spectrum, mz, d)
														: linearSearch(spectrum, mz, d);
	}
	
	private static <S extends Spectrum<P>, P extends Peak> int linearSearch(S spectrum, double mz) {
		double minDiff = Double.POSITIVE_INFINITY;
		int bestPos = -1;
		for (int i=0; i < spectrum.size(); ++i) {
			final double diff = Math.abs(spectrum.getMzAt(i) - mz);
			if (diff < minDiff) {
				minDiff = diff;
				bestPos = i;
			}
		}
		return bestPos;
	}
	
	
	private static <S extends Spectrum<P>, P extends Peak> int linearSearch(S spectrum, double mz, Deviation d) {
		final int bestPos = linearSearch(spectrum, mz);
		if (bestPos >= 0 && d.inErrorWindow(mz, spectrum.getMzAt(bestPos))) return bestPos;
		else return -1;
	}
	
	/**
	 * Binary Search algorithm to find the given the mz value with the lowest distance to the
	 * given mz value which respects the given mass deviation.
	 * @param spectrum
	 * @param mz
	 * @param d allowed deviation (relative and absolute) from the mz value
	 * @return index of the search key, if it is contained in the array within the specified range; 
	 * 	otherwise, (-(insertion point) - 1). The insertion point is defined as the point at which the 
	 * 	key would be inserted into the array: the index of the first element in the range greater 
	 * 	than the key, or toIndex if all elements in the range are less than the specified key. 
	 * 	Note that this guarantees that the return value will be >= 0 if and only if the key is found.
	 */
	public static <S extends Spectrum<P>, P extends Peak> int binarySearch(S spectrum, double mz, Deviation d) {
		int pos = binarySearch(spectrum, mz);
		if (pos >= 0) return pos;
		final int realPos = -pos;
		final double dev1 = realPos < 0 ? Double.POSITIVE_INFINITY : Math.abs(mz - spectrum.getMzAt(realPos));
		final double dev2 = realPos+1 >= spectrum.size() ? 	Double.POSITIVE_INFINITY :
															Math.abs(mz - spectrum.getMzAt(realPos+1));
		if (dev1 < dev2 && d.inErrorWindow(mz, dev1)) return realPos;
		if (dev2 <= dev1 && d.inErrorWindow(mz, dev2)) return realPos+1;
		return -pos;
	}
	
	/**
	 * Search for an exact mz value.
	 * @see Spectrums#binarySearch(Spectrum, double, Deviation)
	 */
	public static <S extends Spectrum<P>, P extends Peak> int binarySearch(S spectrum, double mz) {
		if (spectrum.size() > 0) {
			int low = 0;
			int high = spectrum.size()-1;
	        while (low <= high) {
	            int mid = (low + high) >>> 1;
	            int c = Double.compare(spectrum.getMzAt(mid), mz);
	            if (c < 0)
	                low = mid + 1;
	            else if (c > 0)
	                high = mid - 1;
	            else
	                return mid; // key found
	        }
	        return -(low + 1);
		}
		return -1;
	}
	
	/**
	 * Use quicksort to sort a spectrum by its masses in ascending order
	 * @param spectrum
	 */
	public static <T extends Peak, S extends MutableSpectrum<T>>
	void sortSpectrumByMass(S spectrum) {
		__sortSpectrum__(spectrum, new PeakComparator<T, S>() {
			@Override
			public int compare(S left, S right, int i, int j) {
				return Double.compare(left.getMzAt(i), right.getMzAt(j));
			}
		});
	}
	
	/**
	 * Use quicksort to sort a spectrum by its masses in descending order
	 * @param spectrum
	 */
	public static <T extends Peak, S extends MutableSpectrum<T>>
	void sortSpectrumByDescendingMass(S spectrum) {
		__sortSpectrum__(spectrum, new PeakComparator<T, S>() {
			@Override
			public int compare(S left, S right, int i, int j) {
				return Double.compare(right.getMzAt(j), left.getMzAt(i));
			}
		});
	}
	
	/**
	 * Use quicksort to sort a spectrum by its intensities in ascending order
	 * @param spectrum
	 */
	public static <T extends Peak, S extends MutableSpectrum<T>>
	void sortSpectrumByIntensity(S spectrum) {
		__sortSpectrum__(spectrum, new PeakComparator<T, S>() {
			@Override
			public int compare(S left, S right, int i, int j) {
				return Double.compare(left.getIntensityAt(i), right.getIntensityAt(j));
			}
		});
	}
	
	/**
	 * Use quicksort to sort a spectrum by its intensities in descending order
	 * @param spectrum
	 */
	public static <T extends Peak, S extends MutableSpectrum<T>>
	void sortSpectrumByDescendingIntensity(S spectrum) {
		__sortSpectrum__(spectrum, new PeakComparator<T, S>() {
			@Override
			public int compare(S left, S right, int i, int j) {
				return Double.compare(right.getIntensityAt(j), left.getIntensityAt(i));
			}
		});
	}
	
	/* *******************************************************************************************
	 * 
	 * 								Private static methods
	 * 
	 * ******************************************************************************************* */
	
	private static interface PeakComparator<P extends Peak, S extends Spectrum<P>> {
		int compare(S left, S right, int i, int j);
	}
	
	private static <T extends Peak, S extends MutableSpectrum<T>>
	void __sortSpectrum__(S spectrum, PeakComparator<T,S> comp) {
		final int n = spectrum.size();
		// Insertion sort on smallest arrays
        if (n < 7) {
            for (int i = 0; i < n; i++) {
                for (int j=i; j>0 && comp.compare(spectrum,spectrum, j, j-1) < 0; j--) {
                	__swap__(spectrum, j, j-1);
                }
            }
            return;
        }
        // quicksort on larger arrays
		if (n > 0) {
			__quickSort__(spectrum, comp, 0, n-1);
		}
	}
	
	private static <T extends Peak> void __swap__(MutableSpectrum<T> spectrum, int index1, int index2) {
		final double mz = spectrum.getMzAt(index1);
		final double in = spectrum.getIntensityAt(index1);
		spectrum.setMzAt(index1, spectrum.getMzAt(index2));
		spectrum.setIntensityAt(index1, spectrum.getIntensityAt(index2));
		spectrum.setMzAt(index2, mz);
		spectrum.setIntensityAt(index2, in);
	}
	
	/**
	 * http://en.wikipedia.org/wiki/Quicksort#In-place_version
	 * 
	 * @param low
	 * @param high
	 */
	private static <T extends Peak, S extends MutableSpectrum<T>>
	void __quickSort__(S s, PeakComparator<T, S> comp, int low, int high) {
		if (low < high) {
			int pivot = low+(high-low)/2;
			pivot = __partition__(s,comp, low, high, pivot);
			__quickSort__(s, comp, low, pivot-1);
			__quickSort__(s, comp, pivot+1, high);
		}
	}
	
	/**
	 * http://en.wikipedia.org/wiki/Quicksort#In-place_version
	 * 
	 * @param low
	 * @param high
	 * @param pivot
	 * @return
	 */
	private static <T extends Peak, S extends MutableSpectrum<T>>
    int __partition__(S s, PeakComparator<T, S> comp, int low, int high, int pivot) {
        __swap__(s, high, pivot);
        int store = low;
        for (int i = low; i < high; i++) {
        	if (comp.compare(s,s, i,high) < 0) {
        		__swap__(s, i,store);
        		store++;
        	}
        }
        __swap__(s, store,high);
        return store;
    }
	
}
