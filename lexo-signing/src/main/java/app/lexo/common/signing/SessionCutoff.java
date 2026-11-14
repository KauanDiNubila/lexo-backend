package app.lexo.common.signing;

public final class SessionCutoff {

    private static final String PREFIX = "session-cutoff:";

    private SessionCutoff() {
    }

    public static String key(String userId) {
        return PREFIX + userId;
    }
}
