package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.reduction;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: B. Seelbinder
 * UID:  ga25wul
 * Date: 04.03.14
 * Time: 01:03
 * *
 */

public class TTypeDependentFunctions<T> {

    /**
     * - swap the entries from a[ia] with b[ib] in a safe way
     * @param a
     * @param ia
     * @param ib
     * @return
     */
    public static <T> T[] swap( T[] a, int ia, int ib ) {

        T buf = a[ib]; // remember this entry, because it gets overwritten first
        a[ib] = a[ia];
        a[ia] = buf;

        return a;
    }

	public static Integer[] merge( Integer[] a, Integer[] b ) {

		Integer[] res = new Integer[a.length + b.length];

		int i = 0, j = 0, k = 0;
		while (i < a.length && j < b.length)
		{
			if (a[i] < b[j])
			{
				res[k] = a[i];
				i++;
			}
			else
			{
				res[k] = b[j];
				j++;
			}
			k++;
		}

		while (i < a.length)
		{
			res[k] = a[i];
			i++;
			k++;
		}

		while (j < b.length)
		{
			res[k] = b[j];
			j++;
			k++;
		}

		return res;
	}

	/**
	 * - this function checks, if there are valid ( nonempty ) intersections between the list of l1 and l2
	 * - both lists MUST be SORTED, otherwise this will not work properly
	 * - O( |l1| + |l2| ) runtime!
	 * @param l1
	 * @param l2
	 * @return
	 */
	public boolean has_nonempty_intersection( List<Integer> l1, List<Integer> l2 ) {

		if( l1 == null || l2 == null )
			throw new NullPointerException( " Params are null. l1+ " + l1 + ", l2: " + l2 );

		// if one of them is empty, their cannot be an intersection, obviously
		if( l1.size() == 0 || l2.size() == 0 )
			return false;

		ListIterator<Integer> lit1 = l1.listIterator( 0 );
		ListIterator<Integer> lit2 = l2.listIterator( 0 );

		Integer o1 = lit1.next();
		Integer o2 = lit2.next();

		try {

		while( lit1.hasNext() || lit2.hasNext() ) {
			if( o1 < o2 )
				o1 = lit1.next();
			else if( o2 < o1 )
				o2 = lit2.next();
			else
				// they must be equal
				return true;
		}
		} catch( NoSuchElementException e ) {
			// this happens, when 1 list is at an end, but it should take the next one
			// at this point, every element from the other list is bigger ( since they are sorted )
			// => no equality
			return false;
		}

		return o2 == o1; // this can happen, if there are only a few entries
	}


	/**
	 * - this function imitates the 'unique' iterator function of relative++
	 * - basically, it deletes any element except the first of any particular group of equal elements
	 * - e.g. al = { 1,2,2,2,3,2,2,1 } => {1,2,3,2,1,?,?,?,?}
	 * @param al : arraylist that is being changed
	 * @param first : first index that is being accessed
	 * @param last : last index that is being affected; even so, it can be different in the return value
	 * @return : index to the last position, that is valid
	 */
	public int unique( ArrayList<T> al, int first, int last ) {

		if( first < 0 || last < 0 || first >= al.size() )
			throw new InputMismatchException( " wrong indices: first: " + first + " ,last: " + last + " ,size: " + al.size() );

		if( last >= al.size() )
			last = al.size()-1;

		if( al.size() <= 1 )
			return al.size()-1;

		int dist = 0;
		int i=first;
		while(  ( i<=last ) && (i+1+dist <= last ) ) {

			// the first condition must be 1 index higher, cause we would get out of bounds
			while ( (i+1+dist < al.size() ) && ( al.get( i ).equals( al.get( i+1+dist ) ) ) ) {
				dist++;
			}

			i++;

			if( ( dist > 0 ) && ( i+dist < al.size() ) ) {
				// shift entries
				al.set( i, al.get( i+dist ) );
			}
		}

		// make sure to decrease i, if the last two are equal, cause we couldn't swap anything!
		if( ( i-1 >= 0 ) && ( al.get( i ).equals( al.get( i-1 ) ) ) )
			i--;

		return i;
	}

	public void test() {
		ArrayList<Integer> a = new ArrayList<Integer>(  );
		a.add( 2 );
		a.add( 1 );
		a.add( 1 );
		a.add( 1 );

		/*
		a.add( 2 );
		a.add( 2 );
		a.add( 3 );
		a.add( 3 );
		a.add( 2 );
		a.add( 2 );
		a.add( 2 );
		*/

		String out = "";
		for( Integer I : a )
			out = out + ", " + I;

		System.out.println(out);

		int i = new TTypeDependentFunctions<Integer>().unique( a,0, a.size() );

		out = "";
		for ( Integer I : a )
			out = out + ", " + I;

		System.out.println(out + " with last index: " + i);
	}
}
