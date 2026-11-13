package app.lexo.adapter.in.web;

import app.lexo.domain.Deadline;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

record DeadlineRequest(
        @Size(max = 200) @JsonProperty("titulo") String title,
        @Size(max = 40) @JsonProperty("data") String date,
        @Size(max = 40) @JsonProperty("status") String status
) {

    Deadline toDomain() {
        return new Deadline(title, date, status);
    }
}
