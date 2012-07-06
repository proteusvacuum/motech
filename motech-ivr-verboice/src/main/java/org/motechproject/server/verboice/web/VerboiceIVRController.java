package org.motechproject.server.verboice.web;

import org.apache.commons.lang.StringUtils;
import org.motechproject.server.verboice.VerboiceIVRService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/verboice")
public class VerboiceIVRController {

    public static final String DECISIONTREE_URL = "/decisiontree/node";
    private VerboiceIVRService verboiceIVRService;

    @Autowired
    public VerboiceIVRController(VerboiceIVRService verboiceIVRService) {
        this.verboiceIVRService = verboiceIVRService;
    }

    @RequestMapping("/ivr")
    public String handleRequest(HttpServletRequest request) {
        final String treeName = request.getParameter("tree");
        if (StringUtils.isNotBlank(treeName)) {
            String digits = request.getParameter("DialCallStatus");
            if (StringUtils.isBlank(digits)) {
                digits = request.getParameter("Digits");
            }
            String treePath = request.getParameter("trP");
            String language = request.getParameter("ln");
            return redirectToDecisionTree(treeName, digits, treePath, language, request.getServletPath());
        }
        return verboiceIVRService.getHandler().handle(request.getParameterMap());
    }

    private String redirectToDecisionTree(String treeName, String digits, String treePath, String language, String servletPath) {
        final String transitionKey = digits == null ? "" : "&trK=" + digits;
        return String.format("forward:%s%s?type=verboice&tree=%s&trP=%s&ln=%s%s", servletPath, DECISIONTREE_URL, treeName, treePath, language, transitionKey)
                .replaceAll("//", "/");
    }

}
