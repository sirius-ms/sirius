package de.unijena.bioinf.babelms.json;

import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FTJsonWriter {

    public void writeTree(Writer writer, FTree tree) throws IOException {
        if (!(writer instanceof BufferedWriter)) writer=new BufferedWriter(writer);
        final JSONWriter jwriter = new JSONWriter(writer);
        try {
            writeGraph(tree, jwriter);
        } catch (JSONException e) {
            throw new IOException(e);
        } finally {
            writer.close();
        }
    }

    public void writeTreeToFile(File f, FTree tree) throws IOException {
        final BufferedWriter writer = Files.newBufferedWriter(f.toPath(), Charset.forName("UTF-8"));
        final JSONWriter jwriter = new JSONWriter(writer);
        try {
            writeGraph(tree, jwriter);
        } catch (JSONException e) {
            throw new IOException(e);
        } finally {
            writer.close();
        }
    }

    protected void writeGraph(FTree graph, JSONWriter writer) throws JSONException {
        writer.object();
        writer.key("molecularFormula");
        writer.value(graph.getRoot().getFormula().toString());
        writer.key("root");
        writer.value(graph.getRoot().getFormula().toString());
        writer.key("annotations");
        {
            writer.object();
            for (Map.Entry<Class<Object>, Object> entry : graph.getAnnotations().entrySet()) {
                if (!FTSpecials.writeSpecialAnnotation(writer, entry.getKey(), entry.getValue())) {
                    writer.key(entry.getKey().getName());
                    writer.value(new JSONObject(entry.getValue()));
                }
            }
            writer.endObject();
        }

        final List<FragmentAnnotation<Object>> fAnos = graph.getFragmentAnnotations();

        writer.key("fragments");
        writer.array();
        for (Fragment f : graph) {
            writeFragment(writer, f, fAnos);
        }
        writer.endArray();
        writer.key("losses");
        writer.array();

        writer.endArray();
        writer.endObject();
    }

    private void writeLoss(JSONWriter writer, Loss l, List<LossAnnotation<Object>> lAnos) throws JSONException {
        writer.object();

        writer.key("source");
        writer.value(l.getSource().getFormula().toString());

        writer.key("target");
        writer.value(l.getTarget().getFormula().toString());

        writer.key("molecularFormula");
        writer.value(l.getFormula().toString());


        final ArrayList<LossAnnotation<Object>> custom = new ArrayList<LossAnnotation<Object>>();
        for (LossAnnotation<Object> t : lAnos) {
            if (!FTSpecials.writeSpecialAnnotation(writer, t.getAnnotationType(), t.get(l))) {
                custom.add(t);
            }
        }
        if (custom.size() > 0) {
            writer.key("annotations");
            writer.object();
            for (LossAnnotation<Object> t : custom) {
                writer.key(t.getAnnotationType().getName());
                writer.value(new JSONObject(t.get(l)));
            }
            writer.endObject();
        }
        writer.endObject();
    }

    private void writeFragment(JSONWriter writer, Fragment f, List<FragmentAnnotation<Object>> fAnos) throws JSONException {
        writer.object();

        writer.key("id");
        writer.value(f.getVertexId());

        writer.key("color");
        writer.value(f.getColor());

        writer.key("molecularFormula");
        writer.value(f.getFormula().toString());
        final ArrayList<FragmentAnnotation<Object>> custom = new ArrayList<FragmentAnnotation<Object>>();
        for (FragmentAnnotation<Object> t : fAnos) {
            if (!FTSpecials.writeSpecialAnnotation(writer, t.getAnnotationType(), t.get(f))) {
                custom.add(t);
            }
        }
        if (custom.size() > 0) {
            writer.key("annotations");
            writer.object();
            for (FragmentAnnotation<Object> t : custom) {
                writer.key(t.getAnnotationType().getName());
                writer.value(new JSONObject(t.get(f)));
            }
            writer.endObject();
        }
        writer.endObject();
    }

}
