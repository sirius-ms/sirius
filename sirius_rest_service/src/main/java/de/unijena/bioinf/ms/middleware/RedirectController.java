package de.unijena.bioinf.ms.middleware;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Redirects should be defined here.
 * This class is ignored by the API doc
 */
@Controller
@ApiIgnore
public class RedirectController {
    @RequestMapping(path = {"/", "/api"})
    public String swaggerRoot() {
        return "redirect:/swagger-ui.html";
    }
}
