package de.unijena.bioinf.sirius.cli;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 15.06.16.
 */

import com.lexicalscope.jewel.cli.Option;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface SiriusGUIOptions extends SiriusOptions {
    @Option(shortName = "u", longName = "gui", description = "if set, Sirius 3 graphical user interface gets started")
    boolean isGUI();
}
