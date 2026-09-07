package com.payment.worker;

import com.payment.entity.OutboxEntity;
import com.payment.model.OutboxStatus;
import com.payment.repository.OutboxRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;

    public OutboxPublisher(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    // ⏱️ Runs every 5 seconds (5000 milliseconds)
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        // 1. Fetch PENDING events (Microsecond fast via Partial Index! 🔍)
        List<OutboxEntity> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (OutboxEntity event : pendingEvents) {
            // 2. Simulate sending payload to Kafka 📩
            System.out.println("Publishing to Kafka topic [" + event.getAggregateType() + "]: " + event.getPayload());

            // 3. Update status so it won't be re-fetched 🟢
            event.setStatus(OutboxStatus.PROCESSED);
            outboxRepository.save(event);
        }
    }
}