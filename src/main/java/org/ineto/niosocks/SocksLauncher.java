package org.ineto.niosocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class SocksLauncher {

  private final static Logger log = Logger.getLogger(SocksLauncher.class);
  
  public static void main(String[] args) {
    
    if (args.length < 1) {
      System.out.println("Wrong number of arguments");
      System.exit(1);
    }
    
    String homeDir = new File(args[0]).getParentFile().getAbsolutePath();
    Properties props = load(homeDir, "socks.properties");
    
    System.out.println("Socks Server started at " + props.getProperty("socks.port") + " port");

    final SocksServer server = new SocksServer(homeDir, props);
    
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        server.shutdown();
      }
    });

    server.join();
  }
  
  
  public static Properties load(String homeDir, String fileName) {
    
    String file = homeDir + File.separator + "conf" + File.separator + fileName; 
    
    Properties props = new Properties();
    try {
      FileInputStream in = new FileInputStream(file);
      try {
        props.load(in);
      }
      finally {
        in.close();
      }
    }
    catch(IOException e) {
      log.error("file io fail", e);
    }
    return props;
    
  }
}
