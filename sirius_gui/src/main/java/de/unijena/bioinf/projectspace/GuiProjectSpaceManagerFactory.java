package de.unijena.bioinf.projectspace;

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class GuiProjectSpaceManagerFactory implements ProjectSpaceManagerFactory<GuiProjectSpaceManager> {

    @Override
    public GuiProjectSpaceManager create(@NotNull SiriusProjectSpace space, @NotNull InstanceFactory<Instance> factory, @Nullable Function<Ms2Experiment, String> formatter) {
        return create(space, new BasicEventList<>(), formatter) ;
    }

    public GuiProjectSpaceManager create(@NotNull SiriusProjectSpace space, @NotNull BasicEventList<InstanceBean> actionList, @Nullable Function<Ms2Experiment, String> formatter) {
        return new GuiProjectSpaceManager(space,actionList,formatter);
    }
}
