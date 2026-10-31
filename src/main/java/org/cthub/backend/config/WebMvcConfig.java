package org.cthub.backend.config;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.auth.stepup.StepUpInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ProfileCompletionInterceptor profileCompletionInterceptor;
    private final StepUpInterceptor stepUpInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(profileCompletionInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(stepUpInterceptor).addPathPatterns("/api/**");
    }
}
