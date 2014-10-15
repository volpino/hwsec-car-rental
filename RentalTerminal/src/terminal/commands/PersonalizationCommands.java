package terminal.commands;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;


public class PersonalizationCommands {
	// TODO: Move CLA and INS to a more general class
	public static final byte CLA_ISSUE = (byte) 0xB0;
	public static final byte CMD_INIT = (byte) 0x00;
	public static final byte CMD_RESET = (byte) 0x41;
	public static final byte CMD_READ_CARD_ID = (byte) 0x50;
	public static final byte CMD_READ_VEHICLE_ID = (byte) 0x51;
	public static final byte CMD_READ_KILOMETERS = (byte) 0x52;
	
	public static final int RESP_OK = 0x9000;


	
	CardCommunication comm;
	
	public PersonalizationCommands(CardCommunication c) {
		comm = c;
	}
	
	public void setCardID() {
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_INIT, 0x00, 0x00, new byte[] {0x01, 0x02})
		);
		
		response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_READ_VEHICLE_ID, 0x00, 0x00, 0x02)
		);
		
		response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_READ_KILOMETERS, 0x00, 0x00, 0x02)
		);
		
		response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_READ_CARD_ID, 0x00, 0x00, 0x02)
		);
	}
}
