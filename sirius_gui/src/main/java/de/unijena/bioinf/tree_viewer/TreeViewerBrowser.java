package de.unijena.bioinf.tree_viewer;

import java.util.Map;

/*
Interface for classes displaying the TreeViewer Javascript file (WebView implementation vs JXBrowser implementation).
Allows communication with JavaScript.
 */
public interface TreeViewerBrowser  {
    public void addJS(String resource_url);
    public void load();
    public void load(Map<String, Object> bridges);
    public void loadTree(String jsonTree);
    public Object executeJS(String js_code);
    public Object getJSObject(String name);
    public Object[] getJSArray(String name);
    public void setJSArray(String name, Object[] newArray);
}
