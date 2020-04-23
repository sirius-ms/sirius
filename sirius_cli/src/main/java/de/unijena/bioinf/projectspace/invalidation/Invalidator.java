package de.unijena.bioinf.projectspace.invalidation;

import de.unijena.bioinf.projectspace.Instance;

import java.util.function.Consumer;

@FunctionalInterface
public
interface Invalidator extends Consumer<Instance> {

}
