/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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

package de.unijena.bioinf.ms.rest.model.info;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marcus Ludwig on 25.01.17.
 */
public class News {
    final static public String PROPERTY_KEY = "de.unijena.bioinf.sirius.news";

    public final String id;
    public final String date;
    public final String message;

    public News(String id, String date, String message) {
        this.id = id;
        this.date = date;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public String getDateString() {
        return date;
    }

    public String getMessage() {
        return message;
    }


    public static List<News> parseJsonNews(JsonNode array) {
        List<News> newsList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        for (JsonNode jsonNode : array) {
            final String id = jsonNode.get("id").asText();
            final String message = jsonNode.get("message").asText();
            final String begin = jsonNode.get("date").asText();
            newsList.add(new News(id, begin, message));
        }
        return newsList;
    }
}
