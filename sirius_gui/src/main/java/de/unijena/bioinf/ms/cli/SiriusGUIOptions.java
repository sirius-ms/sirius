package de.unijena.bioinf.ms.cli;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 15.06.16.
 */

import com.lexicalscope.jewel.cli.Option;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface SiriusGUIOptions extends FingerIdOptions {
    @Option(shortName = "u", longName = "gui", description = "if set, SIRIUS graphical user interface gets started")
    boolean isGUI();
}
