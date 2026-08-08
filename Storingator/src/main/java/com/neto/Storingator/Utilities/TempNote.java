package com.neto.Storingator.Utilities;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class TempNote {
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 4096, message = "Note content must not exceed 4096 characters")
    private String content;

    private UUID seq;
    private Boolean encryptOnDevice;
    private String timestamp;
}