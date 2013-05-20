package de.unijena.bioinf.treeviewer;

import de.unijena.bioinf.treeviewer.dot.Graph;
import de.unijena.bioinf.treeviewer.dot.Parser;
import de.unijena.bioinf.treeviewer.dot.Parser2;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.svg.GVTTreeBuilderAdapter;
import org.apache.batik.swing.svg.GVTTreeBuilderEvent;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGDocument;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
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
        for (String path : PATHS) {
            final File f = new File(path);
            if (f.exists() && f.canExecute()) {
                this.dotPath = f;
                break;
            }
        }
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

    private SVGDocument getSvgFromDot(DotSource treeFile) throws IOException {
        if (activeProfile != null) {
            final Reader fr = treeFile.getContent();
            final Graph g = Parser2.parse(fr);//new Parser().parse(fr);
            fr.close();
            this.graph = g;
            return getSvgFromDot(g);
        }
        final ProcessBuilder builder = new ProcessBuilder(dotPath.getAbsolutePath(), "-T", "svg");
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

        final ProcessBuilder builder = new ProcessBuilder(dotPath.getAbsolutePath(), "-T", "svg");
        final Process proc = builder.start();
        final Writer writer = new OutputStreamWriter(proc.getOutputStream());
        graph.write(writer);
        writer.close();
        final String parser = XMLResourceDescriptor.getXMLParserClassName();
        final SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        return (SVGDocument)factory.createDocument("file://", new BufferedInputStream(proc.getInputStream()));
    }

}
