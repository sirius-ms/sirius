package de.unijena.bioinf.babelms;

import de.unijena.bioinf.jjobs.BasicJJob;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class GenericParserJJob<R> extends BasicJJob<List<R>> {
    public final GenericParser<R> parser;
    public final File input;

    public GenericParserJJob(GenericParser<R> parser, File input) {
        this.parser = parser;
        this.input = input;
    }

    @Override
    protected List<R> compute() throws Exception {
        checkForInterruption();
        CloseableIterator<R> elements = parser.parseFromFileIterator(input);
        List<R> result = new LinkedList<>();
        checkForInterruption();
        while (elements.hasNext()) {
            result.add(elements.next());
            checkForInterruption();
        }
        checkForInterruption();
        return result;
    }
}
