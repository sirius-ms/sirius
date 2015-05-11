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

import javax.script.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public class Script {

    private static Logger logger = Logger.getLogger(Script.class.getSimpleName());

    private final String extractSource, applySource;
    private final CompiledScript extractScript, applyScript;

    public Script(String extractSource, String applySource) throws ScriptException {
        final Compilable compiler = (Compilable)new ScriptEngineManager().getEngineByName("javascript");
        this.extractSource = extractSource;
        this.applySource = applySource;
        this.extractScript = extractSource == null ? null : compiler.compile(extractSource);
        this.applyScript = applySource == null ? null : compiler.compile(applySource);
    }

    public void apply(Graph graph, Profile profile, Heat heat) {
        final ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        final Vertex root = graph.getRoot();
        engine.put("graph", graph);
        engine.put("root", root);
        engine.put("profile", profile);
        engine.put("heat", heat);
        final HashMap<String, HashMap<String, JSProperty>> properties = new HashMap<String, HashMap<String, JSProperty>>();
        final HashMap<Vertex, Object> heats = new HashMap<Vertex, Object>();
        if (extractScript != null) {
            for (Vertex v : graph.getVertices()) {
                engine.put("vertex", v);

                final HashMap<String, JSProperty> props = properties(graph, profile, v);
                engine.put("properties", props);
                if (heat.getPattern() != null) engine.put("$", extractHeatValue(heat, v));
                properties.put(v.getName(), props);
                try {
                    heats.put(v, extractScript.eval(engine.getContext()));
                } catch (ScriptException e) {
                    logger.severe(e.getMessage());
                } catch (RuntimeException e) {
                    logger.severe(e.getMessage());
                }
            }
        } else {
            for (Vertex v : graph.getVertices()) {
                if (v != root)
                    heats.put(v, Double.parseDouble(extractHeatValue(heat, v)));
            }
        }
        final HashMap<Vertex, Double> heatMap = new HashMap<Vertex, Double>();
        if (applyScript != null) {
            engine.put("graphProperties", properties);
            for (Vertex v : graph.getVertices()) {
                engine.put("$", v);
                engine.put("properties", properties.get(v.getName()));
                try {
                    final Double degree = (Double)applyScript.eval(engine.getContext());
                    if (degree != null) heatMap.put(v, degree);
                } catch (ScriptException e) {
                    logger.severe(e.getMessage());
                } catch (RuntimeException e) {
                    logger.severe(e.getMessage());
                }
            }
        } else {
            for (Map.Entry<Vertex, Object> entry : heats.entrySet()) {
                if (entry.getValue() == null) continue;
                heatMap.put(entry.getKey(), (Double)entry.getValue());
            }
        }
        if (heatMap.size() > 0)
        heat.apply(graph, heatMap);
    }

    private String extractHeatValue(Heat heat, Vertex v) {
        for (String label : v.getProperties().get("label").split("\\\\n")) {
            final Matcher m = heat.getPattern().matcher(label);
            if (m.find()) {
                return m.groupCount() > 0 ? m.group(1) : m.group();
            }
        }
        return null;
    }

    public static class JSProperty {
        private final String line;
        private final String[] groups;

        private JSProperty(String line, String[] groups) {
            this.line = line;
            this.groups = groups;
        }

        public String value() {
            if (groups != null && groups.length > 0) return groups[0];
            return line;
        }

        public String label() {
            return line;
        }
        public String get(int i) {
            return groups[i];
        }
        public int intValue() {
            return Integer.parseInt(value());
        }
        public double doubleValue() {
            return Double.parseDouble(value());
        }
        public int intValue(int i) {
            return Integer.parseInt(get(i));
        }
        public double doubleValue(int i) {
            return Double.parseDouble(get(i));
        }
    }

    public static HashMap<String, JSProperty> properties(Graph graph, Profile profile, Vertex vertex) {
        final HashMap<String, JSProperty> properties = new HashMap<String, JSProperty>();
        final String label = vertex.getProperties().get("label");
        if (label == null) return properties;
        final String[] labels = label.split("\\\\n");
        for (Property p : profile.getProperties()) {
            final String name = p.getName();
            if (p.getLineNumber() > 0) {
                properties.put(p.getName(), new JSProperty(labels[p.getLineNumber()-1+p.getOffset()], null));
            } else if (p.getRegexp() != null) {
                int j=0;
                for (String line : labels) {
                    final Matcher m = p.getRegexp().matcher(line);
                    if (m.find()) {
                        final String[] groups = new String[m.groupCount()];
                        for (int i=1; i <= m.groupCount(); ++i) groups[i-1] = m.group(i);
                        final JSProperty prop = new JSProperty(labels[p.getOffset()+j], groups);
                        properties.put(name, prop);
                        break;
                    }
                    ++j;
                }
            }
        }
        return properties;
    }
}
