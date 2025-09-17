package de.unijena.bioinf.ms.frontend.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatabaseLinkUtil {

    public static String links(Map<String, List<String>> databases) throws IOException {
        return databases.entrySet().stream().map(e -> e.getKey() + joinDBLinks(e.getValue())).collect(Collectors.joining(";"));
    }

    private static String joinDBLinks(List<String> links) {
        String joined = links.stream().filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(" "));
        return joined.isEmpty() ? "" : ":(" + joined + ")";
    }
}
