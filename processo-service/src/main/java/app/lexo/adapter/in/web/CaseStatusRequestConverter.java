package app.lexo.adapter.in.web;

import app.lexo.domain.CaseStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
class CaseStatusRequestConverter implements Converter<String, CaseStatus> {

    @Override
    public CaseStatus convert(String source) {
        return CaseStatus.fromCode(source);
    }
}
