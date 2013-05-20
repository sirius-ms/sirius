package de.unijena.bioinf.treeviewer.dot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    final Pattern entrypoint = Pattern.compile("\\s*(strict\\s+)?(di)?graph[^{]*\\{", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    final Pattern VERTEXP = Pattern.compile("\\s*([a-zA-Z0-9_.]+)\\s*\\[([^\\]]+)]\\s*;?}?"); // TODO: don't allow xml and escaped ], but who cares
    final Pattern EDGEP = Pattern.compile("\\s*([a-zA-Z0-9_.]+)\\s*->\\s*([a-zA-Z0-9_.]+)\\s*\\[([^\\]]+)]\\s*;?}?");

    public Graph parse(Reader input) throws IOException{
        final BufferedReader bf = new BufferedReader(input);
        final Graph graph = new Graph();
        final StringBuilder buffer = new StringBuilder();
        while (bf.ready()) {
            final String line = bf.readLine();
            if (line == null) break;
            buffer.append(line).append("\n");
            final Matcher m = entrypoint.matcher(buffer);
            if (m.find()) {
                buffer.delete(0, m.end());
                break;
            }
        }
        while (bf.ready()) {
            final String line = bf.readLine();
            if (line == null) break;
            buffer.append(line).append("\n");
            final Matcher m = VERTEXP.matcher(buffer);
            final Matcher m2 = EDGEP.matcher(buffer);
            if (m2.find()) {
                final String u = m2.group(1);
                final String v = m2.group(2);
                final HashMap<String, String> properties = parseProperties(m2.group(3));
                graph.getEdges().add(new Edge(u, v, properties));
                buffer.delete(0, buffer.length());
            } else if (m.find()) {
                final String nodeName = m.group(1);
                final HashMap<String, String> properties = parseProperties(m.group(2));
                graph.getVertices().add(new Vertex(nodeName, properties));
                buffer.delete(0, buffer.length());
            }
        }
        return graph;
    }

    final static Pattern TOKENIZE = Pattern.compile("=|\"|\\|,|[^\"\\,=]+", Pattern.MULTILINE);
    private final int VAL=1, ESCAPE=2, QUOTED=4;

    private HashMap<String, String> parseProperties(String group) {
        final Matcher m = TOKENIZE.matcher(group);
        final HashMap<String, String> properties = new HashMap<String, String>();
        String key = null;
        String value = null;
        int mode = 0;
        final StringBuilder buffer = new StringBuilder();
        while (m.find()) {
            switch (m.group().charAt(0)) {
                case '=':
                    if ((mode & QUOTED) == QUOTED) {
                        buffer.append(',');
                    } else {
                        mode |= VAL;
                        key = buffer.toString().trim();
                        buffer.delete(0, buffer.length());
                    }
                    break;
                case ',':
                    if ((mode & QUOTED) == QUOTED) {
                        buffer.append(',');
                    } else if ((mode & VAL) == VAL){
                        mode &= ~VAL;
                        value = buffer.toString().trim();
                        properties.put(key, value);
                        buffer.delete(0, buffer.length());
                    }
                    break;
                case '\\':
                    if ((mode & ESCAPE) == ESCAPE) {
                        buffer.append("\\");
                        mode &= ~ESCAPE;
                    } else mode |= ESCAPE;
                    break;
                case '"':
                    if ((mode & ESCAPE) == ESCAPE) {
                        buffer.append('"');
                        mode &= ~ESCAPE;
                    } else if ((mode & QUOTED) == QUOTED) {
                        mode &= ~QUOTED;
                    } else mode |= QUOTED;
                    break;
                default:
                   buffer.append(m.group());
            }
        }
        if ((mode & VAL) == VAL) {
            value = buffer.toString().trim();
            properties.put(key, value);
        }
        return properties;
    }

}
