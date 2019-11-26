package de.unijena.bioinf.ms.frontend.io.projectspace;

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

public class GPSMFactory implements ProjectSpaceManagerFactory<GuiProjectSpaceManager> {

    @Override
    public GuiProjectSpaceManager create(@NotNull SiriusProjectSpace space, @NotNull InstanceFactory ignored, @Nullable Function<Ms2Experiment, String> formatter, @Nullable Predicate<CompoundContainerId> compoundFilter) {
        return create(space, new BasicEventList<>(),formatter,compoundFilter) ;
    }

    public GuiProjectSpaceManager create(@NotNull SiriusProjectSpace space, @NotNull BasicEventList<InstanceBean> actionList, @Nullable Function<Ms2Experiment, String> formatter, @Nullable Predicate<CompoundContainerId> compoundFilter) {
        return new GuiProjectSpaceManager(space,actionList,formatter,compoundFilter);
    }
}
