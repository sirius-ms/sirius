package de.unijena.bioinf.babelms.projectspace;

import de.unijena.bioinf.babelms.projectspace.SiriusLocations;

public interface ZodiacLocations extends SiriusLocations {
    Location ZODIAC_SUMMARY = new Location("zodiac", "scores", ".csv");
    Location ZODIAC_NET = new Location("zodiac", "net", ".csv");
}
