package de.unijena.bioinf.ms.cli;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.jjobs.JobManager;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class SiriusCLIApplication {
    public static void main(String[] args) throws InterruptedException {
        long t = System.currentTimeMillis();
        try {
            final FingeridCLI<FingerIdOptions> cli = new FingeridCLI<>();
            if (isZodiac(args)){
                if (args.length==1)  cli.println(CliFactory.createCli(ZodiacOptions.class).getHelpMessage());
                System.exit(0);
                ZodiacOptions options = null;
                try {
                    options = CliFactory.createCli(ZodiacOptions.class).parseArguments(removeFromArrayIgnoreCase(args, "--zodiac"));
                } catch (HelpRequestedException e) {
                    cli.println(e.getMessage());
                    cli.println("");
                    System.exit(0);
                }

                Zodiac zodiac = new Zodiac(options);
                zodiac.run();
            } else {
                cli.parseArgsAndInit(args, FingerIdOptions.class);
                cli.compute();
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(SiriusCLIApplication.class).error("Unkown Error!", e);
        } finally {
            System.out.println("Time: " + ((double) (System.currentTimeMillis() - t)) / 1000d);
            try {
                JobManager.shutDownAllInstances();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    private static boolean isZodiac(String[] args) {
        for (String arg : args) {
            if (arg.toLowerCase().equals("--zodiac")) return true;
        }
        return false;
    }

    private static String[] removeFromArrayIgnoreCase(String[] args, String param) {
        int idx = -1;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase(param)){
                idx = i;
                break;
            }

        }
        if (idx<0) return args.clone();
        String[] argsNew = Arrays.copyOf(args, args.length-1);
        for (int i = idx+1; i < args.length; i++) {
            argsNew[i-1] = args[i];
        }
        return argsNew;
    }
}
