package com.hhu.client.handler;

import com.hhu.codec.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.FileOutputStream;
import java.io.IOException;

public class FileReceiveClientHandler extends ChannelInboundHandlerAdapter {

	static FileOutputStream outputStream;

	static long fileLength;

	private static long readLength;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf byteBuf = (ByteBuf) msg;
		int type = byteBuf.getInt(0);
		if (type != Codec.TYPE) {
			readLength += byteBuf.readableBytes();
			writeToFile(byteBuf);
			sendComplete(readLength);
		} else {
			super.channelRead(ctx, msg);
		}
	}

	private void writeToFile(ByteBuf byteBuf) throws IOException {
		byte[] bytes = new byte[byteBuf.readableBytes()];
		byteBuf.readBytes(bytes);
		outputStream.write(bytes);
		// 调用了bytebuf的release方法，将引用计数减一，当引用计数为0时，会被回收，也就不会传递给下一个handler了
		byteBuf.release();
	}

	private void sendComplete(long readLength) throws IOException {
		if (readLength >= fileLength) {
			System.out.println("文件接收完成...");
			outputStream.close();
		}
	}
}
