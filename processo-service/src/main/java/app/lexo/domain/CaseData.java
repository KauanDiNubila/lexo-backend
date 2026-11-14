package app.lexo.domain;

public record CaseData(ClientId clientId, String number, String area, CaseStatus status, String description,
                       UserId responsibleId) {

    public CaseData {
        area = blankToNull(area);
        description = blankToNull(description);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
