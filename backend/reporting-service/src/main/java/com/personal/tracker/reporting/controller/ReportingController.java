package com.personal.tracker.reporting.controller;

import com.personal.tracker.common.error.ApiError;
import com.personal.tracker.reporting.domain.TaskComplexity;
import com.personal.tracker.reporting.model.ReportFilter;
import com.personal.tracker.reporting.model.ReportResponse;
import com.personal.tracker.reporting.model.ReportSortDirection;
import com.personal.tracker.reporting.model.ReportSortField;
import com.personal.tracker.reporting.model.TimeWindow;
import com.personal.tracker.reporting.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/reports", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Reporting", description = "Productivity and task completion reporting")
@SecurityRequirement(name = "bearerAuth")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/tasks")
        @Operation(
            summary = "Task report",
            description = "Generate task distribution and productivity trends for the authenticated user",
            responses = {
                @ApiResponse(responseCode = "200", description = "Report generated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ReportResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid filter values",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class)))
            }
        )
    public Mono<ReportResponse> taskReport(
            @RequestParam(name = "window", defaultValue = "MONTHLY") TimeWindow window,
            @RequestParam(name = "application", required = false) String application,
            @RequestParam(name = "complexity", required = false) TaskComplexity complexity,
            @RequestParam(name = "sortField", defaultValue = "COMPLETION_DATE") ReportSortField sortField,
            @RequestParam(name = "sortDirection", defaultValue = "DESC") ReportSortDirection sortDirection,
            Authentication authentication) {

        String userId = authentication.getName();
        ReportFilter filter = new ReportFilter(window, application, complexity, sortField, sortDirection);
        return reportingService.generateReport(userId, filter);
    }
}
