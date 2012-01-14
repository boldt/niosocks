package org.ineto.niosocks;

import java.util.Properties;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPipelineFactory;
import io.netty.channel.Channels;
import io.netty.channel.socket.ClientSocketChannelFactory;
import io.netty.handler.execution.ExecutionHandler;
import io.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;

public class SocksPipelineFactory implements ChannelPipelineFactory {

  private final Properties props;
  private final OrderedMemoryAwareThreadPoolExecutor pipelineExecutor;
  private final ClientSocketChannelFactory clientFactory;
  private final TrafficLogger trafficLogger;

  public SocksPipelineFactory(Properties props, OrderedMemoryAwareThreadPoolExecutor pipelineExecutor, ClientSocketChannelFactory clientFactory, TrafficLogger trafficLogger) {
    super();
    this.props = props;
    this.pipelineExecutor = pipelineExecutor;
    this.clientFactory = clientFactory;
    this.trafficLogger = trafficLogger;
  }

  public ChannelPipeline getPipeline() throws Exception {
    ChannelPipeline pipeline = Channels.pipeline();
    pipeline.addLast("pipelineExecutor", new ExecutionHandler(pipelineExecutor));
    pipeline.addLast("handler", new SocksServerHandler(props, clientFactory, trafficLogger));
    return pipeline;
  }

}
