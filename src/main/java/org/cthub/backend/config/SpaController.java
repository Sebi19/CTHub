package org.cthub.backend.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    /**
     * Forwards all non-API and non-static-asset routes to index.html.
     * (?!api|static|assets|favicon.ico) = "Does NOT start with these strings"
     * .* = "Match everything else, including dots"
     */
    @RequestMapping(value = "{path:(?!api|static|assets|favicon\\.ico).*+}")
    public String forward() {
        return "forward:/index.html";
    }
}
