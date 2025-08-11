package com.adp.benefits.carrier.config.app;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Schedulers;

@Configuration
@Slf4j
public class ReactorMdcConfig {

    @PostConstruct
    public void init() {
        // Use the correct method for context tracking in your version of Reactor
        Hooks.enableContextLossTracking();

        Schedulers.onScheduleHook(
                "mdc",
                runnable -> {
                    Map<String, String> contextMap = MDC.getCopyOfContextMap();
                    return () -> {
                        Map<String, String> oldContext = MDC.getCopyOfContextMap();
                        if (contextMap != null) {
                            MDC.setContextMap(contextMap);
                        }
                        try {
                            runnable.run();
                        } finally {
                            if (oldContext != null) {
                                MDC.setContextMap(oldContext);
                            } else {
                                MDC.clear();
                            }
                        }
                    };
                });
    }
}
