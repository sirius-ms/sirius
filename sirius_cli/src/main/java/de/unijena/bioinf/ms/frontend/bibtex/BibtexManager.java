/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.bibtex;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import org.jbibtex.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

/**
 * This class manages a BibTex database and allows to access the enties in different preformatted formats like
 * plaintext, html, bibtex and markdown.
 */
public class BibtexManager {
    public static Key KEY_KEYWORDS = new Key("keywords");

    @NotNull
    public final BibTeXDatabase db;
    @Nullable
    protected final LaTeXParser parser;
    @NotNull
    protected final LaTeXPrinter printer;

    public BibtexManager(BibTeXDatabase db) {
        this.db = db != null ? db : new BibTeXDatabase();
        this.printer = new LaTeXPrinter();
        LaTeXParser tmp = null;
        try {
            tmp = new LaTeXParser();
        } catch (ParseException e) {
            LoggerFactory.getLogger(getClass()).error("Error when initializing Latex Parser!", e);
        } finally {
            this.parser = tmp;
        }
    }

    private Optional<String> getField(BibTeXEntry entry, Key key) {
        return Optional.ofNullable(toText(entry.getField(key)));
    }

    private String toText(@Nullable Value bibValue) {
        if (bibValue == null)
            return null;
        if (parser == null) {
            LoggerFactory.getLogger(getClass()).warn("Cannot parse latex to Text. Cause:  Parser not initialized!");
            return "";
        }

        try {
            List<LaTeXObject> parsed = parser.parse(bibValue.toUserString());
            return printer.print(parsed);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private BibTeXEntry getEntryRaw(String bibtexKey) {
        if (db == null)
            return null;
        return db.resolveEntry(new Key(bibtexKey));
    }

    public Optional<BibTeXEntry> getEntry(String bibtexKey) {
        return Optional.ofNullable(getEntryRaw(bibtexKey));
    }

    public Optional<String> getEntryAsPlainText(String bibtexKey, boolean keywords) {
        return getEntry(bibtexKey).map(e -> {
            final StringBuilder text = new StringBuilder();
            getField(e, BibTeXEntry.KEY_AUTHOR).ifPresent(str -> text.append(str).append(System.lineSeparator()));
            getField(e, BibTeXEntry.KEY_TITLE).ifPresent(str -> text.append(str).append(System.lineSeparator()));
            getField(e, BibTeXEntry.KEY_JOURNAL).ifPresent(str -> text.append(str).append(", "));
            getField(e, BibTeXEntry.KEY_VOLUME).ifPresent(str -> text.append(str).append(", "));
            getField(e, BibTeXEntry.KEY_YEAR).ifPresent(str -> text.append(str).append(". "));
            getField(e, BibTeXEntry.KEY_DOI).ifPresent(doi -> text.append(" https://doi.org/").append(doi));
            if (keywords)
                getField(e, KEY_KEYWORDS).ifPresent(str -> text.append(System.lineSeparator()).append("(Cite if you are using: ").append(str.replaceAll("\\s*;\\s*", ", ")).append(")"));


            return text.toString();
        });
    }

    public Optional<String> getEntryAsHTML(String bibtexKey, boolean keywords, boolean doiLink) {
        return getEntry(bibtexKey).map(e -> {
            final StringBuilder text = new StringBuilder();
            getField(e, BibTeXEntry.KEY_AUTHOR).ifPresent(str -> text.append(str).append("<br>"));
            getField(e, BibTeXEntry.KEY_TITLE).ifPresent(str -> text.append("<b>").append(str).append("</b><br>"));
            text.append("<i>");
            getField(e, BibTeXEntry.KEY_JOURNAL).ifPresent(str -> text.append(str).append(", "));
            getField(e, BibTeXEntry.KEY_VOLUME).ifPresent(str -> text.append(str).append(", "));
            getField(e, BibTeXEntry.KEY_YEAR).ifPresent(str -> text.append(str).append("."));
            text.append("</i>");
            getField(e, BibTeXEntry.KEY_DOI).ifPresent(doi -> {
                if (doiLink)
                    text.append(" <a href=https://doi.org/").append(doi).append(">[doi]</a>");
                else
                    text.append(" doi:").append(doi);
            });
            if (keywords)
                getField(e, KEY_KEYWORDS).ifPresent(str -> text.append("<p style=\"margin-top:5px;\">(Cite if you are using: <b>").append(str.replaceAll("\\s*;\\s*", ", ")).append("</b>)</p>"));

            return text.toString();
        });
    }

    public Optional<String> getEntryAsBibTex(String bibtexKey) {
        return getEntry(bibtexKey).map(e -> {
            BibTeXDatabase db = new BibTeXDatabase();
            db.addObject(e);
            return db;
        }).map(this::getCitationsBibTex);
    }


    public void citeToSystemErr() {
        System.err.println(System.lineSeparator() + System.lineSeparator() + "Please cite the following publications when using our tool:" + System.lineSeparator());
        System.err.println(getCitationsText());
    }


    public String getCitationsHTML(final boolean doilinks) {
        StringBuilder buf = new StringBuilder();
        buf.append("<p>");
        buf.append("<h3>When using the SIRIUS Software please cite the following paper:</h3>");
        getEntryAsHTML("duehrkop19sirius4", false, doilinks).ifPresent(it -> buf.append("<p>").append(it).append("</p>"));
        buf.append("</p>");

        buf.append("<p>");
        buf.append("<h3>Depending on the tools you have used please also cite:</h3>");
        db.getEntries().keySet().stream().filter(k -> !k.toString().equalsIgnoreCase("duehrkop19sirius4")).map(db.getEntries()::get).sorted((e1, e2) -> {
            Key k = new Key("keywords");
            return e1.getField(k).toUserString().compareToIgnoreCase(e2.getField(k).toUserString());
        }).forEach(e -> getEntryAsHTML(e.getKey().toString(), true, doilinks).ifPresent(it -> buf.append("<p>").append(it).append("</p>")));
        buf.append("</p>");

        return buf.toString();
    }

    public String getCitationsText() {
        StringBuilder buf = new StringBuilder();
        buf.append("When using the SIRIUS Software please cite the following paper:").append(System.lineSeparator()).append(System.lineSeparator());
        getEntryAsPlainText("duehrkop19sirius4", false).ifPresent(it -> buf.append(it).append(System.lineSeparator()).append(System.lineSeparator()));

        buf.append(System.lineSeparator());
        buf.append("Depending on the tools you have used please also cite:").append(System.lineSeparator()).append(System.lineSeparator());
        db.getEntries().keySet().stream().filter(k -> !k.toString().equalsIgnoreCase("duehrkop19sirius4")).map(db.getEntries()::get).sorted((e1, e2) -> {
            Key k = new Key("keywords");
            return e1.getField(k).toUserString().compareToIgnoreCase(e2.getField(k).toUserString());
        }).forEach(e ->
                getEntryAsPlainText(e.getKey().toString(), true).ifPresent(it ->
                        buf.append(it).append(System.lineSeparator()).append(System.lineSeparator())));

        return buf.toString();
    }


    public String getCitationsBibTex() {
        return getCitationsBibTex(db);
    }

    public String getCitationsBibTex(BibTeXDatabase db) {
        StringWriter sw = new StringWriter();
        try {
            new BibTeXFormatter().format(db, sw);
            return sw.toString();
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Could not create BibTex Citations!", e);
            return "";
        }
    }


}
