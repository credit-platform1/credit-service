package ru.arman.creditservice.domain.dto;

import java.util.List;

public record StatusHistoryResponse(
        String applicationId,
        List<StatusHistoryItem> items
) {
}
