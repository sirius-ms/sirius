package de.unijena.bioinf.lcms.adducts;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.algorithm.BinarySearch;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AdductNetwork {

    protected AdductNode[] rtOrderedNodes;
    protected AdductManager adductManager;

    protected Deviation deviation;

    ProjectSpaceTraceProvider provider;

    public AdductNetwork(ProjectSpaceTraceProvider provider, AlignedFeatures[] features, AdductManager manager) {
        this.rtOrderedNodes = new AdductNode[features.length];
        for (int k=0; k < features.length; ++k) {
            rtOrderedNodes[k] = new AdductNode(features[k]);
        }
        Arrays.sort(rtOrderedNodes, Comparator.comparingDouble(AdductNode::getRetentionTime));
        adductManager = manager;
        deviation = new Deviation(10);
    }

    protected void buildNetworkFromMassDeltas() {
        for (int l=0; l < rtOrderedNodes.length; ++l) {
            final AdductNode leftNode = rtOrderedNodes[l];
            final double thresholdLeft = rtOrderedNodes[l].getFeature().getRetentionTime().getStartTime();
            final double thresholdRight = rtOrderedNodes[l].getFeature().getRetentionTime().getEndTime();
            final Range<Double> threshold = Range.closed(thresholdLeft, thresholdRight);

            int rLeft=l-1;
            for (; rLeft >= 0; --rLeft) {
                if (!threshold.contains(rtOrderedNodes[rLeft].getRetentionTime() )  ) {
                    break;
                }
            }
            ++rLeft;

            int rRight=l+1;
            for (; rRight < rtOrderedNodes.length; ++rRight) {
                if (!threshold.contains(rtOrderedNodes[rRight].getRetentionTime() )  ) {
                    break;
                }
            }
            --rRight;

            for (int i=rLeft; i <= rRight; ++i) {
                if (i != l) {
                    final AdductNode rightNode = rtOrderedNodes[i];
                    if (rightNode.features.getRetentionTime().asRange().contains(leftNode.getRetentionTime())) {
                        final double massDelta = rightNode.getMass() - leftNode.getMass();
                        List<KnownMassDelta> knownMassDeltas = adductManager.retrieveMassDeltas(massDelta, deviation);
                        if (!knownMassDeltas.isEmpty()) {
                            leftNode.addEdge(rightNode, knownMassDeltas.toArray(KnownMassDelta[]::new));
                        }
                    }
                }
            }

        }

    }

    protected AdductNode[] findNodesByRt(double rtFrom, double rtTo) {
        if (rtTo < rtFrom) {
            throw new IllegalArgumentException("Illegal mass range: rtFrom = " + rtFrom +  " and is larger than rtTo = " + rtTo);
        }
        int i = BinarySearch.searchForDoubleByIndex(j-> rtOrderedNodes[j].getRetentionTime(),
                0, rtOrderedNodes.length, rtFrom);
        if (i < 0)  {
            i = -(i + 1);
        }
        if (i >= rtOrderedNodes.length || rtOrderedNodes[i].getMass() > rtTo) return new AdductNode[0];
        final int start = i;
        for (i=start+1; i < rtOrderedNodes.length; ++i) {
            if (rtOrderedNodes[i].getRetentionTime() > rtTo) {
                break;
            }
        }
        final int end = i;
        final AdductNode[] sublist = new AdductNode[start-end];
        System.arraycopy(rtOrderedNodes, start, sublist, 0, end-start);
        return sublist;
    }
}
