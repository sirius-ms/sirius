package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Predicate;

public class CompoundContainerIterator implements Iterator<CompoundContainer> {

    private final SiriusProjectSpace space;
    private final Iterator<CompoundContainerId> sourceIterator;
    private final Predicate<CompoundContainer> filter;
    private final Class<? extends DataAnnotation>[] components;

    CompoundContainer next = null;

    public CompoundContainerIterator(@NotNull SiriusProjectSpace space, @NotNull Class<? extends DataAnnotation>... components) {
        this.space = space;
        this.sourceIterator = this.space.iterator();
        this.filter = (c) -> true;
        this.components = components;
    }

    public CompoundContainerIterator(@NotNull SiriusProjectSpace space, @Nullable Predicate<CompoundContainerId> prefilter, @Nullable Predicate<CompoundContainer> filter, @NotNull Class<? extends DataAnnotation>... components) {
        this.space = space;
        this.sourceIterator = prefilter != null ? this.space.filteredIterator(prefilter) : this.space.iterator();
        this.filter = filter != null ? filter : (c) -> true;
        this.components = components;
    }

    @Override
    public boolean hasNext() {
        if (next != null)
            return true;

        if (sourceIterator.hasNext()) {
            final CompoundContainerId cid = sourceIterator.next();
            try {
                CompoundContainer c = space.getCompound(cid, components);
                if (!filter.test(c)) {
                    LoggerFactory.getLogger(getClass()).info("Skipping instance " + cid.getDirectoryName() + " because it does not match the Filter criterion.");
                    return hasNext();
                } else {
                    next = c;
                    return true;
                }
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).error("Could not parse Compound with ID '" + cid.getDirectoryName() + "' Skipping it!");
                return hasNext();
            }
        }
        return false;
    }

    @Override
    public CompoundContainer next() {
        try {
            if (!hasNext())
                return null;
            return next;
        } finally {
            next = null;
        }
    }


}
