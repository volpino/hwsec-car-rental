package terminal.utils;

import java.io.IOException;  
import java.util.logging.FileHandler;  
import java.util.logging.Logger;  
import java.util.logging.SimpleFormatter;  

/**
 * Class to log to a file
 * 
 * @author Nils Rodday
 *
 */
public class LogToFile {
    public static void write(String message) {
        Logger logger = Logger.getLogger("LogToFile");  
        FileHandler fh;  
          
        try {  
            // This block configure the logger with handler and formatter  
            fh = new FileHandler("Vehicle.log", true);  
            logger.addHandler(fh);  
            //logger.setLevel(Level.ALL);  
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  
            
            // the following statement is used to log any messages  
            logger.info(message);  
            
            fh.close();
            logger.removeHandler(fh);
        } catch (SecurityException e) {
            e.printStackTrace();  
        } catch (IOException e) {
            e.printStackTrace();  
        }
    } 
} 