package org.motechproject.mds.web.controller;

import org.motechproject.mds.service.RestDocumentationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller that serves json documentation of the REST API.
 * This output is then displayed by the Swagger UI.
 */
@Controller
public class RestDocumentationController {

    @Autowired
    private RestDocumentationService restDocService;

    @RequestMapping(value = "/rest-doc", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public void printMdsRestDocumentation(@RequestParam(value = "serverPrefix", required = false) String serverPrefix,
                                              HttpServletResponse response) throws IOException {
        restDocService.retrieveDocumentation(response.getWriter(), serverPrefix);
    }
}
