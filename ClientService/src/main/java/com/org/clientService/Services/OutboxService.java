package com.org.clientService.Services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.clientService.Entity.OutboxEvent;
import com.org.clientService.Repository.OutboxEventRepository;

@Service
public class OutboxService {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private OutboxEventRepository outboxEventRepository;
	
	public void saveOutboxEvent(String aggregateType, String aggregateId, String eventType, Object payload) throws JsonProcessingException {
		
		OutboxEvent outboxEvent = new OutboxEvent();
		outboxEvent.setAggregateId(aggregateId);
		outboxEvent.setAggregateType(aggregateType);
		outboxEvent.setCreatedAt(LocalDateTime.now());
		outboxEvent.setEventType(eventType);
		outboxEvent.setPayload(objectMapper.valueToTree(payload));
		outboxEvent.setPublished(false);
		
		outboxEventRepository.save(outboxEvent);
	}
}
