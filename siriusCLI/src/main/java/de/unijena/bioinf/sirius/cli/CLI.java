package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.HelpRequestedException;

import java.util.HashMap;
import java.util.Map;

public class CLI {

    protected final static String VERSION_STRING = "Sirius 3.0.0";

    public static void main(String[] args) {
        final HashMap<String, Task> tasks = new HashMap<String, Task>();
        final IdentifyTask identify = new IdentifyTask();
        tasks.put(identify.getName(), identify);

        if (args.length==0) displayHelp(tasks);

        Task currentTask = null;
        int argStart=0;

        for (int k=0; k < args.length; ++k) {
            if (currentTask==null && (args[k].equals("-h") || args[k].equals("--help"))) {
                displayHelp(tasks); return;
            } else if (args[k].equals("--version")) {
                displayVersion(); return;
            } else if (currentTask==null){
                currentTask = tasks.get(args[k]);
                argStart=k+1;
                if (currentTask==null) {
                    System.err.println("Unknown task: " + args[k]);
                    displayHelp(tasks); return;
                }
            } else if (k==args.length-1) {
                final String[] taskArgs = new String[k-argStart+1];
                System.arraycopy(args, argStart, taskArgs, 0, taskArgs.length);
                try {
                    currentTask.setArgs(taskArgs);
                } catch (HelpRequestedException e) {
                    displayVersion();
                    System.out.println(currentTask.getName() + ":\t" + currentTask.getDescription());
                    System.out.println("");
                    System.out.println(e.getMessage());
                    System.exit(0);
                }
                currentTask.run();
            }
        }

    }

    private static void displayVersion() {
        System.out.println(VERSION_STRING);
    }

    private static void displayHelp(HashMap<String, Task> tasks) {
        displayVersion();
        System.out.println("usage: sirius [COMMAND] [OPTIONS] [INPUT]");
        System.out.println("For documentation of available options use: sirius [COMMAND] --help");
        System.out.println("\nAvailable commands:");
        for (Map.Entry<String, Task> entry : tasks.entrySet()) {
            final String descr = entry.getValue().getDescription().replaceAll("\n", "\n\t\t").replace("\n\t\t$","\n");
            System.out.println("\t" + entry.getKey() + ":\t" + descr);
        }
    }
}
