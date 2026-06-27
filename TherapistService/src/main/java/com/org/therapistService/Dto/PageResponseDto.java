package com.org.therapistService.Dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageResponseDto<T> {

	private List<T> content;
	private int totalPages;
	private long totalElements;
	private int page;
	private int size;
}
