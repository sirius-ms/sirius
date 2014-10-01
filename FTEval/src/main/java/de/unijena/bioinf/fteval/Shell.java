package de.unijena.bioinf.fteval;

import java.io.Console;

public class Shell implements Interact {

    private final Console console = System.console();

    public static Interact withAlternative() {
        if (System.console() != null) return new Shell();
        else return new StdOut();
    }

    @Override
    public void say(String s) {
        console.printf(s);
    }

    @Override
    public void sayln(String s) {
        console.printf(s + "\n");
    }

    @Override
    public boolean ask(String question) {
        console.printf(question + "\n");
        final String answer = console.readLine("Y(es)/N(o): ");
        return (answer.matches("^\\s*[Yy](?:es)?\\s*$"));
    }

    @Override
    public int choice(String question, String... choices) {
        while (true) {
            sayln(question);
            for (int i = 0; i < choices.length; ++i) {
                sayln((i + 1) + ".) " + choices[i]);
            }
            final String answer = console.readLine("Your choice (1 - " + choices.length + "): ");
            try {
                final int choice = Integer.parseInt(answer);
                if (choice >= 1 && choice <= choices.length) return choice - 1;
                else sayln("Invalid input: Please enter number from 1 to " + choices.length);
            } catch (NumberFormatException e) {
                sayln("Invalid input: Please enter number from 1 to " + choices.length);
            }
        }
    }
}
