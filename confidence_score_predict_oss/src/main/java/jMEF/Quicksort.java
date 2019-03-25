package jMEF;


/**
 * @author  Vincent Garcia
 * @author  Frank Nielsen
 * @version 1.0
 *
 * @section License
 * 
 * See file LICENSE.txt
 *
 * @section Description
 * 
 * The Quicksort class implements the quicksort algorithm.
 */
public class Quicksort {

	/**
	 * Sorts an array using quicksort algorithm.
	 * @param  data   array to be sorted
	 * @param  index  initial position (index) of the sorted elements
	 */
	public static void quicksort(double[] data, int[] index) {
		shuffle(data, index);                        // to guard against worst-case
		quicksort(data, index, 0, data.length - 1);
	}


	/**
	 * Sorts the left and the right parts of an array using recursive quicksort algorithm.
	 * @param  a     array to be sorted
	 * @param  idx   index
	 * @param  left  left index: sort between left to idx
	 * @param  right right index: sort between idx to right
	 */
	private static void quicksort(double[] a, int[] idx, int left, int right) {
		if (right <= left) return;
		int i = partition(a, idx, left, right);
		quicksort(a, idx, left, i-1);
		quicksort(a, idx, i+1, right);
	}

	/**
	 * Creates the partition.
	 * @param  a     array to be sorted
	 * @param  idx   array of indexes
	 * @param  left  left index: sort between left to i
	 * @param  right right index: sort between i to right
	 */
	private static int partition(double[] a, int[] idx, int left, int right) {
		int i = left - 1;
		int j = right;
		while (true) {
			while (a[++i]<a[right])      // find item on left to swap
				;                        // a[right] acts as sentinel
			while (a[right]<a[--j])      // find item on right to swap
				if (j == left) break;    // don't go out-of-bounds
			if (i >= j) break;           // check if pointers cross
			exch(a, idx, i, j);          // swap two elements into place
		}
		exch(a, idx, i, right);          // swap with partition element
		return i;
	}


	/**
	 * Switches the the values a[i] and a[j] and the indexes idx[i] and idx[j].
	 * @param  a     array to be sorted
	 * @param  idx   array of indexes
	 * @param  i     index
	 * @param  j     index
	 */
	private static void exch(double[] a, int[] idx, int i, int j) {
		double swap = a[i];
		a[i]        = a[j];
		a[j]        = swap;
		int swap2   = idx[i];
		idx[i]      = idx[j];
		idx[j]      = swap2;      
	}

	/**
	 * Shuffles the array a
	 * @param  a     array to be sorted
	 * @param  idx   array of indexes
	 */
	private static void shuffle(double[] a, int[] idx) {
		int N = a.length;
		for (int i = 0; i < N; i++) {
			int r = i + (int) (Math.random() * (N-i));   // between i and N-1
			exch(a, idx, i, r);
		}
	}

}
