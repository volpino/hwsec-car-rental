package terminal.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import terminal.crypto.EECKeyGenerator;
import terminal.crypto.EECSignature;
import terminal.utils.Conversions;

public class PersonalizationCommands {
	// TODO: Move CLA and INS to a more general class
	public static final byte CLA_ISSUE = (byte) 0xB0;
	public static final byte CMD_INIT = (byte) 0x00;
	public static final byte CMD_RESET = (byte) 0x41;
	public static final byte CMD_READ_CARD_ID = (byte) 0x50;
	public static final byte CMD_READ_VEHICLE_ID = (byte) 0x51;
	public static final byte CMD_READ_KILOMETERS = (byte) 0x52;

	public static final byte CMD_KEY_TEST    = (byte) 0x60;
	public static final byte CMD_VERIFY_TEST = (byte) 0x61;
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
	
	public void keyTest() {
		ResponseAPDU response;
		response = comm.sendCommandAPDU(
				new CommandAPDU(CLA_ISSUE, CMD_KEY_TEST, 0x00, 0x00)
		);
	}

	public void verifyTest() {
		try {
			byte[] data = "test".getBytes();
			System.out.println("loading key pair..");
			KeyPair keys;
			keys = EECKeyGenerator.loadKeys("ECDSA", "keys/cars","car1");
			
			byte[] signedData = EECSignature.signData(data, keys.getPrivate());
			
			ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
			stream.write(Conversions.short2bytes((short) data.length));
			stream.write(data);
			stream.write(signedData);
			System.out.println(Arrays.toString(stream.toByteArray()));
			ResponseAPDU response = comm.sendCommandAPDU(
				new CommandAPDU(CLA_ISSUE, CMD_VERIFY_TEST, 0x00, 0x00, stream.toByteArray(), 255)
			);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
}
