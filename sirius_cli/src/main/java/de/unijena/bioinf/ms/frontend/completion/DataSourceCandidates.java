package de.unijena.bioinf.ms.frontend.completion;

import de.unijena.bioinf.chemdb.DataSource;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class DataSourceCandidates implements Iterable<String> {
    public static final List<String> VALID_DATA_SCOURCES = List.copyOf(Arrays.stream(
            DataSource.values()).filter(it -> it != DataSource.TRAIN).map(DataSource::name)
            .collect(Collectors.toList()));

    public static final String VALID_DATA_STRING ="Possible included DBs: {ALL,BIO,PUBCHEM,MESH,HMDB,KNAPSACK,CHEBI,PUBMED,KEGG,HSDB,MACONDA,METACYC,GNPS,ZINCBIO,UNDP,YMDB,PLANTCYC,NORMAN,ADDITIONAL,PUBCHEMANNOTATIONBIO,PUBCHEMANNOTATIONDRUG,PUBCHEMANNOTATIONSAFETYANDTOXIC,PUBCHEMANNOTATIONFOOD,KEGGMINE,ECOCYCMINE,YMDBMINE}";
    public static final String PATAM_LABEL = "<dbName>[,<dbName>...]";

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return VALID_DATA_SCOURCES.iterator();
    }
}
