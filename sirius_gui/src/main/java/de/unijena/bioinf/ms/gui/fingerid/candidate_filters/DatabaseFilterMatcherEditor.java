package de.unijena.bioinf.ms.gui.fingerid.candidate_filters;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import de.unijena.bioinf.ms.gui.fingerid.DBFilterPanel;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
/**
 * Created by fleisch on 19.05.17.
 */
public class DatabaseFilterMatcherEditor extends AbstractMatcherEditor<FingerprintCandidateBean> {

    public DatabaseFilterMatcherEditor(DBFilterPanel panel) {
        panel.addFilterChangeListener(filterSet -> fireChanged(new DatabaseMatcher(filterSet)));

    }

    public static class DatabaseMatcher implements Matcher<FingerprintCandidateBean> {
        final long filterSet;

        public DatabaseMatcher(long filterSet) {
            this.filterSet = filterSet;
        }

        @Override
        public boolean matches(FingerprintCandidateBean candidate) {
            return (filterSet == 0 || (filterSet & candidate.getMergedDBFlags()) != 0);
        }
    }
}
