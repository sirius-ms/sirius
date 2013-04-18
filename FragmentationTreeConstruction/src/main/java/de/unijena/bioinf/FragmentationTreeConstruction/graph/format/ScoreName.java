package de.unijena.bioinf.FragmentationTreeConstruction.graph.format;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ScoreName {

    public String value();

}
