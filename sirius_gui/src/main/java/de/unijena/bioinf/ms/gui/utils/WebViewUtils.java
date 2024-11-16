package de.unijena.bioinf.ms.gui.utils;

import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WebViewUtils {

    /**
     *
     * @param textFileAsSingleString multiple lines with line separator as single string
     * @return
     */
    public static String textToDataURL(String textFileAsSingleString) {
        byte[] bytes = textFileAsSingleString.getBytes(StandardCharsets.UTF_8);
        String dataUrl = "data:text/plain;base64," +
                Base64.getEncoder().encodeToString(bytes);
        return dataUrl;
    }

    public static String loadCSSAndSetColorThemeAndFont(String stylesCssResource) {
        InputStream stream = WebViewUtils.class.getResourceAsStream(stylesCssResource);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                return bufferedReader.lines().map(WebViewUtils::specifyColor).map(WebViewUtils::specifyFont).collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final static Pattern COLOR_VARIABLE = Pattern.compile("\\$([A-Za-z0-9_.]+)");
    private static String specifyColor(String line) {
        Matcher m = COLOR_VARIABLE.matcher(line);
        String replacedLine = m.replaceAll((mr) -> {
            String varName = mr.group(1);

            try {
                int classSplitIndex = varName.lastIndexOf(".");
                Class colorClass;
                if (classSplitIndex < 0) {
                    colorClass = Colors.class;
                } else {
                    colorClass = Class.forName("de.unijena.bioinf.ms.gui.configs.Colors$" + varName.substring(0, classSplitIndex).replace(".", "$"));
                    varName = varName.substring(classSplitIndex + 1);
                }

                Color color = (Color) colorClass.getField(varName).get(null);
                if (color == null) {
                    LoggerFactory.getLogger(WebViewUtils.class).warn("Missing color: " + varName);
                    return Colors.asHex(Colors.FOREGROUND_DATA); //hopefully this is text. Just guessing.
                }
                return Colors.asHex(color);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            } catch (ClassCastException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        return replacedLine;
    }

    private static String specifyFont(String line) {
        return  line.replace("#FONT_URL#", Fonts.getFontURLExternalForm());
    }
}
