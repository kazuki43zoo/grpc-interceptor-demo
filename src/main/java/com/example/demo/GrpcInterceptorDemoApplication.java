package com.example.demo;

import com.example.demo.proto.TransactionGrpc;
import com.example.demo.proto.TransactionReferReply;
import com.example.demo.proto.TransactionReferRequest;
import com.example.demo.proto.TransactionReply;
import com.example.demo.proto.TransactionRequest;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingServerCallListener;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.stub.MetadataUtils;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.UUID;

@SpringBootApplication
public class GrpcInterceptorDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrpcInterceptorDemoApplication.class, args);
	}

	// =================================
	// gRPCサーバ関連のBean定義
	// =================================

	// リクエストヘッダの情報をAP層へ連携するための魔法の器の定義
	@Bean
	public MetadataHolder metadataHolder() {
		return new MetadataHolder();
	}

	@Bean
	ServerInterceptor serverHeadersInterceptor(MetadataHolder metadataHolder) {
		return new ServerInterceptor() {
			@Override
			public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
					Metadata headers, ServerCallHandler<ReqT, RespT> next) {
				return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
						next.startCall(call, headers)) {

					@Override public void onMessage(ReqT message) {
						metadataHolder.save(message, headers); // リクエストされたヘッダ情報を保存
						super.onMessage(message);
					}

					@Override
					public void onComplete() {
						runWithClearHolder(super::onComplete); // サーバ処理の終了時にお掃除
					}

					@Override
					public void onCancel() {
						runWithClearHolder(super::onCancel); // サーバ処理の終了時にお掃除
					}

					private void runWithClearHolder(Runnable runnable) {
						try {
							runnable.run();
						}
						finally {
							metadataHolder.clear(headers);
						}
					}
				};
			}
		};
	}

	// gRPCサーバの定義
	@Bean(initMethod = "start", destroyMethod = "stop")
	public GrpcServer grpcServer(ServerInterceptor serverHeadersInterceptor,
			MetadataHolder metadataHolder) {
		return new GrpcServer(new TransactionStub(metadataHolder))
				.withInterceptors(serverHeadersInterceptor); // サーバ向けのインタセプタを設定
	}

	// =================================
	// gRPCクライアント関連のBean定義
	// =================================

	// gRPCサーバへ接続するためのチャネルを定義
	@Bean
	ManagedChannel grpcManagedChannel() {
		return ManagedChannelBuilder
				.forAddress("localhost", 50051)
				.usePlaintext()
				.build();
	}

	// リクエストヘッダに任意の値を設定するインタセプタの定義
	@Bean
	ClientInterceptor clientHeadersInterceptor(Environment environment) {
		// 任意のヘッダ値を設定してくれるインタセプタを定義
		Metadata headers = new Metadata();
		headers.put(Metadata.Key.of("envId", Metadata.ASCII_STRING_MARSHALLER),
				environment.getProperty("envId", "dev"));
		return MetadataUtils.newAttachHeadersInterceptor(headers);
	}

	// クライアントAP側の実装
	@Bean
	public ApplicationRunner clientRunner(ManagedChannel managedChannel, ClientInterceptor clientHeadersInterceptor) {
		return args -> {
			TransactionGrpc.TransactionBlockingStub blockingStub =
					TransactionGrpc.newBlockingStub(managedChannel)
							.withInterceptors(clientHeadersInterceptor); // クライアント向けのインタセプタを設定
			{
				TransactionRequest request = TransactionRequest.newBuilder()
						.setName("Name")
						.setAmount(100)
						.setVendor("TestPay")
						.build();
				TransactionReply reply = blockingStub.create(request);
				System.out.println(reply);
			}
			{
				TransactionReferRequest request = TransactionReferRequest.newBuilder()
						.setId(UUID.randomUUID().toString())
						.build();
				TransactionReferReply reply = blockingStub.refer(request);
				System.out.println(reply);
			}
		};
	}

}
