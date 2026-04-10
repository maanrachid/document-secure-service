package se.exempel.sds.security;


public final class OrganisationContext {

    private static final ThreadLocal<OrganisationPrincipal> HOLDER = new ThreadLocal<>();

    private OrganisationContext() {}

    public static void set(OrganisationPrincipal principal) {
        HOLDER.set(principal);
    }

    /**
     * Returns the current principal.
     *
     * @throws IllegalStateException if called outside an authenticated request.
     */
    public static OrganisationPrincipal get() {
        OrganisationPrincipal principal = HOLDER.get();
        if (principal == null) {
            throw new IllegalStateException(
                    "No OrganisationPrincipal bound to the current thread – " +
                    "is this being called outside a JWT-authenticated request?");
        }
        return principal;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
