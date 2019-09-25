package de.unijena.bioinf.ms.frontend.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;

public class ResultOutputHandler extends ConsoleHandler {

    public ResultOutputHandler(){
        setOutputStream(System.out);
        //todo we want plain formatter!
        setFormatter(new SimpleFormatter());
    }


}
