package reactionTool.sirius.model;

import java.util.List;

public class LoopStep extends Step {
    private int iterations;
    private List<Step> steps;

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }
}
