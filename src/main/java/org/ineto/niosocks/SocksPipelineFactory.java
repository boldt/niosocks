package org.ineto.niosocks;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;

public class SocksPipelineFactory implements ChannelPipelineFactory {

  private final ChannelGroup clientsGroup;
  private final OrderedMemoryAwareThreadPoolExecutor pipelineExecutor;
  private final ClientSocketChannelFactory clientFactory;
  
  public SocksPipelineFactory(ChannelGroup clientsGroup, OrderedMemoryAwareThreadPoolExecutor pipelineExecutor, ClientSocketChannelFactory clientFactory) {
    super();
    this.clientsGroup = clientsGroup;
    this.pipelineExecutor = pipelineExecutor;
    this.clientFactory = clientFactory;
  }
  
  public ChannelPipeline getPipeline() throws Exception {
    ChannelPipeline pipeline = Channels.pipeline();
    pipeline.addLast("pipelineExecutor", new ExecutionHandler(pipelineExecutor));
    pipeline.addLast("handler", new SocksServerHandler(clientFactory, clientsGroup));
    return pipeline;
  }
  
}
