package de.unijena.bioinf.fingerid.candidate_filters;

import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.fingerid.CompoundCandidate;

import javax.swing.text.JTextComponent;
import java.util.List;

/**
 * Created by fleisch on 18.05.17.
 */
public class CandidateStringMatcherEditor extends TextComponentMatcherEditor<CompoundCandidate> {

    public CandidateStringMatcherEditor(JTextComponent textComponent) {
        super(textComponent, new TextFilterator<CompoundCandidate>() {
            @Override
            public void getFilterStrings(List<String> baseList, CompoundCandidate element) {
                baseList.add(element.getMolecularFormula());
                baseList.add(element.getName());
                baseList.add(element.getInChiKey());
                baseList.add(element.getCompound().getInchi().in3D);
                baseList.add(element.getCompound().getSmiles());
            }
        });
    }
}
