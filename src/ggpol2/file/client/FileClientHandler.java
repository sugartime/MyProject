package ggpol2.file.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;

public class FileClientHandler extends SimpleChannelInboundHandler<Object>{
	
	private Logger log = Logger.getLogger(this.getClass());
	
	//1MB POOLED 버퍼
	ByteBuf m_pool_buf;
	
	
	private String filepath;
	    		 	 
    
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("handlerAdded");
		
		m_pool_buf=PooledByteBufAllocator.DEFAULT.directBuffer(1048576);
	}
	

    @Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("handlerRemoved");
	}
	
	/**
     * Protocol 초기화
     * [4byte|?byte|8byte|chunk ?byte]
     * [filenamelen|filenamedata|filedatalen|filedatadatachunks]
     *
     * @param file
     * @return
     */
    private ByteBuf initializeProtocol(File file) {
    	
    	
    	String f_name ="";
    	try {
    		f_name = URLEncoder.encode(file.getName(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    	//ByteBuf buffer = m_pool_buf.alloc.buffer(file.getName().length() + 12);
    	ByteBuf buffer = m_pool_buf.alloc().buffer(8192);
    	buffer.writeInt(f_name.length());
        buffer.writeBytes(f_name.getBytes());
        buffer.writeLong(file.length());
        return buffer;
    }
    
    
    @Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	System.out.println("channelActive");
    	 
    	 File file  = new File("c:\\Tulips.jpg");
    	 //File file  = new File("E:\\UTIL\\dotnet framework 4.0\\dotNetFx40_Full_x86.exe");
    	   
    	
    	
    	 RandomAccessFile raf = new RandomAccessFile(file, "r");
    	 long fileLength = raf.length();
    	 
    	 ByteBuf bufferHead = initializeProtocol(file);
    	 
    	 // Write the initial line and the header.
         ctx.writeAndFlush(bufferHead);

         // Write the content.
         final ChannelFuture sendFileFuture;
         ChannelFuture lastContentFuture;
         
         if (ctx.pipeline().get(SslHandler.class) == null) {
        	 sendFileFuture =ctx.writeAndFlush(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
         } else {
        	 System.out.println("ssl transfer");
             sendFileFuture = ctx.writeAndFlush(new ChunkedFile(raf, 0, fileLength, 8192),ctx.newProgressivePromise());
             // HttpChunkedInput will write the end marker (LastHttpContent) for us.
             //lastContentFuture = sendFileFuture;
         }
    	     	 
         
         sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
             @Override
             public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                 if (total < 0) { // total unknown
                     System.err.println(future.channel() + " Transfer progress: " + progress);
                 } else {
                     System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
                 }
             }

             @Override
             public void operationComplete(ChannelProgressiveFuture future) {
                 System.err.println(future.channel() + " Transfer complete.");
                 //lastContentFuture.addListener(ChannelFutureListener.CLOSE);
                 //sendFileFuture.addListener(ChannelFutureListener.CLOSE);
                
                 //자원 해제
                 ctx.close();
                 
                 //bufferHead.release();
             }
         });
      
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg)	throws Exception {
		// TODO Auto-generated method stub
	}


	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
    

	

}
