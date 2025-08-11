package com.adp.benefits.carrier.model.enrollment;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.*;

/**
 * Cursor-based page envelope for the Enrollment Info API.
 * Carries the current page data and the opaque cursor for the next page.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrollmentInfoPageEnvelope {
    private List<String> clients;          // All client names (for UI filters)
    private List<String> carriers;         // Union of carriers in this page
    private Timeframe duration;            // Timeframe echo
    private List<PsoDataDto> psoData;      // The page rows
    private String nextCursor;             // Opaque Base64 cursor to request the next page
    private boolean hasMore;               // Whether more pages likely exist
    private int pageSize;                  // Requested page size
}