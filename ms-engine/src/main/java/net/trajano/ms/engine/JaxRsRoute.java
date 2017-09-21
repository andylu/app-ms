package net.trajano.ms.engine;

import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import net.trajano.ms.engine.internal.VertxBinder;
import net.trajano.ms.engine.internal.VertxBlockingInputStream;
import net.trajano.ms.engine.internal.VertxBufferInputStream;
import net.trajano.ms.engine.internal.VertxSecurityContext;
import net.trajano.ms.engine.internal.VertxWebResponseWriter;

public class JaxRsRoute implements
    Handler<RoutingContext> {

    /**
     * Constructs a new route for the given router to a JAX-RS application.
     *
     * @param router
     * @param applicationClass
     */
    public static void route(final Vertx vertx,
        final Router router,
        final Class<? extends Application> applicationClass) {

        new JaxRsRoute(vertx, router, applicationClass);

    }

    private volatile ApplicationHandler appHandler;

    private final URI baseUri;

    private final Vertx vertx;

    private JaxRsRoute(final Vertx vertx,
        final Router router,
        final Class<? extends Application> applicationClass) {

        this.vertx = vertx;
        final ResourceConfig resourceConfig = ResourceConfig.forApplicationClass(applicationClass);
        resourceConfig.register(new VertxBinder(vertx));
        resourceConfig.register(JacksonJaxbJsonProvider.class);

        final String resourcePackage = applicationClass.getPackage().getName();
        resourceConfig.addProperties(singletonMap(ServerProperties.PROVIDER_PACKAGES, resourcePackage));
        resourceConfig.addProperties(singletonMap(ServerProperties.TRACING, "ALL"));

        final ApplicationPath annotation = applicationClass.getAnnotation(ApplicationPath.class);
        if (annotation != null) {
            baseUri = URI.create(annotation.value() + "/").normalize();
        } else {
            baseUri = URI.create("/");
        }

        final BeanConfig beanConfig = new BeanConfig();
        beanConfig.setResourcePackage(resourcePackage);
        beanConfig.setScan(true);
        beanConfig.setBasePath(baseUri.getPath());
        beanConfig.scanAndRead();

        try {
            final Swagger swagger = beanConfig.getSwagger();
            final String json = Json.mapper().writeValueAsString(swagger);
            final String yaml = Yaml.mapper().writeValueAsString(swagger);
            router.get(baseUri.getPath()).produces("application/json").handler(context -> context.response().putHeader("Content-Type", "application/json").end(json));
            router.get(baseUri.getPath()).produces("application/yaml").handler(context -> context.response().putHeader("Content-Type", "application/yaml").end(yaml));
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        appHandler = new ApplicationHandler(resourceConfig);
        router.route(baseUri.getPath() + "*").handler(this);
    }

    @Override
    public void handle(final RoutingContext context) {

        final HttpServerRequest event = context.request();
        final URI requestUri = URI.create(event.absoluteURI());

        final ContainerRequest request = new ContainerRequest(baseUri, requestUri, event.method().name(), new VertxSecurityContext(event), new MapPropertiesDelegate());

        event.headers().entries().forEach(entry -> request.getHeaders().add(entry.getKey(), entry.getValue()));
        request.setWriter(new VertxWebResponseWriter(event.response()));

        final String contentLengthString = event.getHeader("Content-Length");
        final int contentLength;
        if (contentLengthString != null) {
            contentLength = Integer.parseInt(contentLengthString);
        } else {
            contentLength = 0;
        }

        if (contentLength == 0) {
            final Buffer body = Buffer.buffer();
            event
                .endHandler(aVoid -> {
                    request.setEntityStream(new VertxBufferInputStream(body));
                    appHandler.handle(request);
                });
        } else if (contentLength < 1024) {
            event
                .bodyHandler(body -> {
                    request.setEntityStream(new VertxBufferInputStream(body));
                    appHandler.handle(request);
                });
        } else {
            try (final VertxBlockingInputStream is = new VertxBlockingInputStream(event)) {
                event
                    .handler(buffer -> is.populate(buffer))
                    .endHandler(aVoid -> is.end());
                vertx.executeBlocking(future -> {
                    request.setEntityStream(is);
                    appHandler.handle(request);
                    future.complete();
                }, false, result -> {
                });
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

}
