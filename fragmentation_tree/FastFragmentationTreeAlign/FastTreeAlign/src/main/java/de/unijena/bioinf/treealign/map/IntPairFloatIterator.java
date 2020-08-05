
package de.unijena.bioinf.treealign.map;

import java.util.NoSuchElementException;

public interface IntPairFloatIterator {

    public boolean hasNext();
    public int getLeft();
    public int getRight();
    public float getValue();
    public void next();

    public static IntPairFloatIterator Empty = new IntPairFloatIterator() {


      public void next() {
          throw new NoSuchElementException();
      }

      public boolean hasNext() {
          return false;
      }

        public int getLeft() {
            throw new NoSuchElementException();
        }

        public int getRight() {
            throw new NoSuchElementException();
        }

        public float getValue() {
            throw new NoSuchElementException();
        }
    };

}
