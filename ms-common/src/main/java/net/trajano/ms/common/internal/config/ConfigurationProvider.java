package net.trajano.ms.common.internal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.ProxyOptions;

@Configuration
public class ConfigurationProvider {

    @Value("${http.client.proxy:}")
    private String httpClientProxy;

    @Value("${http.port:8900}")
    private int httpPort;

    @Value("${vertx.warningExceptionTime:1}")
    private long vertxWarningExceptionTime;

    @Value("${vertx.workerPoolSize:50}")
    private int vertxWorkerPoolSize;

    @Bean
    public HttpClientOptions httpClientOptions() {

        final HttpClientOptions options = new HttpClientOptions();
        final ProxyOptions proxyOptions = new ProxyOptions();
        proxyOptions.setUsername(httpClientProxyUsername);
        options.setProxyOptions(proxyOptions);
        return options;
    }

    @Bean
    public HttpServerOptions httpServerOptions() {

        final HttpServerOptions options = new HttpServerOptions();
        options.setPort(httpPort);
        return options;
    }

    @Bean
    public VertxOptions vertxOptions() {

        final VertxOptions options = new VertxOptions();
        options.setWarningExceptionTime(vertxWarningExceptionTime);
        options.setWorkerPoolSize(vertxWorkerPoolSize);

        return options;
    }

}
