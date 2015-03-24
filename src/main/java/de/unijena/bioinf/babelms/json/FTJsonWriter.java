package de.unijena.bioinf.babelms.json;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import org.json.JSONException;
import org.json.JSONWriter;

import java.util.Map;

public class FTJsonWriter {

    public void writeGraph(FGraph graph) {

    }

    protected void writeGraph(FTree graph, JSONWriter writer) throws JSONException {
        writer.object();
        writer.key("molecularFormula");
        writer.value(graph.getRoot().getFormula());
        writer.key("annotations");
        {
            writer.object();
            for (Map.Entry<Class<Object>, Object> entry : graph.getAnnotations().entrySet()) {
                writer.endObject();
            }
        }
        writer.endObject();
    }

}
