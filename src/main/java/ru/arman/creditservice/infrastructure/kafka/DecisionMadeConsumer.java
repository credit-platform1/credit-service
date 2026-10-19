package ru.arman.creditservice.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.arman.creditservice.domain.dto.ChangeApplicationStatusRequest;
import ru.arman.creditservice.domain.event.DecisionMadeEvent;
import ru.arman.creditservice.domain.model.ApplicationStatus;
import ru.arman.creditservice.infrastructure.metrics.CreditMetrics;
import ru.arman.creditservice.service.CreditApplicationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionMadeConsumer {

    private final CreditApplicationService applicationService;
    private final CreditMetrics metrics;

    @KafkaListener(
            topics = "${topics.decision-made}",
            groupId = "credit-service"
    )
    public void handle(DecisionMadeEvent event) {
        log.info(
                "Received decision for application {}: {}",
                event.applicationId(),
                event.decision()
        );

        var targetStatus = switch (event.decision()) {
            case APPROVED -> ApplicationStatus.APPROVED;
            case DECLINED -> ApplicationStatus.DECLINED;
            case MANUAL_REVIEW -> ApplicationStatus.MANUAL_REVIEW;
            default -> throw new IllegalArgumentException(
                    "Unsupported decision status: " + event.decision()
            );
        };

        applicationService.changeStatus(
                event.applicationId(),
                new ChangeApplicationStatusRequest(
                        targetStatus,
                        String.join(",", event.reasonCodes()),
                        "SCORING_SERVICE"),
                event.correlationId()
        );
    }
}