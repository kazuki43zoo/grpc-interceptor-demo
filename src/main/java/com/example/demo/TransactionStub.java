package com.example.demo;

import com.example.demo.proto.TransactionGrpc;
import com.example.demo.proto.TransactionReferReply;
import com.example.demo.proto.TransactionReferRequest;
import com.example.demo.proto.TransactionReply;
import com.example.demo.proto.TransactionRequest;
import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

public class TransactionStub extends TransactionGrpc.TransactionImplBase {

	private final MetadataHolder metadataHolder;

	public TransactionStub(MetadataHolder metadataHolder) {
		// リクエストヘッダを受け取る魔法の器をインジェクションしておく
		this.metadataHolder = metadataHolder;
	}

	@Override
	public void create(TransactionRequest request, StreamObserver<TransactionReply> responseObserver) {
		// 魔法の器経由でリクエストヘッダの値を取得
		String envId = metadataHolder.get(request).get(Metadata.Key.of("envId", Metadata.ASCII_STRING_MARSHALLER));
		TransactionReply reply = TransactionReply.newBuilder().setId(envId + ":" + UUID.randomUUID()).build();
		responseObserver.onNext(reply);
		responseObserver.onCompleted();
	}

	@Override
	public void refer(TransactionReferRequest request, StreamObserver<TransactionReferReply> responseObserver) {
		// 魔法の器経由でリクエストヘッダの値を取得
		String envId = metadataHolder.get(request).get(Metadata.Key.of("envId", Metadata.ASCII_STRING_MARSHALLER));
		TransactionReferReply reply = TransactionReferReply.newBuilder()
				.setId(request.getId())
				.setName(envId + ":" + "Name").setAmount(100)
				.setStatus(TransactionReferReply.Status.ACCEPT)
				.build();
		responseObserver.onNext(reply);
		responseObserver.onCompleted();
	}

}
