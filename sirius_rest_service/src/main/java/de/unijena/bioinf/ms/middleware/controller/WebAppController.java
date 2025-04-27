package de.unijena.bioinf.ms.middleware.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebAppController {
    
    @GetMapping({
            "/KMD", "/KMD/**",
            "/formulaTreeView", "/formulaTreeView/**",
            "/epi", "/epi/**",
            "/lcms", "/lcms/**",
            "/libmatch", "/libmatch/**"
    })
    public String serveReactApp(Model model, HttpServletRequest request) {
        // Dynamically build the base URL from the request
        String baseUrl = request.getScheme() + "://" + request.getServerName();

        // Add port if it's not the default port for the scheme
        if (!(request.getScheme().equals("http") && request.getServerPort() == 80) &&
                !(request.getScheme().equals("https") && request.getServerPort() == 443)) {
            baseUrl += ":" + request.getServerPort();
        }

        // Add the context path if any
        if (request.getContextPath() != null && !request.getContextPath().isEmpty()) {
            baseUrl += request.getContextPath();
        }

//        // Add /api to the baseUrl
//        baseUrl += "/api";

        model.addAttribute("apiBaseUrl", baseUrl);
        return "sirius_java_integrated/index";
    }
    
    // Handle the JS files that were previously redirected
    @GetMapping({"/d3-colorbar.js"})
    public String d3ColorbarJs() {
        // Still serve these as static resources
        return "forward:/sirius_java_integrated/d3-colorbar.js";
    }
    
    @GetMapping({"/treeViewer.js"})
    public String treeViewerJs() {
        // Still serve these as static resources
        return "forward:/sirius_java_integrated/treeViewer.js";
    }

    @GetMapping({"/apps/hello-world", "/apps/hello-world/**"})
    public String forwardHelloWorld() {
        return "hello-world/index";
    }
}