package com.payment.worker;

import com.payment.entity.OutboxEntity;
import com.payment.model.OutboxStatus;
import com.payment.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    @Test
    void publishPendingEvents_shouldSendToKafkaAndUpdateStatusToProcessed() {
        // 1. Arrange (Given)
        OutboxEntity pendingEvent = new OutboxEntity(
                "PAYMENT", "KEY_123", "PAYMENT_CREATED", "{\"amount\":100}"
        );

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(pendingEvent));

        // 2. Act (When)
        outboxPublisher.publishPendingEvents();

        // 3. Assert & Verify (Then)
        // Verify kafkaTemplate.send was called with topic, key, and payload 📩
        verify(kafkaTemplate, times(1)).send("PAYMENT", "KEY_123", "{\"amount\":100}");

        // Verify entity status was updated 🟢
        assertEquals(OutboxStatus.PROCESSED, pendingEvent.getStatus());

        // Verify outboxRepository.save was called once to persist the update 💾
        verify(outboxRepository, times(1)).save(pendingEvent);
    }
}