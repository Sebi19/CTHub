package org.fllhub.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Component
public class SpaRedirectController implements ErrorViewResolver {

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        // 1. Check if it's a 404 (Page not found)
        if (status == HttpStatus.NOT_FOUND) {
            String path = request.getRequestURI();

            // 2. If it is NOT an API call and NOT a file (like image.png or style.css)
            //    Then it must be a React Route -> Forward to index.html
            boolean isApi = path.startsWith("/api");
            boolean isFile = path.lastIndexOf('.') > path.lastIndexOf('/'); // Simple check for extensions

            if (!isApi && !isFile) {
                return new ModelAndView("forward:/index.html");
            }
        }
        return null; // Otherwise let normal error handling happen (JSON or White Label)
    }
}