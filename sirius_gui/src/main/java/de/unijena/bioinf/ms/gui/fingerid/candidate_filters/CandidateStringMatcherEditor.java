package de.unijena.bioinf.ms.gui.fingerid.candidate_filters;

import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidatePropertyChangeSupport;

import javax.swing.text.JTextComponent;

/**
 * Created by fleisch on 18.05.17.
 */
public class CandidateStringMatcherEditor extends TextComponentMatcherEditor<FingerprintCandidatePropertyChangeSupport> {

    public CandidateStringMatcherEditor(JTextComponent textComponent) {
        super(textComponent, (baseList, element) -> {
            baseList.add(element.getMolecularFormula());
            baseList.add(element.getName());
            baseList.add(element.getInChiKey());
            baseList.add(element.getFingerprintCandidate().getInchi().in3D);
            baseList.add(element.getFingerprintCandidate().getSmiles());
        });
    }
}
