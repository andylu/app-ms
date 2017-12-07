package net.trajano.ms.auth.spi;

import java.net.URI;

import net.trajano.ms.auth.util.HttpAuthorizationHeaders;
import net.trajano.ms.core.ErrorCodes;
import net.trajano.ms.core.ErrorResponses;

/**
 * This is used to validate the client ID and secret.
 *
 * @author Archimedes Trajano
 */
public interface ClientValidator {

    /**
     * Gets the JWKS URI associated with the client ID.
     *
     * @param clientId
     *            client ID
     * @return JWKS URI
     */
    default URI getJwksUri(final String clientId) {

        return null;
    }

    default boolean isValid(final String grantType,
        final String authorization) {

        final String[] authInfo = HttpAuthorizationHeaders.parseBasicAuthorization(authorization);
        return isValid(grantType, authInfo[0], authInfo[1]);
    }

    /**
     * Obtains the client redirect URI from the Authorization header.
     * 
     * @param authorization
     *            authorization header value
     * @return redirect URI
     */
    default URI getRedirectUriFromAuthorization(final String authorization) {

        final String[] authInfo = HttpAuthorizationHeaders.parseBasicAuthorization(authorization);
        if (!isValid(null, authInfo[0], authInfo[1])) {
            throw ErrorResponses.unauthorized(ErrorCodes.UNAUTHORIZED_CLIENT, "Unauthorized client", "Basic");
        }
        return getRedirectUri(authInfo[0]);
    }

    default boolean isOriginAllowed(final String clientId,
        String origin) {

        if (origin == null) {
            return false;
        }
        return isOriginAllowed(clientId, URI.create(origin));
    }

    boolean isOriginAllowed(String clientId,
        URI origin);

    URI getRedirectUri(String clientId);

    boolean isValid(String grantType,
        String clientId,
        String clientSecret);

    /**
     * Checks if the given origin URI is allowed for the given Authorization header
     * 
     * @param originUri
     *            origin URI
     * @param authorization
     *            authorization header value
     * @return true if it is allowed.
     */
    default boolean isOriginAllowedFromAuthorization(final URI originUri,
        final String authorization) {

        final String[] authInfo = HttpAuthorizationHeaders.parseBasicAuthorization(authorization);
        if (!isValid(null, authInfo[0], authInfo[1])) {
            throw ErrorResponses.unauthorized(ErrorCodes.UNAUTHORIZED_CLIENT, "Unauthorized client", "Basic");
        }
        return isOriginAllowed(authInfo[0], originUri);
    }
}
