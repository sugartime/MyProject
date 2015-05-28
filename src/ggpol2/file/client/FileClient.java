package ggpol2.file.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


/**
 * Simple SSL chat client modified from {@link TelnetClient}.
 */
public final class FileClient {

	
	private Logger log = Logger.getLogger(this.getClass());
	
	
	static final boolean SSL = true;
	
    static final String HOST = System.getProperty("host", "192.168.0.2");

    static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8992" : "8023"));
    
    public static void main(String[] args) throws Exception {
    	
    	PropertyConfigurator.configure("resources/log4j.properties");
    	
    	File f_certificate = new File("src/client.pem");
   	 	File f_privatekey = new File("src/clientkey.pem");
    	
        // Configure SSL.
        //final SslContext sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        //final SslContext sslCtx = SslContextBuilder.forClient().trustManager(f_certificate).build();
   	 	
   	 	final SslContext sslCtx;
   	 	
   	 	if(SSL){
   	 		sslCtx=SslContextBuilder.forClient().keyManager(f_certificate, f_privatekey,"12345").build();
   	 	}else{
   	 		sslCtx=null;
   	 	}
   	 		
   	 	
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                   @Override
                   public void initChannel(SocketChannel ch) throws Exception {
                       ChannelPipeline p = ch.pipeline();
                       if (sslCtx != null) {
                           p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
                       }
                       //p.addLast(new LoggingHandler(LogLevel.INFO));
                       p.addLast(//new StringEncoder(CharsetUtil.UTF_8),
                    		     new ChunkedWriteHandler(),
                    		     new FileClientHandler());
                   }
               });
             

            // Start the connection attempt.
            ChannelFuture f = b.connect(HOST, PORT).sync();
            
            f.channel().closeFuture().sync();
           
           
        } finally {
            // The connection is closed automatically on shutdown.
            group.shutdownGracefully();
        }
    }
    
   
}
