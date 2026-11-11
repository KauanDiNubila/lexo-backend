package app.lexo.adapter.in.web;

import app.lexo.domain.InvoiceStatus;
import jakarta.validation.constraints.NotNull;

record StatusRequest(@NotNull InvoiceStatus status) {
}
