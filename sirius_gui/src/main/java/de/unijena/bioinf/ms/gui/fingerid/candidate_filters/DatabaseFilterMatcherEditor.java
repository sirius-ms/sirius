package de.unijena.bioinf.ms.gui.fingerid.candidate_filters;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import de.unijena.bioinf.fingerid.db.custom.CustomDataSourceService;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidatePropertyChangeSupport;
import de.unijena.bioinf.ms.gui.fingerid.DBFilterPanel;

import java.util.Map;

/**
 * Created by fleisch on 19.05.17.
 */
public class DatabaseFilterMatcherEditor extends AbstractMatcherEditor<FingerprintCandidatePropertyChangeSupport> {
    Map<String, CustomDataSourceService.Source> SOURCE_MAP = CustomDataSourceService.SOURCE_MAP;

    public DatabaseFilterMatcherEditor(DBFilterPanel panel) {
        panel.addFilterChangeListener(filterSet -> fireChanged(new DatabaseMatcher(filterSet)));

    }

    public static class DatabaseMatcher implements Matcher<FingerprintCandidatePropertyChangeSupport> {
        final long filterSet;

        public DatabaseMatcher(long filterSet) {
            this.filterSet = filterSet;
        }

        @Override
        public boolean matches(FingerprintCandidatePropertyChangeSupport candidate) {
            return (filterSet == 0 || (filterSet & candidate.getFingerprintCandidate().getBitset()) != 0);
        }
    }
}
