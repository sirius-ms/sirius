/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.model.core.run;

import de.unijena.bioinf.babelms.utils.CSVParser;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Factory class for instrument-related meta information.
 * Builds a small library of objects based on the HUPO MS ontology.
 * To fetch the current HUPO MS ontology, go to <a href="https://sparql.hegroup.org/sparql">He group</a> and run:
 *
 * <pre>{@code
 * PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
 * PREFIX obo: <http://purl.obolibrary.org/obo/>
 * PREFIX oio: <http://www.geneontology.org/formats/oboInOwl#>
 * SELECT ?id ?label (group_concat(?syn;separator="|") as ?synonyms) ?desc
 * FROM <http://purl.obolibrary.org/obo/merged/MS>
 * WHERE{
 *   {
 *     SELECT ?super
 *     WHERE {
 *       ?super rdfs:subClassOf obo:<accession>
 *     }
 *   }
 *   ?x rdfs:subClassOf* ?super.
 *   ?x rdfs:label ?label.
 *   ?x oio:id ?id.
 *   ?x obo:IAO_0000115 ?desc.
 *   optional {
 *     ?x oio:hasExactSynonym ?syn
 *   }
 *
 * }
 * group by ?id ?label ?desc
 * order by ?id
 * }</pre>
 *
 * Replace {@code <accession>} by:
 * <ul>
 *  <li>Fragmentation types: {@code MS_1000044}</li>
 *  <li>Ionization types: {@code MS_1000008}</li>
 *  <li>Mass analyzer types: {@code MS_1000443}</li>
 * </ul>
 *
 */
@Slf4j
public class InstrumentConfigs {

    @FunctionalInterface
    private interface Instantiator<T> {

        T call(String hupoId, String fullName, String acronym, String[] synonyms, String description, Pattern pattern);

    }

    //region static instantiation
    protected static final Map<String, Fragmentation> fragmentMap = new ConcurrentHashMap<>();

    protected static final Map<String, Ionization> ionizationMap = new ConcurrentHashMap<>();

    protected static final Map<String, MassAnalyzer> analyzerMap = new ConcurrentHashMap<>();

    static {
        synchronized (fragmentMap) {
            parse(fragmentMap,"/dissociation.csv", (name) -> name.replaceAll("[\\s/-]dissociation", "").trim().replaceAll("[_\\s/-]", "[_\\\\s/-]*"), Fragmentation::new);
            parse(ionizationMap, "/ionization.csv", (name) -> name.replaceAll("[\\s/-]ionization", "").trim().replaceAll("[_\\s/-]", "[_\\\\s/-]*"), Ionization::new);
            parse(analyzerMap, "/mass_analyzers.csv", (name) -> name.replaceAll("[_\\s/-]", "[_\\\\s/-]*"), MassAnalyzer::new);
        }
    }

    private static <T extends InstrumentConfig> void parse(Map<String, T> map, String resource, Function<String, String> namePatternBuilder, Instantiator<T> instantiator) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        Optional.ofNullable(
                                InstrumentConfig.class.getResourceAsStream(resource)).orElseThrow(() -> new FileNotFoundException(resource))))) {
            // skip header
            reader.readLine();
            CSVParser parser = new CSVParser(',', '"', null, true);
            Pattern acSplitter = Pattern.compile("[\\s/;:-]");
            Pattern synSplitter = Pattern.compile("\\|");
            for (Iterator<String[]> it = parser.parse(reader); it.hasNext(); ) {
                String[] cols = it.next(); // HUPO ID, full name, synonyms, description
                String acronym = null;
                String[] synonyms = !cols[2].isBlank() ? synSplitter.split(cols[2]) : new String[0];

                StringBuilder p = new StringBuilder(namePatternBuilder.apply(cols[1]));
                for (String synonym : synonyms) {
                    p.append("|").append(namePatternBuilder.apply(synonym));
                }
                Pattern pattern = Pattern.compile(p.toString(), Pattern.CASE_INSENSITIVE);

                // find matching acronym in synonyms
                if (synonyms.length > 0) {
                    String acCandidate = Arrays.stream(acSplitter.split(cols[1])).map(word -> word.substring(0, 1)).collect(Collectors.joining());
                    for (String synonym : synonyms) {
                        if (acCandidate.equalsIgnoreCase(synonym)) {
                            acronym = synonym;
                        }
                    }
                }
                // else find the shortest synonym that starts with upper case letter
                if (acronym == null) {
                    int l = Integer.MAX_VALUE;
                    for (String synonym : synonyms) {
                        if (synonym.length() < l && synonym.substring(0, 1).equals(synonym.substring(0, 1).toUpperCase())) {
                            acronym = synonym;
                            l = acronym.length();
                        }
                    }
                }
                // remove acronym from synonyms
                if (acronym != null) {
                    final String finalAcronym = acronym;
                    synonyms = Arrays.stream(synonyms).filter(s -> !s.equals(finalAcronym)).toArray(String[]::new);
                }
                // else check if the full name is an acronym
                if (acronym == null) {
                    if (cols[1].equals(cols[1].toUpperCase())) {
                        acronym = cols[1];
                    }
                }

                T ic = instantiator.call(cols[0], cols[1], acronym, synonyms, cols[3], pattern);
                map.put(cols[0], ic);
            }
        } catch (IOException e) {
            log.error("Unable to read data", e);
        }
    }
    //endregion

    //region instance getters
    protected static <T extends InstrumentConfig> Optional<T> byHupoId(@Nonnull String hupoId, @Nonnull Map<String, T> map) {
        return Optional.ofNullable(map.getOrDefault(hupoId, null));
    }

    protected static <T extends InstrumentConfig> Optional<T> byValue(@Nonnull String value, @Nonnull Map<String, T> map) {
        int maxmatch = 0;
        T maxic = null;
        for (T ic : map.values()) {
            int match = ic.pattern.matcher(value).results().map(mr -> mr.end() - mr.start()).max(Integer::compare).orElse(0);
            if (match > 0 && match > maxmatch) {
                maxmatch = match;
                maxic = ic;
            }
        }
        return Optional.ofNullable(maxic);
    }
    //endregion


}
