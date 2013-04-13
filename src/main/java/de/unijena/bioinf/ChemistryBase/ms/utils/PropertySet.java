package de.unijena.bioinf.ChemistryBase.ms.utils;

public interface PropertySet {
	public <T> T getProperty(String name);
	public <T> T getProperty(String name, T defaultValue);
}
