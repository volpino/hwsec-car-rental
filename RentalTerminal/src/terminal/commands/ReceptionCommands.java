package terminal.commands;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import terminal.crypto.ECCKeyGenerator;
import terminal.crypto.ECCSignature;
import terminal.utils.CertCounter;
import terminal.utils.Conversions;
import terminal.utils.Log;


public class ReceptionCommands {
	public static final byte CLA_RECEPTION = (byte) 0xB1;
	public static final byte CMD_REC_INIT = (byte) 0x00;
	public static final byte CMD_REC_GET_KM = (byte) 0x01;
	public static final byte CMD_REC_RESET_KM = (byte) 0x02;
	public static final byte CMD_REC_CHECK_INUSE = (byte) 0x03;
	public static final byte CMD_REC_ADD_CERT = (byte) 0x04;
	public static final byte CMD_REC_DEL_CERT = (byte) 0x05;

	public static final short NONCE_LENGTH = 8;
	
	CardCommunication comm;
	SecureRandom random;
	
	byte[] cardNonce;
	byte[] cardID;
	
	boolean cardAuthenticated = false;
	
	ECPublicKey cardKey;
	
	
	public ReceptionCommands(CardCommunication c) {
		comm = c;
		random = new SecureRandom();
	}
	
	public void sendInitNonce() throws Exception {
		byte[] nonce = new byte[NONCE_LENGTH];
		random.nextBytes(nonce);
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_RECEPTION, CMD_REC_INIT, 0x00, 0x00, nonce, 64)
		);
		
		if (response.getSW() != 0x9000) {
			throw new Exception("Got invalid response");
		}
		byte[] buf = response.getData();
		
		// message is of the format <nonce>||<cardIDLength>||cardID
		cardID = Arrays.copyOfRange(buf, 0, 2);
		cardNonce = Arrays.copyOfRange(buf, 2, 2+NONCE_LENGTH);
		byte[] signature = Arrays.copyOfRange(buf, NONCE_LENGTH+2, buf.length);
		
		Log.info("Card ID: "+Conversions.bytesToHex(cardID));
		
		cardKey = (ECPublicKey) ECCKeyGenerator.loadPublicKey("keys/customers", Conversions.bytesToHex(cardID));
		boolean result = ECCSignature.verifySig(nonce, cardKey, signature);
		if (!result) {
			throw new Exception("Invalid signature from the card. Authentication aborted");
		}
		
		cardAuthenticated = true;
	}
	
	byte[] sendCommand(byte command, byte[][] payload) throws Exception {
		ByteArrayOutputStream dataToSign = new ByteArrayOutputStream();
		KeyPair companyKey = ECCKeyGenerator.loadKeys("keys/master", "company");
		dataToSign.write(cardNonce);
		dataToSign.write(command);
		if (payload != null) {
			for (int i=0; i<payload.length; i++) {
				dataToSign.write(payload[i]);
			}
		}
		
		byte[] signature = ECCSignature.signData(dataToSign.toByteArray(), companyKey.getPrivate());
		
		byte[] nonce = new byte[NONCE_LENGTH];
		random.nextBytes(nonce);
		
		ByteArrayOutputStream dataToSend = new ByteArrayOutputStream();
		dataToSend.write(nonce);
		dataToSend.write(signature.length);
		dataToSend.write(signature);
		
		if (payload != null) {
			for (int i=0; i<payload.length; i++) {
				dataToSend.write(payload[i].length);
				dataToSend.write(payload[i]);
			}
		}
		else {
			dataToSend.write(0);
		}
		
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_RECEPTION, command, 0x00, 0x00, dataToSend.toByteArray(), 255)
		);
		byte[] buf = response.getData();
		cardNonce = Arrays.copyOfRange(buf, 0, NONCE_LENGTH);

		ByteArrayOutputStream dataToVerify = new ByteArrayOutputStream();
		dataToVerify.write(nonce);
		byte[] result = null;
		
		if (buf[NONCE_LENGTH] != 0) {
			result = Conversions.getChunk(buf, NONCE_LENGTH, 0);
			dataToVerify.write(result);
		}
		
		byte[] cardSignature = Conversions.getChunk(buf, NONCE_LENGTH, 1);
		
		boolean verified = ECCSignature.verifySig(dataToVerify.toByteArray(), cardKey, cardSignature);
		if (!verified) {
			throw new Exception("Invalid signature from the card. The result of the command is not valid");
		}
		return result;
	}
	
	public int getKilometers() throws Exception{
		byte[] kilometers = sendCommand(CMD_REC_GET_KM, null);
		return  Conversions.bytesToInt(kilometers);
	}
	
	public void resetKilometers() throws Exception{
			sendCommand(CMD_REC_RESET_KM, null);
	}
	
	public boolean checkInUseFlag() throws Exception{
		byte[] response = sendCommand(CMD_REC_CHECK_INUSE, null);
		return (response[0] == (byte) 1);
	}
	
	public void addVehicleCert(ECPublicKey publicVehicleKey) throws Exception {
		short counter = (short) CertCounter.getNewCounter();
		ByteArrayOutputStream dataToSign = new ByteArrayOutputStream();
		dataToSign.write(Conversions.encodePubKey(publicVehicleKey));
		dataToSign.write(Conversions.encodePubKey(cardKey));
		dataToSign.write(counter);
		
		KeyPair companyKey = ECCKeyGenerator.loadKeys("keys/master", "company");
		byte[] signature = ECCSignature.signData(dataToSign.toByteArray(), companyKey.getPrivate());

		byte[][] data = new byte[3][];
		data[0] = signature;
		data[1] = Conversions.encodePubKey(publicVehicleKey);
		data[2] = Conversions.short2bytes(counter);
		
		sendCommand(CMD_REC_ADD_CERT, data);
	}
	
	public void deleteVehicleCert() throws Exception{
		sendCommand(CMD_REC_DEL_CERT, null);
	}
}
