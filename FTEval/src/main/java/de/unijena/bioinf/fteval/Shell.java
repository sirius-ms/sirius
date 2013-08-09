package de.unijena.bioinf.fteval;

import java.io.Console;

public class Shell implements Interact {

    private final Console console = System.console();

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
}
