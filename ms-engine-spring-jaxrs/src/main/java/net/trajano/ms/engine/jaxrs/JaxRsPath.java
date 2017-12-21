package net.trajano.ms.engine.jaxrs;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class JaxRsPath implements
    Comparable<JaxRsPath> {

    /**
     * Mime types that get consumed by the path. This is expected to be empty for
     * GET requests.
     */
    private final String[] consumes;

    private final boolean exact;

    /**
     * VertX HTTP method.
     */
    private final HttpMethod method;

    private final String path;

    private final String pathRegex;

    private final String[] produces;

    public JaxRsPath(final String path,
        final String[] consumes,
        final String[] produces,
        final HttpMethod method) {

        this.path = path;
        this.consumes = consumes;
        this.produces = produces;
        this.method = method;
        final Pattern placeholderPattern = Pattern.compile("/\\{([^}]+)}");
        final Pattern regexPlaceholderPattern = Pattern.compile("[-A-Za-z_0-9]+:\\s*(.+)");
        final Matcher matcher = placeholderPattern.matcher(path);

        final StringBuffer b = new StringBuffer();
        while (matcher.find()) {
            final Matcher m2 = regexPlaceholderPattern.matcher(matcher.group(1));
            if (m2.matches()) {
                matcher.appendReplacement(b, "/" + m2.group(1));
            } else {
                matcher.appendReplacement(b, "/[^/]+");
            }
        }
        matcher.appendTail(b);
        pathRegex = b.toString();
        exact = pathRegex.equals(path);
    }

    /**
     * Apply path to router and assign the appropriate handlers.
     *
     * @param router
     *            router
     * @param jaxRsHandler
     *            JAX-RS Handler
     * @param failureHandler
     *            failure handler
     */
    public void apply(final Router router,
        final Handler<RoutingContext> jaxRsHandler,
        final Handler<RoutingContext> failureHandler) {

        if (isGet()) {
            if (isExact()) {
                router.head(getPath());
            } else {
                router.headWithRegex(getPathRegex());
            }
        }

        Route route;
        if (isExact()) {
            route = router.route(getMethod(), getPath());
        } else {
            route = router.routeWithRegex(getMethod(), getPathRegex());
        }

        for (final String mimeType : consumes) {
            route = route.consumes(mimeType);
        }
        for (final String mimeType : produces) {
            route = route.produces(mimeType);
        }
        route.handler(jaxRsHandler).failureHandler(failureHandler);
    }

    /**
     * Compares two JaxRsPath objects such that it is ordered by most specific and
     * lowest level first. It sorts it in reverse by path. A path with produces is
     * order before one than one that does not. {@inheritDoc}
     */
    @Override
    public int compareTo(final JaxRsPath o) {

        if (exact && !o.exact) {
            return -1;
        } else if (!exact && o.exact) {
            return 1;
        }

        final int c = o.path.compareTo(path);
        if (c != 0) {
            return c;
        }

        if (isNoProduces() && !o.isNoProduces()) {
            return 1;
        } else if (!isNoProduces() && o.isNoProduces()) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JaxRsPath other = (JaxRsPath) obj;
        if (!Arrays.equals(consumes, other.consumes)) {
            return false;
        }
        if (method != other.method) {
            return false;
        }
        if (!Objects.equals(path, other.path)) {
            return false;
        }
        return Arrays.equals(produces, other.produces);
    }

    public String[] getConsumes() {

        return consumes;
    }

    /**
     * Gets the VertX HTTP method.
     *
     * @return VertX HTTP method
     */
    public HttpMethod getMethod() {

        return method;
    }

    /**
     * Gets the path that is specified in the JAX-RS classes.
     *
     * @return path
     */
    public String getPath() {

        return path;
    }

    /**
     * Gets the path that is suitable for the router.
     *
     * @return path regular expression
     */
    public String getPathRegex() {

        return pathRegex;
    }

    public String[] getProduces() {

        return produces;
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(consumes);
        result = prime * result + (method == null ? 0 : method.hashCode());
        result = prime * result + (path == null ? 0 : path.hashCode());
        result = prime * result + Arrays.hashCode(produces);
        return result;
    }

    /**
     * Checks if the path is an exact one (i.e. no regex)
     *
     * @return exact path indicator
     */
    public boolean isExact() {

        return exact;
    }

    public boolean isGet() {

        return method == HttpMethod.GET;
    }

    public boolean isNoProduces() {

        return produces.length == 0;
    }

    @Override
    public String toString() {

        return "JaxRsPath [consumes=" + Arrays.toString(consumes) + ", exact=" + exact + ", method=" + method + ", path=" + path + ", pathRegex=" + pathRegex + ", produces=" + Arrays.toString(produces) + "]";
    }

}
