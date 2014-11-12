package terminal.utils;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;


import javax.swing.JTextArea;


public class Log {
	public static Logger logger;
	static {
		logger = Logger.getLogger("");
		logger.addHandler(new TextAreaHandler());
	}
	
	public static void info(Object o) {
		logger.log(Level.INFO, o.toString());
	}
	
	public static void error(Object o) {
		logger.log(Level.SEVERE, o.toString());
	}
	
	public static void debug(Object o) {
		logger.log(Level.INFO, o.toString());		
	}
	
	public static JTextArea getLoggingArea() {
		for(Handler handler: logger.getHandlers()){
			if(handler instanceof TextAreaHandler){
				TextAreaHandler textAreaHandler = (TextAreaHandler) handler;
				return textAreaHandler.getTextArea();
			}
		}
		return null;
	}
}