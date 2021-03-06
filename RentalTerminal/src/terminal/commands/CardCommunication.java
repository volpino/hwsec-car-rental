package terminal.commands;

import java.util.Arrays;
import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import terminal.gui.TerminalInterface;
import terminal.utils.Log;


/**
 * Class for handling card communication and providing handy wrappers for sending APDUs 
 * 
 * @author Leon Schoorl
 * @author Federico Scrinzi
 *
 */
public class CardCommunication {
	static final byte[] APPLET_AID = {0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x01};
	static final CommandAPDU SELECT_APDU = new CommandAPDU((byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, APPLET_AID);
	static final int RESP_OK = 0x9000;
	
	TerminalInterface terminal = null;
    CardChannel applet;
	
    /**
     * Constructor for the class. It starts a thread that handles card communication
     */
	public CardCommunication() {
        (new CardThread()).start();		
	}
	
	/**
	 * Constructor for the class. It starts a thread that handles card communication
	 * It also receives a TerminalInterface parameter, callbacks on this objects are called on card
	 * insertion and removal
	 * 
	 * @param t TerminalInterface associated with the card communication
	 */
	public CardCommunication(TerminalInterface t) {
		terminal = t;
        (new CardThread()).start();
	}
	
	/**
	 * Wrapper for easing the sending of APDUs
	 * 
	 * @param command a commandAPDU object with the command that we want to send to the card
	 * @return ResponseAPDU object with the response of the sent command
	 */
	public ResponseAPDU sendCommandAPDU(CommandAPDU command) {
		Log.debug("Sending: " + command);
		Log.debug("Sending data: " + Arrays.toString(command.getData()));
		ResponseAPDU response = null;
		try {
			response = applet.transmit(command);
		} catch (CardException e) {
			Log.error(e.getMessage());
			e.printStackTrace();
			return null;
		}
		Log.debug("Response: " + response);
		Log.debug("Response data: " + Arrays.toString(response.getData()));
		return response;
	}
	
	/**
	 * Thread that handles the card communication
	 * 
	 * @author Leon Schoorl
	 * @author Federico Scrinzi
	 *
	 */
    class CardThread extends Thread {
		public void run() {
            try {
            	TerminalFactory tf = TerminalFactory.getDefault();
    	    	CardTerminals ct = tf.terminals();
    	    	List<CardTerminal> cs = null;
    	    	try {
    	    		cs = ct.list(CardTerminals.State.CARD_PRESENT);
    	    	} catch (Exception e) {
    	    		Log.error("No readers available.");
    	    		return;
    	    	}
    	    	if (cs.isEmpty()) {
    	    		Log.error("No terminals with a card found.");
    	    		return;
    	    	}
    	    	
    	    	while (true) {
    	    		try {
    	    			for(CardTerminal c : cs) {
    	    				if (c.isCardPresent()) {
    	    					try {
    	    						Card card = c.connect("*");    	    															
									try {
										// select applet.
										applet = card.getBasicChannel();
										
										Log.debug("Selecting application");
										ResponseAPDU resp = applet.transmit(SELECT_APDU);

										if (resp.getSW() != RESP_OK) {
											throw new Exception("Select failed");
										}

										// now we are ready
										Log.info("Card is ready!");
										if (terminal != null)
											terminal.cardInserted();
										
										// wait for the card to be removed
										while (c.isCardPresent());
										
										// card has been removed!
										if (terminal != null)
											terminal.cardRemoved();
										
										break;
									} catch (Exception e) {
										Log.error("Card does not contain applet!");
										e.printStackTrace();
										sleep(2000);
										continue;
									}
   	    						
    	    					} catch (CardException e) {
    	    						Log.error("Couldn't connect to card!");
    	    						sleep(2000);
    	    						continue;
    	    					}
    	    				} else {
    	    					Log.error("No card present!");
    	    					sleep(2000);
    	    					continue;
    	    				}
    	    			}
    	    		} catch (CardException e) {
    	    			Log.error("Card status problem!");
    	    		}
    	    	}
            } catch (Exception e) {
                Log.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
