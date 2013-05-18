package de.unijena.bioinf.FTAnalysis;

public enum ErrorCode {

    NOERROR(0, "no errors"),
    UNINITIALIZED(1, "instance is not properly initialized"),
    IO(2, "IO exception"),
    DECOMPNOTFOUND(3, "cannot find correct decomposition"),
    REALTREENOTFOUND(4, "cannot find correct tree"),
    OUTOFTIME(6, "computation needs to much time"),
    TOMUCHTIME(7, "computation needs to much time to complete"),
    UNKNOWN(5, "unknown exception");

    private int code;
    private String message;

    private ErrorCode(int code, String message) {
        this.code=code;
        this.message=message;
    }

}
