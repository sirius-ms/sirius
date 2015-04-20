package de.unijena.bioinf.sirius.cli;

import java.io.File;
import java.util.*;

public class TaskHandler {

    public static TaskHandler create(Options options) {
        final List<String> opts = options.getCommands();
        final List<String> cmds = new ArrayList<String>();
        final List<File> files = new ArrayList<File>();
        boolean listenToCommands = true;
        for (String s : opts) {
            if (listenToCommands && AVAILABLE_COMMANDS.contains(s.toUpperCase())) {
                cmds.add(s);
            } else {
                listenToCommands=false;
                final File f = new File(s);
                if (f.isDirectory()) {
                    for (File g : f.listFiles()) {
                        if (g.isFile()) {
                            files.add(g);
                        }
                    }
                } else {
                    files.add(f);
                }
            }
        }
        final TaskHandler h = new TaskHandler();
        h.files=files;
        h.commands = cmds;
        return h;
    }

    protected static Set<String> AVAILABLE_COMMANDS = new HashSet<String>(Arrays.asList("IDENTIFY"));

    protected List<File> files;
    protected List<String> commands;

    public List<File> getInputFiles() {
        return files;
    }


}
