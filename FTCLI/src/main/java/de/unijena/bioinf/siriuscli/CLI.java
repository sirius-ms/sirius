package de.unijena.bioinf.siriuscli;


import java.util.Arrays;
import java.util.regex.Pattern;

public class CLI {

    public final static Command[] COMMANDS = new Command[]{
        new TreeCommand(), new LearnCommand(), new DecompCommand(), new IsogenCommand()
    };

    public static void main(String[] args) {
        final Pattern helpPattern = Pattern.compile("-h|--help");
        for (int i=0; i < args.length; ++i ) {
            final String arg = args[i];
            if (helpPattern.matcher(arg).find()) {
                printHelp();
            } else {
                for (Command c : COMMANDS) {
                    if (c.getName().equalsIgnoreCase(arg)) {
                        final String[] commandParameters = Arrays.copyOfRange(args, i+1, args.length);
                        c.run(commandParameters);
                        return;
                    }
                }
                System.err.println("Unknown command '" + arg + "'");
                System.err.flush();
                printHelp();
            }
        }
    }

    private static void printHelp() {
        System.out.println("SIRIUS command line tool\n" +
                "Available commands:");
        for (Command c : COMMANDS) {
            System.out.println("\t" + c.getName() + "\t=>" + c.getDescription());
        }
        System.out.println("\nWrite\nsirius <command> --help\nfor details");
    }

}
