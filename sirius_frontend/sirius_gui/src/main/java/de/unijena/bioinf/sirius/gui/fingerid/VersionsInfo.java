package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.dialogs.News;

import java.util.*;

public class VersionsInfo {

    public String siriusGuiVersion, siriusGuiDate, databaseDate;
    protected List<News> newsList;

    public VersionsInfo(String siriusGuiVersion, String siriusGuiDate, String databaseDate) {
        this(siriusGuiVersion, siriusGuiDate, databaseDate, null);
    }

    public VersionsInfo(String siriusGuiVersion, String siriusGuiDate, String databaseDate, List<News> newsList) {
        this.siriusGuiVersion = siriusGuiVersion;
        this.siriusGuiDate = siriusGuiDate;
        this.databaseDate = databaseDate;
        this.newsList = filterNews(newsList);
    }

    /**
     * filter already seen news
     * @param newsList
     * @return
     */
    private List<News> filterNews(List<News> newsList){
        List<News> filteredNews = new ArrayList<>();
        final String property = News.PROPERTY_KEY;
        Properties properties = ApplicationCore.getUserCopyOfUserProperties();
        final String propertyValue = properties.getProperty(property, "");
        Set<String> knownNews = new HashSet<>(Arrays.asList(propertyValue.split(",")));
        for (News news : newsList) {
            if (!knownNews.contains(news.getId())) filteredNews.add(news);
        }
        return filteredNews;
    }

    public boolean outdated() {
        return (siriusGuiDate.compareTo(WebAPI.DATE) > 0 && !siriusGuiVersion.equalsIgnoreCase(WebAPI.VERSION));
    }

    public boolean hasNews(){
        return  (newsList!=null && newsList.size()>0);
    }

    public List<News> getNews() {
        return newsList;
    }

    public boolean databaseOutdated(String s) {
        return databaseDate.compareTo(s) > 0;
    }
}
