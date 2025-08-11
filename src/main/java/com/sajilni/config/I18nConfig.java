package com.sajilni.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class I18nConfig {
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setCacheSeconds(3600);            // reload interval in dev; tune for prod
        ms.setFallbackToSystemLocale(false);
        return ms;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver lr = new AcceptHeaderLocaleResolver();
        lr.setDefaultLocale(Locale.ENGLISH);
        return lr;
    }
}
