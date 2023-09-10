package com.hhu.client;


import com.hhu.client.console.SendFileConsole;
import com.hhu.client.handler.FileReceiveClientHandler;
import com.hhu.client.handler.FileSendClientHandler;
import com.hhu.client.handler.FilePacketClientHandler;
import com.hhu.client.handler.LoginResponseHandler;
import com.hhu.codec.DecodeHandler;
import com.hhu.codec.EncodeHandler;
import com.hhu.protocol.request.LoginPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.stream.ChunkedWriteHandler;

public class Client {

	private static final String HOST = System.getProperty("host", "127.0.0.1");

	private static final int PORT = Integer.parseInt(System.getProperty("port", "8080"));

	public static void main(String[] args) throws InterruptedException {

		Bootstrap bootstrap = new Bootstrap();

		NioEventLoopGroup group = new NioEventLoopGroup();

		bootstrap.group(group)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.option(ChannelOption.TCP_NODELAY, true)
				// 登录： DecodeHandle -> LoginResponseHandler
				// 接收文件： 1. 文件信息： FileReceiveClientHandler -> FileSendClientHandler(在这里做消息的类型转换) ->FilePacketClientHandler<FilePacket> 接收后回复客户端一个Ack+1的值，也就是1
				// 接收文件： 2. 文件内容： FileReceiveClientHandler(重复调用channelRead来接收文件内容) 随后就结束了，其原因是调用了bytebuf的release方法，将引用计数减一，当引用计数为0时，会被回收，也就不会传递给下一个handler了
				.handler(new ChannelInitializer<NioSocketChannel>() {
					@Override
					protected void initChannel(NioSocketChannel channel) throws Exception {
						ChannelPipeline pipeline = channel.pipeline();
						pipeline.addLast(new FileReceiveClientHandler()); //入站第一个handler
						pipeline.addLast(new FileSendClientHandler());    //入站第二个handler
						pipeline.addLast(new DecodeHandler());			  //入站第三个handler
						pipeline.addLast(new EncodeHandler());        						// 出站第二个handler
						pipeline.addLast(new ChunkedWriteHandler());   						// 出站第一个handler
						pipeline.addLast(new LoginResponseHandler());     // 入站第四个handler
						pipeline.addLast(new FilePacketClientHandler());  // 入站第五个handler
						// pipeline.addLast(new MyClientHandler());
					}
				});

		ChannelFuture future = bootstrap.connect(HOST, PORT).sync();
		if (future.isSuccess()) {
			System.out.println("连接服务器成功");
			Channel channel = future.channel();
			joinCluster(channel);
			console(channel);
		} else {
			System.out.println("连接服务器失败");
		}

		future.channel().closeFuture().sync();
	}

	private static void joinCluster(Channel channel) throws InterruptedException {
		LoginPacket loginPacket = new LoginPacket("node1");
		channel.writeAndFlush(loginPacket);
		Thread.sleep(2000);
	}

	private static void console(Channel channel) {
		new Thread(() -> {
			while (!Thread.interrupted()) {
				SendFileConsole.exec(channel);
			}
		}).start();
	}

}
