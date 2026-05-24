package reactionTool.sirius.model;

import java.util.List;

public class ParallelStep extends Step {
    private List<Reaction> reactions;

    public List<Reaction> getReactions() {
        return reactions;
    }

    public void setReactions(List<Reaction> reactions) {
        this.reactions = reactions;
    }
}
