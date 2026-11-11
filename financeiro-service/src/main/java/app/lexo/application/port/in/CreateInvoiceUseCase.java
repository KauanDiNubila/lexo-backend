package app.lexo.application.port.in;

public interface CreateInvoiceUseCase {

    InvoiceResponse create(CreateInvoiceCommand command);
}
