package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ms.frontend.subtools.Instance;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.projectspace.StandardMSFilenameFormatter;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Function;

/**
 * Manage the project space.
 * e.g. iteration on Instance level.
 * maybe some type of caching?
 */
public class ProjectSpaceManager implements Iterable<Instance> {

    private final SiriusProjectSpace space;
    protected Function<Ms2Experiment, String> nameFormatter = new StandardMSFilenameFormatter();


    public ProjectSpaceManager(@NotNull SiriusProjectSpace space) {
        this(space, null);
    }

    public ProjectSpaceManager(@NotNull SiriusProjectSpace space, @Nullable Function<Ms2Experiment, String> formatter) {
        this.space = space;
        if (formatter != null)
            nameFormatter = formatter;
    }

    public SiriusProjectSpace projectSpace() {
        return space;
    }

    public Function<Ms2Experiment, String> nameFormatter() {
        return nameFormatter;
    }


    //todo lets write the experiment if the configuration merging is done (Add configs jobs)
    // todo just to save soe io
    /*@NotNull
    public CompoundContainerId newUniqueCompoundId(Ms2Experiment inputExperiment) {
        final String name = nameFormatter().apply(inputExperiment);
        try {
            final CompoundContainerId id = projectSpace().newUniqueCompoundId(name, (idx) -> idx + "_" + name).orElseThrow(()->new RuntimeException("Could not create an project space ID for the Instance" ));
            final CompoundContainer container = projectSpace().getCompound(id);
            container.setAnnotation(Ms2Experiment.class, inputExperiment);
            projectSpace().updateCompound(container, Ms2Experiment.class);
            return id;
        } catch (IOException e) {
            LoggerFactory.getLogger(ProjectSpaceManager.class).error("Could not create an project space ID for the Instance", e);
            throw new RuntimeException("Could not create an project space ID for the Instance");
        }
    }*/

    @NotNull
    public CompoundContainerId newUniqueCompoundId(Ms2Experiment inputExperient) {
        final String name = nameFormatter().apply(inputExperient);
        return projectSpace().newUniqueCompoundId(name, (idx) -> idx + "_" + name)
                .orElseThrow(() -> new RuntimeException("Could not create an project space ID for the Instance"));
    }


    @NotNull
    @Override
    public Iterator<Instance> iterator() {
        return new Iterator<>() {
            final Iterator<CompoundContainerId> it = space.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Instance next() {
                final CompoundContainerId id = it.next();
                if (id == null) return null;
                return new Instance(ProjectSpaceManager.this, id);
            }
        };
    }
}
