package com.example.demo;

import io.grpc.Metadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetadataHolder {
	private final Map<Object, Metadata> requestMetadataMap = new ConcurrentHashMap<>();

	private final Map<Metadata, Object> metadataRequestMap = new ConcurrentHashMap<>();

	void save(Object request, Metadata metadata) {
		requestMetadataMap.put(request, metadata);
		metadataRequestMap.put(metadata, request);
	}

	Metadata get(Object request) {
		return requestMetadataMap.get(request);
	}

	void clear(Metadata metadata) {
		Object request = metadataRequestMap.remove(metadata);
		requestMetadataMap.remove(request);
	}
}
