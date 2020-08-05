
package de.unijena.bioinf.treealign.map;

import java.util.NoSuchElementException;

public interface IntFloatIterator {
  public void next();
  public int getKey();
  public float getValue();
  public boolean hasNext();

  public static IntFloatIterator Empty = new IntFloatIterator() {


      public void next() {
          throw new NoSuchElementException();
      }

      public int getKey() {
          throw new NoSuchElementException();
      }

      public float getValue() {
          throw new NoSuchElementException();
      }

      public boolean hasNext() {
          return false;
      }
  };

}
