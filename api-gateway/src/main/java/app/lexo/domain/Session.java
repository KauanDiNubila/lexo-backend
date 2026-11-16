package app.lexo.domain;

import java.util.OptionalLong;

public record Session(Identity identity, OptionalLong issuedAtSeconds) {

    public boolean isEndedBy(long cutoffSeconds) {
        return issuedAtSeconds.isEmpty() || issuedAtSeconds.getAsLong() <= cutoffSeconds;
    }
}
