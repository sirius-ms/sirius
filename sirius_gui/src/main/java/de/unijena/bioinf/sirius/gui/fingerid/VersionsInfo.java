package de.unijena.bioinf.sirius.gui.fingerid;

public class VersionsInfo {

    public String siriusGuiVersion, siriusGuiDate, databaseDate;

    public VersionsInfo(String siriusGuiVersion, String siriusGuiDate, String databaseDate) {
        this.siriusGuiVersion = siriusGuiVersion;
        this.databaseDate = databaseDate;
        this.siriusGuiDate= siriusGuiDate;
    }

    public boolean outdated() {
        return (siriusGuiDate.compareTo(WebAPI.DATE) > 0 && !siriusGuiVersion.equalsIgnoreCase(WebAPI.VERSION));
    }

    public boolean databaseOutdated(String s) {
        return databaseDate.compareTo(s) > 0;
    }
}
