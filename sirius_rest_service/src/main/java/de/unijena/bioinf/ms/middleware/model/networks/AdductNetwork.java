package de.unijena.bioinf.ms.middleware.model.networks;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
@Schema(name = "AdductNetworkExperimental",
        description = "EXPERIMENTAL: This schema is experimental and may be changed (or even removed) without notice until it is declared stable.")
public class AdductNetwork {

    private ArrayList<AdductNode> nodes;
    private List<AdductEdge> edges;

    public AdductNetwork() {
    }

    public static AdductNetwork from(de.unijena.bioinf.ms.persistence.model.core.networks.AdductNetwork network) {
        AdductNetwork jnet = new AdductNetwork();
        jnet.nodes = new ArrayList<>();
        HashMap<Long,Integer> dict = new HashMap<>();
        for (int k=0; k < network.getNodes().size(); ++k) {
            de.unijena.bioinf.ms.persistence.model.core.networks.AdductNode node = network.getNodes().get(k);
            AdductNode jnode = new AdductNode();
            jnode.setMz(node.getMz());
            final HashMap<String,Double> ano = new HashMap<>();
            for (int i=0; i < node.getAdductProbabilities().length; ++i) {
                ano.put(node.getPossibleAdducts()[i].toString(), (double)(node.getAdductProbabilities()[i]));
            }
            jnode.setAdductAnnotations(ano);
            jnode.setAlignedFeatureId(node.getAlignedFeatureId());
            dict.put(node.getAlignedFeatureId(), k);
            jnet.nodes.add(jnode);
        }
        jnet.edges = new ArrayList<>();
        for (int k=0; k < network.getEgdes().size(); ++k) {
            de.unijena.bioinf.ms.persistence.model.core.networks.AdductEdge edge = network.getEgdes().get(k);
            AdductEdge jedge = new AdductEdge();
            jedge.setFrom(dict.get(edge.getLeftFeatureId()));
            jedge.setTo(dict.get(edge.getRightFeatureId()));
            AdductNode a = jnet.getNodes().get(jedge.from);
            AdductNode b = jnet.getNodes().get(jedge.to);
            jedge.setMzDelta(Math.abs(a.mz-b.mz));
            if (a.mz > b.mz) {
                System.err.println("check");
            }
            jedge.setPvalue(edge.getPvalue());
            jedge.setIntensityRatioScore(edge.getIntensityRatioScore());
            jedge.setMergedCorrelation(edge.getMergedCorrelation());
            jedge.setMs2cosine(edge.getMs2cosine());
            jedge.setRepresentativeCorrelation(edge.getRepresentativeCorrelation());
            jedge.setAnnotation(edge.getLabel());
            // todo: add cosine stuff
            jnet.edges.add(jedge);
        }
        return jnet;
    }

    @Getter
    @Setter
    @Schema(name = "AdductEdgeExperimental",
            description = "EXPERIMENTAL: This schema is experimental and may be changed (or even removed) without notice until it is declared stable.")
    public static class AdductEdge {

        private double mzDelta;
        private String annotation;
        private int from, to;
        private float mergedCorrelation, representativeCorrelation, ms2cosine, pvalue, intensityRatioScore;

        public AdductEdge() {
        }

    }

    @Getter
    @Setter
    @Schema(name = "AdductNodeExperimental",
            description = "EXPERIMENTAL: This schema is experimental and may be changed (or even removed) without notice until it is declared stable.")
    public static class AdductNode {
        long alignedFeatureId;
        private double mz;
        private Map<String, Double> adductAnnotations;

        public AdductNode() {
        }
    }

}
