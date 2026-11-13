package app.lexo.application.service;

import app.lexo.domain.CaseContext;

final class CaseText {

    private CaseText() {
    }

    static String lines(CaseContext context) {
        return "Número: " + orDefault(context.number(), "não informado") + "\n"
                + "Área: " + orDefault(context.area(), "não informada") + "\n"
                + "Status: " + orDefault(context.status(), "não informado") + "\n"
                + "Cliente: " + orDefault(context.client(), "não informado");
    }

    static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
