package com.lpu.authservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BinaryFileDTO {
    private byte[] data;
    private String contentType;
    private String fileName;
}
