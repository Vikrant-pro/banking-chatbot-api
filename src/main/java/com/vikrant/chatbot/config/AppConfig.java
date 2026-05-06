package com.vikrant.chatbot.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.vikrant.chatbot.middleware.RateLimiterFilter;
import com.vikrant.chatbot.middleware.AuditLogFilter;
import com.vikrant.chatbot.repository.AuditLogRepository;

@Configuration
public class AppConfig {

    @Bean
    public FilterRegistrationBean<RateLimiterFilter> rateLimiterFilter() {
        FilterRegistrationBean<RateLimiterFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RateLimiterFilter());
        registrationBean.addUrlPatterns("/api/chat/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<AuditLogFilter> auditLogFilter(AuditLogRepository auditLogRepository) {
        FilterRegistrationBean<AuditLogFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new AuditLogFilter(auditLogRepository));
        registrationBean.addUrlPatterns("/api/chat/*");
        registrationBean.setOrder(2);
        return registrationBean;
    }
}
