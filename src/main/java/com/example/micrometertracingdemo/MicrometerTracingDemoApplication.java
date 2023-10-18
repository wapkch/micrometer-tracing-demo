package com.example.micrometertracingdemo;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.contextpropagation.ObservationAwareSpanThreadLocalAccessor;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class MicrometerTracingDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicrometerTracingDemoApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(ObservationRegistry observationRegistry, Tracer tracer) {
        return args -> {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setTaskDecorator(runnable ->
                ContextSnapshotFactory.builder().build().captureAll().wrap(runnable));
            executor.initialize();

            ScopedSpan scopedSpan = tracer.startScopedSpan("test");
            try (BaggageInScope baggageInScope = tracer.getBaggage("tenant").makeCurrent("tenant")) {
                executor.submit(() -> {
                    String tenant = tracer.getBaggage("tenant").get();
                    // tenant should not be null
                    System.out.println(tenant);
                });
            } finally {
                scopedSpan.end();
            }
        };
    }

}

@Component
class Registrar implements SmartInitializingSingleton {

    @Autowired
    ObservationRegistry observationRegistry;

    @Autowired
    Tracer tracer;

    @Override
    public void afterSingletonsInstantiated() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(
            new ObservationAwareSpanThreadLocalAccessor(observationRegistry, tracer));
    }
}