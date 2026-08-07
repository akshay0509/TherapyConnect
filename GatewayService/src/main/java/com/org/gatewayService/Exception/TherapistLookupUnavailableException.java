package com.org.gatewayService.Exception;

/**
 * TherapistService could not be reached, so whether this user has a profile is
 * UNKNOWN — which is not the same as knowing they have none.
 *
 * The distinction matters because the two were previously identical: the
 * internal lookup answers 200 with an empty body when a user has no profile, and
 * the circuit-breaker fallback also returned null when the service was down. A
 * therapist logging in during a restart therefore got a token with no
 * therapistId claim, and the app read that as "new user" and sent them to create
 * a profile they already had.
 */
public class TherapistLookupUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TherapistLookupUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
