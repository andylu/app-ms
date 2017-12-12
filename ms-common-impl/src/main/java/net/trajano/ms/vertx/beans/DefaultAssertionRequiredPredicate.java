package net.trajano.ms.vertx.beans;

import java.lang.reflect.Method;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.ResourceInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks if the URI has any JSR-250 annotations that would indicate a secured
 * resource. On a method level if PermitAll is present then it will allow access
 * without any assertion even if RolesAllowed is present on the class level.
 * <p>
 * However, the default is to require the assertion if nothing has been
 * specified.
 * <p>
 * This will throw an IllegalArgumentException if both {@link RolesAllowed} and
 * {@link PermitAll} are present in a class or method.
 *
 * @author Archimedes Trajano
 */
public class DefaultAssertionRequiredPredicate implements
    JwtAssertionRequiredPredicate {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAssertionRequiredPredicate.class);

    /**
     * <p>
     * The key logic for the test is as follows:
     * </p>
     * <p>
     * Let:
     * </p>
     * <table>
     * <tr>
     * <td>a =</td>
     * <td>resourceMethodHasRolesAllowed</td>
     * </tr>
     * <tr>
     * <td>b =</td>
     * <td>resourceClassHasRolesAllowed</td>
     * </tr>
     * <tr>
     * <td>c =</td>
     * <td>resourceMethodHasPermitAll</td>
     * </tr>
     * <tr>
     * <td>d =</td>
     * <td>resourceClassHasPermitAll</td>
     * </tr>
     * </table>
     * <p>
     * The rules that need to be applied translate to:
     * </p>
     *
     * <pre>
     * = a || ( b && !c ) || ( !a && !b && !c && !d )
     * = (a || b || !d) && ( a || !c )
     * </pre>
     *
     * @param resourceInfo
     *            resource info
     * @return <code>true</code> if the resource is protected
     */
    @Override
    public boolean test(final ResourceInfo resourceInfo) {

        final Method resourceMethod = resourceInfo.getResourceMethod();
        final Class<?> resourceClass = resourceInfo.getResourceClass();

        final boolean resourceMethodHasRolesAllowed = resourceMethod.getAnnotation(RolesAllowed.class) != null;
        final boolean resourceClassHasRolesAllowed = resourceClass.getAnnotation(RolesAllowed.class) != null;

        final boolean resourceMethodHasPermitAll = resourceMethod.getAnnotation(PermitAll.class) != null;
        final boolean resourceClassHasPermitAll = resourceClass.getAnnotation(PermitAll.class) != null;

        if (LOG.isDebugEnabled()) {
            LOG.debug("resourceMethod={} PermitAll={} RolesAllowed={}", resourceMethod, resourceMethodHasPermitAll, resourceMethodHasRolesAllowed);
            LOG.debug("resourceClass={} PermitAll={} RolesAllowed={}", resourceClass, resourceClassHasPermitAll, resourceClassHasRolesAllowed);
        }

        if (resourceMethodHasRolesAllowed && resourceMethodHasPermitAll) {
            throw new IllegalArgumentException("The resource method " + resourceMethod + " may not have both @RolesAllowed and @PermitAll annotations.");
        } else if (resourceClassHasRolesAllowed && resourceClassHasPermitAll) {
            throw new IllegalArgumentException("The resource class " + resourceClass + " may not have both @RolesAllowed and @PermitAll annotations.");
        } else {
            return (resourceMethodHasRolesAllowed || resourceClassHasRolesAllowed || !resourceClassHasPermitAll) && (resourceMethodHasRolesAllowed || !resourceMethodHasPermitAll);
        }

    }

}
