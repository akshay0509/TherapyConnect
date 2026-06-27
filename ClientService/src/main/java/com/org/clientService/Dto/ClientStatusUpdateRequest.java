package com.org.clientService.Dto;

import com.org.events.Client.ClientStatus;

import lombok.Data;

@Data
public class ClientStatusUpdateRequest {

	private ClientStatus status;
}
