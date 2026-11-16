package app.lexo.domain;

public record Session(Identity identity, long issuedAtSeconds) {

    public boolean isEndedBy(long cutoffSeconds) {
        return issuedAtSeconds <= cutoffSeconds;
    }
}
