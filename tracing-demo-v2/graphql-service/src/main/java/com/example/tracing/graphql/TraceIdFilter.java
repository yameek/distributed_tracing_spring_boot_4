package com.example.tracing.graphql;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private final Tracer tracer;

    public TraceIdFilter(@Nullable Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String traceId = getTraceId();
            String spanId = getSpanId();
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }
            if (spanId != null) {
                MDC.put("spanId", spanId);
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private @Nullable String getTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            var span = tracer.currentSpan();
            var traceContext = span.context();
            return traceContext != null ? traceContext.traceId() : null;
        }
        return null;
    }

    private @Nullable String getSpanId() {
        if (tracer != null && tracer.currentSpan() != null) {
            var span = tracer.currentSpan();
            var traceContext = span.context();
            return traceContext != null ? traceContext.spanId() : null;
        }
        return null;
    }
}
