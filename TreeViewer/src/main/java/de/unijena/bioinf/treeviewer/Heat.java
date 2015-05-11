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

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Heat {
    private final String name;
    private final Pattern pattern;
    private final Method method;
    private final Script script;

    public Heat(String name, Pattern pattern, Method method, Script script) {
        this.name = name;
        this.pattern = pattern;
        this.method = method;
        this.script = script;
    }

    public Script getScript() {
        return script;
    }

    public String getName() {
        return name;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public Method getMethod() {
        return method;
    }

    public void apply(Graph input, HashMap<Vertex, Double> heatValues) {
        // add offset
        final Double min = Collections.min(heatValues.values());
        if (min < 0) {
            for (Map.Entry<Vertex, Double> entry : heatValues.entrySet()) {
                entry.setValue(entry.getValue() - min);
            }
        }
        if (method == Method.LOGNORM) {
            for (Map.Entry<Vertex, Double> entry : heatValues.entrySet()) {
                entry.setValue(Math.log(entry.getValue()+1));
            }
        }
        if (method == Method.NORMALIZE || method == Method.LOGNORM) {
            // normalize
            final Double max = Collections.max(heatValues.values());
            for (Map.Entry<Vertex, Double> entry : heatValues.entrySet()) {
                entry.setValue(entry.getValue() / max);
            }
        }
        // colorize
        for (Map.Entry<Vertex, Double> entry : heatValues.entrySet()) {
            final Color c = getGradient(entry.getValue());
            entry.getKey().getProperties().put("style", "filled");
            entry.getKey().getProperties().put("fillcolor", toHex(c));
        }
    }

    private static Color getGradient(double scale) {
        if (scale > 0.6) {
            return getGrad(Color.YELLOW, Color.RED, (1 - scale)/0.4 );
        } else {
            return getGrad(Color.BLUE, Color.YELLOW, (0.6 - scale)/0.6 );
        }
    }

    private static Color getGrad(Color init, Color end, double i) {
        return new Color((int)(end.getRed() + (init.getRed()-end.getRed())*i),
                (int)(end.getGreen() + (init.getGreen()-end.getGreen())*i),
                (int)(end.getBlue() + (init.getBlue()-end.getBlue())*i)
        );
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue(), 128);
    }

    public enum Method {
        NORMALIZE, LOGNORM;
    }

}
