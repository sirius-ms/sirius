package de.unijena.bioinf.ms.gui.tree_viewer;

import java.util.Map;

/*
Interface for classes displaying the TreeViewer Javascript file (WebView implementation vs JXBrowser implementation).
Allows communication with JavaScript.
 */
public interface TreeViewerBrowser  {
    void addJS(String resource_url);
    void load();
    void load(Map<String, Object> bridges);
    void loadTree(String jsonTree);
    Object executeJS(String js_code);
    Object getJSObject(String name);
    Object[] getJSArray(String name);
    void setJSArray(String name, Object[] newArray);
}
