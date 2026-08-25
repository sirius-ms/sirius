package reactionTool.sirius.model;

public class Reaction {
    private String name;
    private String smarts;
    /**
     * Whether this reaction is one of the defaults shipped with the application (seeded from the
     * bundled reactionLibrary.json) as opposed to one a user created. Defaults to {@code false} so any
     * reaction added through the normal code paths is treated as user-created unless explicitly marked.
     */
    private boolean preshipped;

    public Reaction() {
    }

    public Reaction(String name, String smarts) {
        this(name, smarts, false);
    }

    public Reaction(String name, String smarts, boolean preshipped) {
        this.name = name;
        this.smarts = smarts;
        this.preshipped = preshipped;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSmarts() {
        return smarts;
    }

    public void setSmarts(String smarts) {
        this.smarts = smarts;
    }

    public boolean isPreshipped() {
        return preshipped;
    }

    public void setPreshipped(boolean preshipped) {
        this.preshipped = preshipped;
    }
}
