package com.hascode.tutorial;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.apache.log4j.BasicConfigurator;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public class TcpFireForgetExample {

  public static void main(String[] args) throws Exception {
    BasicConfigurator.configure();

    final int port = 7777;

    RSocket responseHandler = new AbstractRSocket() {
      @Override
      public Mono<Void> fireAndForget(Payload payload) {
        System.out.printf("fire-forget: %s%n", payload.getDataUtf8());
        return Mono.empty();
      }
    };

    Disposable server = RSocketFactory.receive()
        .acceptor(
            (setupPayload, rsocket) ->
                Mono.just(responseHandler))
        .transport(TcpServerTransport.create("localhost", port))
        .start()
        .subscribe();

    System.out.printf("tcp server started on port %d%n", port);

    RSocket socket =
        RSocketFactory.connect()
            .transport(TcpClientTransport.create("localhost", port))
            .start()
            .block();

    System.out.printf("tcp client initialized, connecting to port %d%n", port);

    socket
        .fireAndForget(DefaultPayload.create("message send as fire and forget"))
        .doOnNext(System.out::println)
        .block();

    Thread.sleep(2_000);
    socket.dispose();
    server.dispose();
  }
}
