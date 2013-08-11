package de.unijena.bioinf.fteval;

public interface Interact {


    public void say(String s);
    public void sayln(String s);
    public boolean ask(String question);
    public int choice(String question, String... choices);

}
