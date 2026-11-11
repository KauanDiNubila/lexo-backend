package app.lexo.application.port.in;

public interface ChangeInvoiceStatusUseCase {

    InvoiceResponse changeStatus(ChangeInvoiceStatusCommand command);
}
