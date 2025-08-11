package com.adp.benefits.carrier.service;

import com.adp.benefits.carrier.entity.client.Message;
import com.adp.benefits.carrier.entity.client.repository.MessagesRepository;
import com.adp.benefits.carrier.exceptions.DatabaseServiceOperationException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientMessageService {

    private final MessagesRepository messagesRepository;

    public Message saveMessage(String outcomeId, JsonNode payLoad) {
        try {
            Message message =
                    Message.builder()
                            .messageId(UUID.randomUUID().toString())
                            .processStepOutcomeId(outcomeId)
                            .message(payLoad)
                            .createDt(Date.from(Instant.now()))
                            .modifyDt(Date.from(Instant.now()))
                            .build();

            return messagesRepository.save(message);
        } catch (Exception e) {
            log.error(
                    "Database error while saving message for outcomeId={}: {}",
                    outcomeId,
                    e.getMessage(),
                    e);
            throw new DatabaseServiceOperationException("Failed to save message", e);
        }
    }

    public List<Message> getMessagesByOutcomeId(String outcomeId) {
        try {
            return messagesRepository.findByProcessStepOutcomeId(outcomeId);
        } catch (Exception e) {
            log.error(
                    "Database error while retrieving messages for outcomeId={}: {}",
                    outcomeId,
                    e.getMessage(),
                    e);
            throw new DatabaseServiceOperationException("Failed to retrieve messages", e);
        }
    }
}
