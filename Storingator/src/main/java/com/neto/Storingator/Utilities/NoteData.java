package com.neto.Storingator.Utilities;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NoteData {
    @NotNull(message = "Note is required")
    @Valid
    private TempNote tmpNote;

    private String passphrase;
}