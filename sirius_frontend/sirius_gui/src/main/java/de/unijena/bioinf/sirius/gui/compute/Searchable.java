package de.unijena.bioinf.sirius.gui.compute;

import java.util.Collection;


/// von http://www.algosome.com/articles/java-jcombobox-autocomplete.html

/**

 * Interface to search an underlying inventory of items and return a collection of found items. 

 * @author G. Cope

 *

 * @param <E> The type of items to be found.

 * @param <V> The type of items to be searched

 */

public interface Searchable<E, V>{

	

	/**

	 * Searches an underlying inventory of items consisting of type E

	 * @param value A searchable value of type V

	 * @return A Collection of items of type E.

	 */

	public Collection<E> search(V value);

	

}