package de.unijena.bioinf.projectspace;

import java.nio.file.Path;
import java.util.function.Function;

public class Location {
    final String relativePath;
    final String fileExt;
    final Function<FormulaResultId, String> filename;


    public Location(String relativePath, String fileExt, Function<FormulaResultId, String> filenameFunction) {
        this.relativePath = relativePath;
        this.filename = filenameFunction;
        this.fileExt = fileExt;
    }

    public String fileExt() {
        return fileExt;
    }

    public String fileExtDot() {
        return "." + fileExt();
    }

    public String fileName(FormulaResultId id) {
        return filename.apply(id) + fileExtDot();
    }

    public String relFilePath(FormulaResultId id) {
        return Path.of(relativePath).resolve(fileName(id)).toString();
    }

    public String relDir() {
        return relativePath;
    }





}
