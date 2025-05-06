/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.configs;

import java.util.ArrayList;
import java.util.List;
import java.awt.Color; // Using standard Java Color class

public class ColorGenerator {

    /**
     * Generates a list of Colors,
     * converting the generated HSV values directly to java.awt.Color objects.
     * The underlying HSV generation logic is:
     * i=0: -> HSV(0.0, 0.6, 0.9)
     * i=1: j=1 -> HSV(1/2, 0.6, 0.9)
     * i=2: j=1,3 -> HSV(1/4, 0.6, 0.9), HSV(3/4, 0.6, 0.9)
     * etc.
     *
     * @param limit The maximum number of Color objects to generate.
     * @return A list of java.awt.Color objects.
     */
    public static List<Color> generateColors(int limit) {
        List<Color> results = new ArrayList<>();
        if (limit <= 0) {
            return results; // Return empty list if limit is zero or negative
        }

        // Fixed saturation and value (brightness in HSB terms)
        // Convert to float as required by Color.HSBtoRGB
        final float saturation = 0.6f;
        final float brightness = 0.9f; // HSB brightness = HSV value

        // 1. Add the first specific color: HSB(0.0, 0.6, 0.9)
        float firstHue = 0.0f;
        int firstRgbInt = Color.HSBtoRGB(firstHue, saturation, brightness);
        results.add(new Color(firstRgbInt)); // Create Color from packed RGB int

        if (results.size() >= limit) {
            return results; // Return if limit was 1
        }

        // 2. Loop through i, starting from 1
        // Continue looping as long as we haven't reached the desired limit.
        for (int i = 1; results.size() < limit; i++) {
            // Calculate the denominator: 2^i
            long denominator = 1L << i; // Equivalent to Math.pow(2, i)

            // Inner loop for j: range(1, 2**i, 2) -> 1, 3, 5, ..., denominator - 1
            for (long j = 1; j < denominator; j += 2) {
                // Calculate the hue component and cast to float
                float hue = (float) ((double) j / denominator); // Ensure floating-point division before casting

                // Convert HSB to RGB integer
                int rgbInt = Color.HSBtoRGB(hue, saturation, brightness);

                // Create the Color object
                Color color = new Color(rgbInt);

                // Add to results
                results.add(color);

                // Check if we have generated enough colors
                if (results.size() >= limit) {
                    break; // Exit the inner loop (j loop)
                }
            }
            // The outer loop condition (results.size() < limit) handles termination.
        }

        return results;
    }

    public static Color desaturate(Color color) {
        float[] hsb = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
        return new Color(Color.HSBtoRGB(hsb[0], hsb[1] / 2f, hsb[2]));
    }

    /**
     * Helper method to convert a Color object to a CSS hex string (#RRGGBB).
     * @param color The Color object.
     * @return CSS hex string.
     */
    public static String colorToCss(Color color) {
        // getRGB() returns packed ARGB, mask out alpha using 0xFFFFFF
        return String.format("#%06x", color.getRGB() & 0xFFFFFF);
    }


    public static void main(String[] args) {
        int numberOfColors = 26;
        List<Color> colorList = generateColors(numberOfColors);

        System.out.println("Generated " + colorList.size() + " Colors:");
        for (int i = 0; i < colorList.size(); i++) {
            Color color = colorList.get(i);
            //System.out.print("\"" + colorToCss(color) + "\", ");
            System.out.printf("%3d: %-35s CSS: %s%n",
                    i + 1,
                    color.toString(), // e.g., java.awt.Color[r=230,g=92,b=92]
                    colorToCss(color) // e.g., #e65c5c
            );
        }
        System.out.println();
    }
}
