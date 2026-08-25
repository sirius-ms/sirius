/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.utils.search;

/**
 * How much of the query grammar the builder offers.
 * <p>
 * {@link #SIMPLE} fixes the shape of the query so the connector never has to be chosen: the top-level
 * terms are ANDed, the alternatives inside a group are ORed, and groups do not nest. That is the shape
 * nearly every filter has ("all of these, and one of those"), and it is the shape whose meaning is
 * unambiguous - a flat mix of AND and OR is not read left-to-right by lucene, so a query built without
 * groups would not mean what it looks like.
 * <p>
 * {@link #ADVANCED} offers both connectors for every term and lets groups nest, for queries the simple
 * shape cannot express. Existing chips are never rewritten by switching: the mode decides what the
 * builder OFFERS, not what a query may contain (a query pasted or built in advanced mode stays as it
 * is when the user switches back).
 */
public enum QueryMode {
    SIMPLE, ADVANCED
}
