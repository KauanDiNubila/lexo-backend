package app.lexo.adapter.out.persistence;

import app.lexo.domain.InvoiceStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class InvoiceStatusConverter implements AttributeConverter<InvoiceStatus, String> {

    @Override
    public String convertToDatabaseColumn(InvoiceStatus status) {
        return status == null ? null : status.code();
    }

    @Override
    public InvoiceStatus convertToEntityAttribute(String code) {
        return code == null ? null : InvoiceStatus.fromCode(code);
    }
}
