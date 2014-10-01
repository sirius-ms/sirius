package de.unijena.bioinf.fteval;


public class StdOut implements Interact {
    @Override
    public void say(String s) {
        System.out.print(s);
    }

    @Override
    public void sayln(String s) {
        System.out.println(s);
    }

    @Override
    public boolean ask(String question) {
        return false;
    }

    @Override
    public int choice(String question, String... choices) {
        return 0;
    }
}
