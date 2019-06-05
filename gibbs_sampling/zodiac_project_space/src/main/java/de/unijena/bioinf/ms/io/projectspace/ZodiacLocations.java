package de.unijena.bioinf.ms.io.projectspace;

public interface ZodiacLocations extends SiriusLocations {
    Location ZODIAC_SUMMARY = new Location("zodiac", "scores", ".csv");
    Location ZODIAC_NET = new Location("zodiac", "net", ".csv");
}
