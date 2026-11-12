package app.lexo.adapter.out.persistence;

import app.lexo.domain.DeadlineStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class DeadlineStatusConverter implements AttributeConverter<DeadlineStatus, String> {

    @Override
    public String convertToDatabaseColumn(DeadlineStatus value) {
        return value == null ? null : value.code();
    }

    @Override
    public DeadlineStatus convertToEntityAttribute(String code) {
        return code == null ? null : DeadlineStatus.fromCode(code);
    }
}
