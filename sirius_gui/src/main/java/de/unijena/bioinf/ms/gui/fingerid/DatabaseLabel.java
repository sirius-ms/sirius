/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.chemdb.custom.CustomDataSources;

import java.awt.*;

public class DatabaseLabel implements Comparable<DatabaseLabel> {

    protected String displayName;
    protected String sourceName;
    protected String[] values;
    protected final Rectangle rect;

    public DatabaseLabel(String name, String[] values) {
        this(name, null, values);
    }

    public DatabaseLabel(String name, String[] values, Rectangle rect) {
        this(name, null, values, rect);
    }

    public DatabaseLabel(String sourceName, String displayName, String[] values) {
        this(sourceName, displayName, values, new Rectangle(0, 0, 0, 0));
    }

    public DatabaseLabel(String sourceName, String displayName, String[] values, Rectangle rect) {
        this.sourceName = sourceName;
        this.displayName = displayName;
        this.values = values;
        this.rect = rect;
    }

    public String name() {
        if (displayName != null)
            return displayName;
        return sourceName;
    }

    @Override
    public int compareTo(DatabaseLabel o) {
        return sourceName.compareTo(o.sourceName);
    }

    public boolean contains(Point point) {
        return rect.contains(point);
    }

    public String getToolTipOrNull() {
        return CustomDataSources.getSourceFromNameOpt(sourceName)
                .filter(CustomDataSources.Source::noCustomSource)
                .map(s -> ((CustomDataSources.EnumSource) s).source())
                .map(ds -> ds.publication).map(pub -> {
                    String citation = pub.citationText();
                    String doi = pub.doi();
                    if (citation == null) return null;
                    if (doi == null) return citation;
                    return citation + "\ndoi: " + doi;
                }).orElse(null);
    }

    public boolean hasLinks() {
        return CustomDataSources.getSourceFromNameOpt(sourceName).map(s ->
                values != null && values.length > 0 && s.URI() != null).orElse(false);
    }
}
