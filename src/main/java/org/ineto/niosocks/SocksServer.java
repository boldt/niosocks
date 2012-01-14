package org.ineto.niosocks;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.socket.ClientSocketChannelFactory;
import io.netty.channel.socket.nio.NioClientSocketChannelFactory;
import io.netty.channel.socket.nio.NioServerSocketChannelFactory;
import io.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocksServer {

  private Channel listenChannel;
  private OrderedMemoryAwareThreadPoolExecutor pipelineExecutor;
  private ChannelFactory factory;
  private ClientSocketChannelFactory clientFactory;
  private TrafficLogger trafficLogger;

  public SocksServer(String homeDir, Properties props) {

    int port = 1080;
    if (props.getProperty("socks.port") != null) {
      port = Integer.parseInt(props.getProperty("socks.port"));
    }

    int threads = 1;
    if (props.getProperty("socks.threads") == null) {
      threads = Runtime.getRuntime().availableProcessors() * 2 + 1;
    } else {
      threads = Integer.parseInt(props.getProperty("socks.threads"));
    }

    trafficLogger = new TrafficLogger(homeDir, props);

    clientFactory =  new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
    factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool(), threads);
    ServerBootstrap bootstrap = new ServerBootstrap(factory);

    // 200 threads max, Memory limitation: 1MB by channel, 1GB global, 100 ms of timeout
    pipelineExecutor = new OrderedMemoryAwareThreadPoolExecutor(200, 1048576, 1073741824, 100, TimeUnit.MILLISECONDS, Executors.defaultThreadFactory());

    bootstrap.setPipelineFactory(new SocksPipelineFactory(props, pipelineExecutor, clientFactory, trafficLogger));
    bootstrap.setOption("child.tcpNoDelay", true);
    bootstrap.setOption("child.keepAlive", true);
    bootstrap.setOption("child.reuseAddress", true);
    bootstrap.setOption("readWriteFair", true);

    listenChannel = bootstrap.bind(new InetSocketAddress(port));

  }

  public void join() {
    listenChannel.getCloseFuture().awaitUninterruptibly();
  }

  public void shutdown() {
    System.out.println("Shutdown server");
    listenChannel.close().awaitUninterruptibly();
    pipelineExecutor.shutdownNow();
    trafficLogger.shutdown();
    factory.releaseExternalResources();
    clientFactory.releaseExternalResources();
  }

}
