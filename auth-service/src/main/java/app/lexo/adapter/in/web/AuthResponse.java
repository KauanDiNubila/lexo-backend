package app.lexo.adapter.in.web;

import app.lexo.application.port.in.SessionResponse;
import app.lexo.application.port.in.UserInfo;

record AuthResponse(String accessToken, UserInfo user) {

    static AuthResponse of(SessionResponse session) {
        return new AuthResponse(session.accessToken(), session.user());
    }
}
