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

import de.unijena.bioinf.babelms.dot.DotParser;
import de.unijena.bioinf.babelms.dot.Graph;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

public class FTCanvas extends JSVGCanvas {
    private final static Logger logger = Logger.getLogger(FTCanvas.class.getSimpleName());
    private final static String[] PATHS = new String[]{"/usr/bin/dot", "/usr/local/bin/dot"};

    private File dotPath;
    private DotSource treeFile;
    private SVGDocument svg;
    private Profile activeProfile;
    private Graph graph;

    public FTCanvas() {
        setSize(640, 480);
        setVisible(true);
    }

    public void setActiveProfile(Profile f) {
        this.activeProfile = f;
    }

    @Override
    public void dispose() {
        treeFile = null;
        svg = null;
        super.dispose();
    }

    public void setFile(final DotSource file) throws IOException{
        treeFile = file;
        svg = getSvgFromDot(treeFile);
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    setSVGDocument(svg);
                    setVisible(true);
                }
            });
        } catch (InterruptedException e) {
            logger.severe(e.getMessage());
        } catch (InvocationTargetException e) {
            logger.severe(e.getMessage());
        }
    }

    public synchronized void refreshFilters() {

        try {
            svg = getSvgFromDot(graph.clone());
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    setSVGDocument(svg);
                    setVisible(true);
                }
            });
        } catch (InterruptedException e) {
            logger.severe(e.getMessage());
        } catch (InvocationTargetException e) {
            logger.severe(e.getMessage());
        }
    }

    private File getDotPath() {
        if (dotPath != null) return dotPath;
        final String os = System.getProperty("os.name").toLowerCase();
        final boolean isWindoof = os.contains("win");
        // check PATH variable
        final String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(isWindoof ? ";" : ":")) {
                final File exec = new File(dir, isWindoof  ? "dot.exe" : "dot");
                if (exec.exists()) {
                    dotPath = exec;
                    return exec;
                }
            }
        }
        if (!isWindoof) {
            for (String s : PATHS) {
                final File f = new File(s);
                if (f.exists()) {
                    dotPath = f;
                    return f;
                }
            }
        }
        throw new RuntimeException("Can't find Graphviz. Please add dot command line tool to PATH variable.");
    }

    private SVGDocument getSvgFromDot(DotSource treeFile) throws IOException {
        if (activeProfile != null) {
            final Reader fr = treeFile.getContent();
            final Graph g = DotParser.parseGraph(fr);//new Parser().parse(fr);
            fr.close();
            this.graph = g;
            return getSvgFromDot(g);
        } else {
            final Reader fr = treeFile.getContent();
            this.graph = DotParser.parseGraph(fr);//new Parser().parse(fr);
            fr.close();
        }
        final ProcessBuilder builder = new ProcessBuilder(getDotPath().getAbsolutePath(), "-T", "svg");
        final Process proc = builder.start();
        final Writer writer = new OutputStreamWriter(proc.getOutputStream());
        graph.write(writer);
        writer.close();
        final String parser = XMLResourceDescriptor.getXMLParserClassName();
        final SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        return (SVGDocument)factory.createDocument("file://", new BufferedInputStream(proc.getInputStream()));
    }

    private SVGDocument getSvgFromDot(Graph graph) throws IOException {
        logger.info("render graph");
        if (activeProfile != null) {
            activeProfile.apply(graph);
        }

        final ProcessBuilder builder = new ProcessBuilder(getDotPath().getAbsolutePath(), "-T", "svg");
        final Process proc = builder.start();
        final Writer writer = new OutputStreamWriter(proc.getOutputStream());
        graph.write(writer);
        writer.close();
        final String parser = XMLResourceDescriptor.getXMLParserClassName();
        final SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        return (SVGDocument)factory.createDocument("file://", new BufferedInputStream(proc.getInputStream()));
    }

}
