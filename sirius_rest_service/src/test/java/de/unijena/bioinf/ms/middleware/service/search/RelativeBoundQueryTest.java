/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.projectspace.IndexField;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A numeric bound followed by {@code %} is matched against the field's relative form - the indexed field with the
 * suffix {@code Relative}, holding the value in percent of its reference. For the intensities that reference is
 * the most intense feature of the same run, so "at least 10 % of the run maximum" can be written in the query
 * itself. Anchored on an intensity, but the rule is the same for every numeric field that has a relative form.
 */
public class RelativeBoundQueryTest {

    @NoArgsConstructor
    @AllArgsConstructor
    public static class Measured {
        @IndexField(documentId = true)
        public String id;
        /** raw MS1 apex intensity as the API reports it */
        @IndexField(sortable = true)
        public Double intensity;
        /** the same in percent of the run maximum, written when the document is built */
        @IndexField(sortable = true)
        public Double intensityRelative;
        /** numeric, but with no relative form */
        @IndexField
        public Double plain;
        @IndexField(fullTextSearch = true, defaultSearchField = true)
        public String name;
    }

    private SearchService searchService;
    private Project<?> project;
    private final String projectId = "relative-bound-test";

    @BeforeEach
    public void setup() throws IOException {
        project = Mockito.mock(Project.class);
        Mockito.when(project.getProjectId()).thenReturn(projectId);
        Mockito.when(project.getSystemUID()).thenReturn("test-system-uid");
        searchService = new SearchServiceImpl(p -> new PerPojoSearchContext(null, new HashMap<>()));
        searchService.openOrCreateProjectIndex(project);
        // the worked example: run A's maximum is 1,000,000, run B's 2,000,000, run C's 500,000. The weak feature
        // sits in run C, the moderate one in A, the strongest one is B's maximum; one feature has no intensity
        searchService.addDocument(projectId, new Measured("weak", 100_000d, 20d, 1d, "weak 50% signal"));
        searchService.addDocument(projectId, new Measured("moderate", 400_000d, 40d, 2d, "moderate"));
        searchService.addDocument(projectId, new Measured("strongest", 2_000_000d, 100d, 3d, "strongest"));
        searchService.addDocument(projectId, new Measured("unknown", null, null, 4d, "no intensity"));
    }

    @AfterEach
    public void cleanup() throws IOException {
        searchService.closeProjectIndex(project, true);
    }

    private List<String> ids(String query) {
        return searchService.search(projectId, query, PageRequest.of(0, 10), Measured.class)
                .getContent().stream().map(m -> m.id).sorted().toList();
    }

    @Test
    public void testPercentageIsMatchedAgainstTheRelativeForm() {
        // the weak feature is only 100,000 counts but 20 % of its own run's maximum, so it passes a 10 % bound
        assertEquals(List.of("moderate", "strongest", "weak"), ids("intensity:[10% TO *]"));
        assertEquals(List.of("moderate", "strongest"), ids("intensity:[30% TO *]"));
        assertEquals(List.of("strongest"), ids("intensity:[50% TO *]"));
    }

    @Test
    public void testComparisonSyntaxAndUpperBounds() {
        assertEquals(List.of("moderate", "strongest"), ids("intensity>=40%"));
        assertEquals(List.of("weak"), ids("intensity:[* TO 20%]"));
        assertEquals(List.of("moderate"), ids("intensity:{20% TO 99%}"));
    }

    @Test
    public void testAbsoluteBoundsAreUntouched() {
        assertEquals(List.of("moderate", "strongest"), ids("intensity:[300000 TO *]"));
    }

    @Test
    public void testTheRelativeFormCanBeQueriedDirectly() {
        assertEquals(ids("intensity:[30% TO *]"), ids("intensityRelative:[30 TO *]"));
    }

    @Test
    public void testAPercentSignInATextFieldIsJustACharacter() {
        assertEquals(List.of("weak"), ids("name:50%"));
    }

    @Test
    public void testMalformedPercentageIsTheClientsError() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> ids("intensity:[ten% TO *]"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    public void testMixingAPercentageWithAnAbsoluteBoundIsRefused() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> ids("intensity:[100000 TO 50%]"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    public void testAFieldWithoutARelativeFormRefusesThePercentage() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> ids("plain:[10% TO *]"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertTrue(String.valueOf(e.getReason()).contains("relative form"), e.getReason());
    }
}
