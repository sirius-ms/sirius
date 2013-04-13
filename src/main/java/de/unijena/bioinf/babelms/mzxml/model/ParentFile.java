/*
 * Sirius MassSpec Tool
 * based on the Epos Framework
 * Copyright (C) 2009.  University of Jena
 *
 * This file is part of Sirius.
 *
 * Sirius is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sirius is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Sirius.  If not, see <http://www.gnu.org/licenses/>;.
*/
package de.unijena.bioinf.babelms.mzxml.model;

import java.io.File;
import java.io.Serializable;

public class ParentFile implements Serializable {

    private static final long serialVersionUID = 4967905531776535629L;

    private String fileName;
    private FileType fileType;
    private String fileSha1;

    public ParentFile() {
        this(null, null, null);
    }

    @Override
    public String toString() {
        if (fileType != null)
            return fileName + " (" + fileType + ")";
        return fileName;
    }

    public ParentFile(String fileName, FileType fileType, String fileSha1) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSha1 = fileSha1;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public File getFile() {
        return new File(fileName);
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public String getFileSha1() {
        return fileSha1;
    }

    public void setFileSha1(String fileSha1) {
        this.fileSha1 = fileSha1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParentFile that = (ParentFile) o;

        if (fileName != null ? !fileName.equals(that.fileName) : that.fileName != null) return false;
        if (fileSha1 != null ? !fileSha1.equals(that.fileSha1) : that.fileSha1 != null) return false;
        if (fileType != that.fileType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fileName != null ? fileName.hashCode() : 0;
        result = 31 * result + (fileType != null ? fileType.hashCode() : 0);
        result = 31 * result + (fileSha1 != null ? fileSha1.hashCode() : 0);
        return result;
    }
}
