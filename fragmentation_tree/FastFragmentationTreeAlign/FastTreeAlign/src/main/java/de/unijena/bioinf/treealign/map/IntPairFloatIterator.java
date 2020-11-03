
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *  
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker, 
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

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
