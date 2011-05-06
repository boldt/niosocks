package org.ineto.niosocks;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

public class FileUtils {

  private final static Logger log = Logger.getLogger(FileUtils.class);

  public static void writeLog(String logfile, int num, byte[] blob) {
    try {
      FileOutputStream out = new FileOutputStream("../log/" + logfile + num);
      try {
        out.write(blob);
      }
      finally {
        out.close();
      }
    }
    catch(IOException e) {
      log.error("fail to write to file", e);
    }
  }
  
}
