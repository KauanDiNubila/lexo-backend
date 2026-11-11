package app.lexo.application.port.in;

public interface UpdateInvoiceUseCase {

    InvoiceResponse update(UpdateInvoiceCommand command);
}
