package de.unijena.bioinf.ms.middleware;

import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@RequestMapping("/api")
public class BaseApiController {

    protected final SiriusContext context;

    public BaseApiController(SiriusContext context) {
        this.context = context;
    }

    protected SiriusProjectSpace projectSpace(String pid) {
        return context.getProjectSpace(pid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"There is no project space with name '"+pid+"'"));
    }
}
