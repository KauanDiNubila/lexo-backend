package app.lexo.adapter.out.persistence;

import app.lexo.domain.DeadlineType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class DeadlineTypeConverter implements AttributeConverter<DeadlineType, String> {

    @Override
    public String convertToDatabaseColumn(DeadlineType value) {
        return value == null ? null : value.code();
    }

    @Override
    public DeadlineType convertToEntityAttribute(String code) {
        return code == null ? null : DeadlineType.fromCode(code);
    }
}
