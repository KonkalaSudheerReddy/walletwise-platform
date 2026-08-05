package com.walletwise.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration(proxyBeanMethods = false)
public class SpaResourceConfiguration implements WebMvcConfigurer {
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations("classpath:/static/")
        .resourceChain(true)
        .addResolver(
            new PathResourceResolver() {
              @Override
              protected Resource getResource(String resourcePath, Resource location)
                  throws IOException {
                Resource requested = location.createRelative(resourcePath);
                if (requested.exists() && requested.isReadable()) return requested;
                if (resourcePath.startsWith("api/")
                    || resourcePath.startsWith("actuator/")
                    || resourcePath.startsWith("swagger-ui/")
                    || resourcePath.startsWith("v3/api-docs")) return null;
                Resource index = location.createRelative("index.html");
                return index.exists() && index.isReadable() ? index : null;
              }
            });
  }
}
