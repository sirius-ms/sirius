/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
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

package de.unijena.bioinf.babelms;

import java.io.Closeable;
import java.util.Iterator;

/**
 * Behaves like an iterator, but can be closed.
 * The iterator is closed automatically, after all elements are iterated. As it is also auto-closeable this
 * might lead to multiple close calls. Therefore, implementing classes are not allowed to throw an exception
 * if close is called several times.
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable, AutoCloseable {

}
