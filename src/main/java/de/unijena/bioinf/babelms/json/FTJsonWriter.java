package de.unijena.bioinf.babelms.json;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.WriteIntoDataDocument;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
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

    private ParameterHelper helper = ParameterHelper.getParameterHelper();

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
                if (graph.isAliasAnnotation(entry.getKey())) continue;
                if (!FTSpecials.writeSpecialAnnotation(writer, entry.getKey(), entry.getValue())) {
                    writeAnotationValue(writer, entry.getKey(), entry.getValue());
                }
            }
            writer.endObject();
        }

        final List<FragmentAnnotation<Object>> fAnos = graph.getFragmentAnnotations();
        final List<LossAnnotation<Object>> lAnos = graph.getLossAnnotations();

        final Ionization ion = graph.getAnnotationOrNull(Ionization.class);
        final FragmentAnnotation<Peak> peakAno = graph.getFragmentAnnotationOrNull(Peak.class);

        writer.key("fragments");
        writer.array();
        for (Fragment f : graph) {
            writeFragment(writer, f, ion, peakAno!=null ? peakAno.get(f) : null, fAnos);
        }
        writer.endArray();
        writer.key("losses");
        writer.array();
        for (Loss l : graph.losses()) {
            writeLoss(writer, l, lAnos);
        }
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
            if (t.isAlias()) continue;
            if (!FTSpecials.writeSpecialAnnotation(writer, t.getAnnotationType(), t.get(l))) {
                custom.add(t);
            }
        }
        if (custom.size() > 0) {
            for (LossAnnotation<Object> t : custom) {
                final Object v = t.get(l);
                if (v!=null)
                    writeAnotationValue(writer, (Class<Object>)v.getClass(), v);
            }
        }
        writer.endObject();
    }

    private void writeFragment(JSONWriter writer, Fragment f, Ionization ion, Peak p, List<FragmentAnnotation<Object>> fAnos) throws JSONException {
        writer.object();

        writer.key("id");
        writer.value(f.getVertexId());

        writer.key("molecularFormula");
        writer.value(f.getFormula().toString());

        writer.key("mz"); writer.value(p.getMass());
        writer.key("intensity"); writer.value(p.getIntensity());

        if (p!=null && ion!=null) {
            writer.key("massdev");
            writer.value(Deviation.fromMeasurementAndReference(ion.subtractFromMass(p.getMass()), f.getFormula().getMass()).toString());
        }

        final ArrayList<FragmentAnnotation<Object>> custom = new ArrayList<FragmentAnnotation<Object>>();
        for (FragmentAnnotation<Object> t : fAnos) {
            if (t.isAlias()) continue;
            if (!FTSpecials.writeSpecialAnnotation(writer, t.getAnnotationType(), t.get(f))) {
                custom.add(t);
            }
        }
        if (custom.size() > 0) {
            for (FragmentAnnotation<Object> t : custom) {
                final Object v = t.get(f);
                if (v!=null)
                    writeAnotationValue(writer, (Class<Object>)v.getClass(), v);
            }
        }
        writer.endObject();
    }

    protected <T> void writeAnotationValue(JSONWriter writer, Class<T> anoClass, T ano) throws JSONException {
        if (helper.isConvertable(ano)) {
            final JSONDocumentType jsonDocumentType = new JSONDocumentType();
            writer.key(getAnotationName(anoClass, ano));
            writer.value(helper.wrap(jsonDocumentType, ano));
        } else if (ano instanceof WriteIntoDataDocument) {
            final JSONDocumentType jsonDocumentType = new JSONDocumentType();
            final JSONObject dict = jsonDocumentType.newDictionary();
            ((WriteIntoDataDocument)ano).writeIntoDataDocument(jsonDocumentType, dict);
            for (String name : JSONObject.getNames(dict)) {
                writer.key(name);
                writer.value(dict.get(name));
            }
        }
    }

    protected <T> String getAnotationName(Class<T> anoClass, T ano) {
        if (anoClass.isAnnotationPresent(Called.class)) {
            return anoClass.getAnnotation(Called.class).value();
        } else return helper.toClassName(anoClass);
    }

}
