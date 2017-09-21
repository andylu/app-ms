package net.trajano.ms.engine.sample;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import net.trajano.ms.engine.JaxRsRoute;

public class Main {

    public static void main(final String[] args) {

        final VertxOptions options = new VertxOptions();
        Vertx.clusteredVertx(options, event -> {
            final Vertx vertx = event.result();

            final Router router = Router.router(vertx);
            final HttpServer http = vertx.createHttpServer();

            JaxRsRoute.route(vertx, router, MyApp.class);
            http.requestHandler(req -> router.accept(req)).listen(8280);
        });

        //final Vertx vertx = Vertx.vertx();

    }
}