package app.lexo.common.signing;

import java.util.List;

public final class IdentityHeaders {

    public static final String H_USER_ID = "X-User-Id";
    public static final String H_ORG_ID = "X-Org-Id";
    public static final String H_ROLE = "X-User-Role";
    public static final String H_NAME = "X-User-Name";
    public static final String H_EMAIL = "X-User-Email";
    public static final String H_CLIENT_ADDRESS = "X-Client-Address";

    public static final List<String> ALL = List.of(H_USER_ID, H_ORG_ID, H_ROLE, H_NAME, H_EMAIL);

    private IdentityHeaders() {
    }
}
