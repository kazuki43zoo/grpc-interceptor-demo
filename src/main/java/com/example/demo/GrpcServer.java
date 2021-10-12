package com.example.demo;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class GrpcServer {

	private static final int DEFAULT_PORT = 50051;

	private final BindableService[] services;

	private ServerInterceptor[] serverInterceptors;

	private Server server;

	public GrpcServer(BindableService... services) {
		this.services = services;
	}

	public GrpcServer withInterceptors(ServerInterceptor... serverInterceptors) {
		this.serverInterceptors = serverInterceptors;
		return this;
	}

	public void start() {
		ServerBuilder<?> builder = ServerBuilder.forPort(DEFAULT_PORT);
		Stream.of(services)
				.forEach(service -> builder.addService(
						ServerInterceptors.intercept(service, serverInterceptors))); // サーバ向けのインタセプタを適用したサービスを追加
		try {
			server = builder.build().start();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(GrpcServer.this::stop));
	}

	public void stop() {
		if (server != null) {
			try {
				server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				e.printStackTrace(System.err);
			}
			server = null;
		}
	}

}
