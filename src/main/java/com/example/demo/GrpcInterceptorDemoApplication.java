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

	@Bean
	public ThreadLocal<Metadata> requestHeaderHolder() {
		return ThreadLocal.withInitial(Metadata::new);
	}

	@Bean
	ServerInterceptor serverHeadersInterceptor(ThreadLocal<Metadata> requestHeaderHolder) {
		return new ServerInterceptor() {
			@Override
			public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
					Metadata headers, ServerCallHandler<ReqT, RespT> next) {
				requestHeaderHolder.set(headers); // リクエストされたヘッダ情報をスレッドローカルへ設定
				return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
						next.startCall(call, headers)) {
					@Override
					public void onComplete() {
						runWithClearHolder(super::onComplete); // サーバ処理の終了時にスレッドローカルをお掃除
					}

					@Override
					public void onCancel() {
						runWithClearHolder(super::onCancel); // サーバ処理の終了時にスレッドローカルをお掃除
					}

					private void runWithClearHolder(Runnable runnable) {
						try {
							runnable.run();
						}
						finally {
							requestHeaderHolder.remove();
						}
					}
				};
			}
		};
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public GrpcServer grpcServer(ServerInterceptor serverHeadersInterceptor,
			ThreadLocal<Metadata> requestHeaderHolder) {
		return new GrpcServer(new TransactionStub(requestHeaderHolder))
				.withInterceptors(serverHeadersInterceptor); // サーバ向けのインタセプタを設定
	}

	@Bean
	ManagedChannel grpcManagedChannel() {
		return ManagedChannelBuilder
				.forAddress("localhost", 50051)
				.usePlaintext()
				.build();
	}

	@Bean
	ClientInterceptor clientHeadersInterceptor(Environment environment) {
		// 任意のヘッダ値を設定してくれるインタセプタを定義
		Metadata headers = new Metadata();
		headers.put(Metadata.Key.of("envId", Metadata.ASCII_STRING_MARSHALLER),
				environment.getProperty("envId", "dev"));
		return MetadataUtils.newAttachHeadersInterceptor(headers);
	}

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
