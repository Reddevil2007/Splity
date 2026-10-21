package com.splitcloud;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Lets an external servlet container (Tomcat 10.1 on Azure App Service) bootstrap the
 * application from the WAR, instead of Spring Boot starting its own server.
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SplitCloudApplication.class);
    }
}
