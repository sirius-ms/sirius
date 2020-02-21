package de.unijena.bioinf.ms.middleware.version;

import de.unijena.bioinf.ms.middleware.BaseApiController;
import de.unijena.bioinf.ms.middleware.SiriusContext;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionInfoController extends BaseApiController {

    private final SiriusContext context;

    @Autowired
    public VersionInfoController(SiriusContext context) {
        super(context);
        this.context = context;
    }


    @RequestMapping(value = "/version.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getVersionInfo() {
        return "{" +
                "\"version\": \"0.1.0-SNAPSHOT\"" +
                ", \"sirius_version\": \"" + PropertyManager.getProperty("de.unijena.bioinf.sirius.version") + "\"" +
                ", \"fingerid_version\": \"" + PropertyManager.getProperty("de.unijena.bioinf.fingerid.version") + "\"" +
                "}";
    }


}
