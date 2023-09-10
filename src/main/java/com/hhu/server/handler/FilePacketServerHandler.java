package com.hhu.server.handler;

import com.hhu.protocol.FilePacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 该入站处理器只处理FilePacket类型的数据包，因此如果不是FilePacket类型的数据包，就会跳过该处理器
 */
@ChannelHandler.Sharable
public class FilePacketServerHandler extends SimpleChannelInboundHandler<FilePacket> {
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FilePacket packet) throws Exception {
		File file = packet.getFile();
		System.out.println("receive file from client: " + file.getName());
		FileReceiveServerHandler.fileLength = file.length();
		FileReceiveServerHandler.outputStream = new FileOutputStream(
				new File("./server-receive-" + file.getName())
		);
		// 一个数据包传递到pipeline的尾部，经过处理，重复利用了这个packet，再从尾部到头部写出
		packet.setACK(packet.getACK() + 1);
		ctx.writeAndFlush(packet);
	}
}
