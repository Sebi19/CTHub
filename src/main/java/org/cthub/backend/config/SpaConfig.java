package org.cthub.backend.config;

import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@Configuration
public class SpaConfig implements ErrorViewResolver {

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        String path = (String) model.get("path");

        // Check if it's exactly "/api" OR starts with "/api/"
        boolean isApiRoute = path.equals("/api") || path.startsWith("/api/");

        // 1. We only care about 404 Not Found errors (files/routes that don't exist).
        // 2. We explicitly ignore anything starting with "/api" so backend errors remain normal 404s.
        if (status == HttpStatus.NOT_FOUND && !isApiRoute) {

            // Forward the request to React's index.html and return a 200 OK status.
            return new ModelAndView("forward:/index.html", Map.of(), HttpStatus.OK);
        }

        // Return null for all other cases (e.g., 500 errors, or 404s inside /api).
        // This tells Spring to handle those errors using its default behavior.
        return null;
    }
}