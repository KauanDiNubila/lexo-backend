package app.lexo.application.port.in;

public interface SummarizeCaseUseCase {

    SummaryResponse summarize(SummarizeCaseCommand command);
}
