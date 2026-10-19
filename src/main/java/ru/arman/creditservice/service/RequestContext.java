package ru.arman.creditservice.service;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class RequestContext {
    private static final String MDC_KEY = "correlationId"; // ваш ключ

    public String getCorrelationId() {
        String id = MDC.get(MDC_KEY);
        return id != null ? id : "UNKNOWN";
    }
}