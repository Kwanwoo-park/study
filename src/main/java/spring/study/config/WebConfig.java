package spring.study.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String resourcePath;
    private final String uploadPath;

    public WebConfig(@Value("${resource.path}") String resourcePath, @Value("${upload.path}") String uploadPath) {
        this.resourcePath = resourcePath;
        this.uploadPath = uploadPath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(uploadPath).addResourceLocations(resourcePath);
    }
}
