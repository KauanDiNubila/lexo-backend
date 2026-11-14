package app.lexo.adapter.out.persistence;

import app.lexo.domain.CaseStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class CaseStatusConverter implements AttributeConverter<CaseStatus, String> {

    @Override
    public String convertToDatabaseColumn(CaseStatus value) {
        return value == null ? null : value.code();
    }

    @Override
    public CaseStatus convertToEntityAttribute(String code) {
        return code == null ? null : CaseStatus.fromCode(code);
    }
}
