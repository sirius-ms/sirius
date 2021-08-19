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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.rest.model.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Term {
    private String name;
    private URI link;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getLink() {
        return link;
    }

    public void setLink(URI link) {
        this.link = link;
    }

    @JsonIgnore
    public String getKey() {
        String[] k = link.toString().split("[/]");
        return k[k.length - 1];
    }

    @JsonIgnore
    public Date getDate() {
        String[] d = getKey().split("[-]");
        Calendar c = Calendar.getInstance();
        c.set(Integer.parseInt(d[d.length - 3]),
                Integer.parseInt(d[d.length - 2]) - 1,
                Integer.parseInt(d[d.length - 1]));
        return c.getTime();
    }


    public static Term of(@NotNull URI link) {
        return of(null, link);
    }

    public static Term of(@Nullable String name, @NotNull URI link) {
        Term it = new Term();
        it.setName(name);
        it.setLink(link);
        return it;
    }

    public static Term of(@NotNull String link) {
        return of(null, link);
    }

    public static Term of(@Nullable String name, @NotNull String link) {
        try {
            return of(name, new URI(link));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String toLinks(Term... terms){
        return toLinks(List.of(terms));
    }

    @Nullable
    public static String toLinks(@Nullable List<Term> terms){
        if (terms == null || terms.isEmpty())
            return null;
        StringBuilder builder = new StringBuilder()
                .append("<a href=").append(terms.get(0).getLink())
                .append(">").append(terms.get(0).getName()).append("</a>");
        for (int i = 1; i < terms.size(); i++) {
            builder.append(" and ")
                    .append("<a href=").append(terms.get(i).getLink())
                    .append(">").append(terms.get(i).getName()).append("</a>");
        }
        return builder.toString();
    }
}
