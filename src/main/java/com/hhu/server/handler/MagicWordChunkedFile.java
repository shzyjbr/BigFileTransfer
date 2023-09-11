package com.hhu.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.stream.ChunkedInput;

import java.io.IOException;
import java.io.RandomAccessFile;

public class MagicWordChunkedFile implements ChunkedInput<ByteBuf> {

    private final DefaultFileRegion region;
    private final int magicWord;
    private boolean magicWordWritten;

    public MagicWordChunkedFile(RandomAccessFile file, int magicWord) throws IOException {
        this.region = new DefaultFileRegion(file.getChannel(), 0, file.length());
        this.magicWord = magicWord;
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        return magicWordWritten && region.transferred() >= region.count();
    }

    @Override
    public void close() throws Exception {
        region.release();
    }

    @Override
    public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
        if (!magicWordWritten) {
            magicWordWritten = true;
            return ctx.alloc().buffer(4).writeInt(magicWord);
        } else {
            return null;  // Let Netty read from the FileRegion directly.
        }
    }


    @Override
    public ByteBuf readChunk(ByteBufAllocator byteBufAllocator) throws Exception {
        if (!magicWordWritten) {
            magicWordWritten = true;
            return byteBufAllocator.buffer(4).writeInt(magicWord);
        } else {
            return null;  // Let Netty read from the FileRegion directly.
        }
    }


    @Override
    public long length() {
        return region.count() + 4;
    }

    @Override
    public long progress() {
        return region.transferred() + (magicWordWritten ? 4 : 0);
    }
}
