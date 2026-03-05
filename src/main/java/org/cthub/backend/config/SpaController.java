package org.cthub.backend.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    // Diese Regex fängt alles ab, was KEINEN Punkt im Pfad hat.
    // 1. Echte API-Endpunkte (z.B. @GetMapping("/api/users")) haben Vorrang.
    // 2. Dateien (z.B. /assets/style.css) werden ignoriert (wegen des Punkts).
    // 3. Alles andere (z.B. /dashboard, /login) landet hier -> index.html.
    @RequestMapping(value = "/**/{path:[^\\.]*}")
    public String forward() {
        return "forward:/index.html";
    }
}