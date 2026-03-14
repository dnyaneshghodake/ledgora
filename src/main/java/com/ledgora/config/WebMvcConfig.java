package com.ledgora.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final BusinessDayInterceptor businessDayInterceptor;
    private final PendingApprovalsInterceptor pendingApprovalsInterceptor;

    public WebMvcConfig(
            BusinessDayInterceptor businessDayInterceptor,
            PendingApprovalsInterceptor pendingApprovalsInterceptor) {
        this.businessDayInterceptor = businessDayInterceptor;
        this.pendingApprovalsInterceptor = pendingApprovalsInterceptor;
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.jsp("/WEB-INF/jsp/", ".jsp");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**").addResourceLocations("/resources/");
        registry.addResourceHandler("/css/**").addResourceLocations("/resources/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("/resources/js/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(businessDayInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/resources/**", "/login", "/logout");
        registry.addInterceptor(pendingApprovalsInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**", "/js/**", "/resources/**", "/login", "/logout", "/api/**");
    }
}
