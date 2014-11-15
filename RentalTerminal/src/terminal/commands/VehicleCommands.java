package terminal.commands;

import java.security.SecureRandom;

public class VehicleCommands {
	public static final byte CLA_VEHICLE = (byte) 0xB2;
	public static final byte CMD_VEH_INIT = (byte) 0x00;

	public static final short NONCE_LENGTH = 8;
	
	CardCommunication comm;
	SecureRandom random;	
	
	public VehicleCommands(CardCommunication c) {
		comm = c;
		random = new SecureRandom();
	}
}
