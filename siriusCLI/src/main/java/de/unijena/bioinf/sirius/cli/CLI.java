package de.unijena.bioinf.sirius.cli;

import java.util.HashMap;

public class CLI {

    public static void main(String[] args) {
        final HashMap<String, Task> tasks = new HashMap<String, Task>();
        final IdentifyTask identify = new IdentifyTask();
        tasks.put(identify.getName(), identify);

        if (args.length==0) displayHelp();

        Task currentTask = null;
        int argStart=0;

        for (int k=0; k < args.length; ++k) {
            if (args[k].equals("-h") || args[k].equals("--help")) {
                displayHelp(); return;
            } else if (args[k].equals("--version")) {
                displayVersion(); return;
            } else if (currentTask==null){
                currentTask = tasks.get(args[k]);
                argStart=k+1;
                if (currentTask==null) {
                    System.err.println("Unknown task: " + args[k]);
                    displayHelp(); return;
                }
            } else if (k==args.length-1) {
                final String[] taskArgs = new String[k-argStart+1];
                System.arraycopy(args, argStart, taskArgs, 0, taskArgs.length);
                currentTask.setArgs(taskArgs);
                currentTask.run();
            }
        }

    }

    private static void displayVersion() {

    }

    private static void displayHelp() {

    }
}
