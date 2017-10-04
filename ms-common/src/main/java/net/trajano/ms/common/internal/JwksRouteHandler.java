package net.trajano.ms.common.internal;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.trajano.ms.common.JwksProvider;

/**
 * This endpoint is exposed by every microservice to provide JWKS that is used
 * by the microservice.
 *
 * @author Archimedes Trajano
 */
@Component
public class JwksRouteHandler implements
    Handler<RoutingContext> {

    /**
     * JWKS provider
     */
    @Autowired
    private JwksProvider jwksProvider;

    @Override
    public void handle(final RoutingContext context) {

        context.response().putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).end(jwksProvider.getKeySet().toPublicJWKSet().toString());

    }

    public void setJwksProvider(final JwksProvider jwksProvider) {

        this.jwksProvider = jwksProvider;
    }
}
