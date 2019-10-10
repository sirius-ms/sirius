package de.unijena.bioinf.sirius;

@FunctionalInterface
public interface SiriusFactory {
    Sirius sirius(String profile);
}
