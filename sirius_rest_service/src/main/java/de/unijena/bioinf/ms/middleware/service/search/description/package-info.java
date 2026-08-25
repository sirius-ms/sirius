/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

/**
 * Describes a search index to the people querying it.
 * <p>
 * Everything here answers "how do I explain this index to a client": the API types a field can be given, the
 * values it can take, the documentation it carries. None of it affects what the index does - remove this package
 * and every query still returns the same documents, clients just have to guess how to write them.
 * <p>
 * The dependency runs one way: this package reads
 * {@link de.unijena.bioinf.ms.middleware.service.search.mappers.IndexSchema} and the field names an index has
 * materialized, and nothing in the search engine may depend on anything here.
 */
package de.unijena.bioinf.ms.middleware.service.search.description;
