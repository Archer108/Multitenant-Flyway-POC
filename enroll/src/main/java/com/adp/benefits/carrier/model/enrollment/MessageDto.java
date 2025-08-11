package com.adp.benefits.carrier.model.enrollment;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class MessageDto {
    private String message;
    private List<ErrorDetailDto> errorDetails;
}
