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
package de.unijena.bioinf.treeviewer;


import de.unijena.bioinf.babelms.dot.Graph;
import de.unijena.bioinf.babelms.dot.Vertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

public class Profile {

    private final String name;
    private final List<Heat> heats;
    private Heat activeHeat;
    private final List<Property> properties;

    public Profile(String name) {
        this.name = name;
        this.heats = new ArrayList<Heat>();
        this.properties = new ArrayList<Property>();
        activeHeat = null;
    }

    public void apply(Graph input) {
        final ArrayList<Property> regs = new ArrayList<Property>();
        for (Property p : properties) if (p.getRegexp() != null) regs.add(p);
        final ArrayList<Property> lineProps = new ArrayList<Property>();
        final HashMap<Vertex, Double> heatValues = new HashMap<Vertex, Double>();
        for (Property p : properties) if (p.getLineNumber() != 0) lineProps.add(p);
        final Vertex root = input.getRoot();
        for (Vertex v : input.getVertices()) {
            for (Property lp : lineProps) {
                final int n = lp.getLineNumber()-1;
                v.getInvisible().set(n + lp.getOffset(), n + lp.getOffset() + lp.getLength(), !lp.isEnabled());
            }
            final String label = v.getProperties().get("label");
            if (label != null) {
                for (Property reg : regs) {
                    int n=0;
                    for (String line : label.split("\\\\n")) {
                        if (reg.getRegexp().matcher(line).find()) {
                            if (!reg.isEnabled()) {
                                v.getInvisible().set(n + reg.getOffset(), n + reg.getOffset() + reg.getLength(), true);
                            }
                        }
                        ++n;
                    }
                }
                if (activeHeat != null && activeHeat.getScript() == null && activeHeat.getPattern()!=null && v != root) {
                    for (String line : label.split("\\\\n")) {
                        final Matcher m = activeHeat.getPattern().matcher(line);
                        if (m.find()) {
                            heatValues.put(v, Double.parseDouble(m.group(1)));
                        }
                    }
                }
                if (activeHeat == null) {
                    v.getProperties().remove("style");
                    v.getProperties().remove("fillcolor");
                }
            }
        }
        if (activeHeat.getScript() != null) {
            activeHeat.getScript().apply(input, this, activeHeat);
        } else if (activeHeat != null && heatValues.size() > 1) {
            activeHeat.apply(input, heatValues);
        }
    }

    public Heat getActiveHeat() {
        return activeHeat;
    }

    public void setActiveHeat(Heat activeHeat) {
        this.activeHeat = activeHeat;
    }

    public String getName() {
        return name;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public List<Heat> getHeats() {
        return heats;
    }
}
