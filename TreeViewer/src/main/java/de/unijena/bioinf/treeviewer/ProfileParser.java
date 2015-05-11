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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderSAX2Factory;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ProfileParser {

    private static Logger logger = Logger.getLogger(ProfileParser.class.getSimpleName());

    private static final File defaultPath = new File("/home/" + System.getenv("USER") + "/.appstorage/treeviewer");

    public ProfileParser() {

    }

    public List<Profile> parseProfiles() throws JDOMException, IOException {
        final List<Profile> profiles = new ArrayList<Profile>();
        if (defaultPath.exists()) {
            for (File f : defaultPath.listFiles()) {
                if (f.getName().endsWith(".xml")) {
                    profiles.addAll(parseProfiles(f));
                }
            }
        }
        return profiles;
    }

    public List<Profile> parseProfiles(File path) throws IOException, JDOMException{
        final SAXBuilder saxBuilder = new SAXBuilder(new XMLReaderSAX2Factory(false));
        final Document document = saxBuilder.build(path);
        final Element root = document.getRootElement();
        final ArrayList<Profile> profiles = new ArrayList<Profile>();
        for (Element profile : root.getChildren("profile")) {
            final String name = profile.getChildText("name");
            final ArrayList<Heat> heatList = new ArrayList<Heat>();
            if (name == null) throw new RuntimeException("Unnamed Profile in '" + path + "'");
            if (profile.getChild("heats") != null) {
                final Element heats = profile.getChild("heats");
                for (Element heatElem : heats.getChildren("heat")) {
                    final String heatName = heatElem.getChildText("name");
                    final String regexp = heatElem.getChildText("regexp");
                    final String method = heatElem.getChildText("method");
                    final String extract = heatElem.getChildText("extract");
                    final String apply = heatElem.getChildText("apply");
                    Script script;
                    try {
                        script = (extract != null || apply != null) ? new Script(extract, apply) : null;
                    } catch (ScriptException e) {
                        logger.severe(e.getMessage());
                        script = null;
                    }
                    if (heatName == null || (regexp == null && script == null) || method == null)
                        throw new RuntimeException("Heat element needs name, regexp (or script) and method. Error in profile '" +
                                name + "' in heat '" + heatName + "' in '" + path + "'");
                    final Heat.Method methodh = Heat.Method.valueOf(method.toUpperCase());
                    if (methodh == null)
                        throw new RuntimeException("Unknown method '" + method + "'. Allowed are {'normalized'}. Error in profile '" +
                                name + "' in '" + path + "'");
                    heatList.add(new Heat(heatName, regexp != null ? Pattern.compile(regexp) : null, methodh, script));
                }
            }
            final Profile prof = new Profile(name);
            prof.getHeats().addAll(heatList);
            if (heatList.size() > 0) prof.setActiveHeat(heatList.get(0));
            final Element properties = profile.getChild("properties");
            if (properties != null) {
                for (Element child : properties.getChildren("property")) {
                    final String propertyName = child.getChildText("name");
                    final String pattern = child.getChildText("regexp");
                    final String line = child.getChildText("line");
                    final String offset = child.getChildText("offset");
                    final String length = child.getChildText("length");
                    if (propertyName==null) throw new RuntimeException("Unnamed Property in profile '"+ name  +"' in '" + path + "'");
                    final Pattern pat = pattern==null ? null : Pattern.compile(pattern);
                    final int lineNo = line == null ? 0 : Integer.parseInt(line);
                    final int o = offset == null ? 0 : Integer.parseInt(offset);
                    final int l = length == null ? 1 : Integer.parseInt(length);
                    prof.getProperties().add(new Property(propertyName, lineNo, o, l, pat));
                }
            }
            profiles.add(prof);
        }
        return profiles;
    }

}
