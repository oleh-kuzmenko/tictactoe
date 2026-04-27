package com.ttt.common.config;

import org.slf4j.MDC;
import org.springframework.lang.NonNull;

public class MdcTaskDecorator implements org.springframework.core.task.TaskDecorator {
    
    @Override
    public @NonNull Runnable decorate(@NonNull Runnable runnable) {
        var contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (contextMap != null)
                    MDC.setContextMap(contextMap);
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
