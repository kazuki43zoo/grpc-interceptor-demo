package com.example.demo;

import com.example.demo.proto.TransactionGrpc;
import com.example.demo.proto.TransactionReferReply;
import com.example.demo.proto.TransactionReferRequest;
import com.example.demo.proto.TransactionReply;
import com.example.demo.proto.TransactionRequest;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

public class TransactionStub extends TransactionGrpc.TransactionImplBase {

	private final ThreadLocal<Metadata> requestHeaderHolder;

	public TransactionStub(ThreadLocal<Metadata> requestHeaderHolder) {
		this.requestHeaderHolder = requestHeaderHolder;
	}

	@Override
	public void create(TransactionRequest request, StreamObserver<TransactionReply> responseObserver) {
		String envId = requestHeaderHolder.get().get(Metadata.Key.of("envId", Metadata.ASCII_STRING_MARSHALLER));
		TransactionReply reply = TransactionReply.newBuilder().setId(envId + ":" + UUID.randomUUID()).build();
		responseObserver.onNext(reply);
		responseObserver.onCompleted();
	}

	@Override
	public void refer(TransactionReferRequest request, StreamObserver<TransactionReferReply> responseObserver) {
		String envId = requestHeaderHolder.get().get(Metadata.Key.of("envId", Metadata.ASCII_STRING_MARSHALLER));
		TransactionReferReply reply = TransactionReferReply.newBuilder()
				.setId(request.getId())
				.setName(envId + ":" + "Name").setAmount(100)
				.setStatus(TransactionReferReply.Status.ACCEPT)
				.build();
		responseObserver.onNext(reply);
		responseObserver.onCompleted();
	}

}
