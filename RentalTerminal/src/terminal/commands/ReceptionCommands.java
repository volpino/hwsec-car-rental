package terminal.commands;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import terminal.crypto.ECCKeyGenerator;
import terminal.crypto.ECCSignature;
import terminal.utils.Conversions;


public class ReceptionCommands {
	public static final byte CLA_RECEPTION = (byte) 0xB1;
	public static final byte CMD_REC_INIT = (byte) 0x00;
	public static final byte CMD_REC_GET_KM = (byte) 0x01;
	public static final byte CMD_REC_RESET_KM = (byte) 0x02;
	public static final byte CMD_REC_CHECK_INUSE = (byte) 0x03;
	public static final byte CMD_REC_ADD_CERT = (byte) 0x04;
	public static final byte CMD_REC_DEL_CERT = (byte) 0x05;


	CardCommunication comm;
	SecureRandom random;
	
	byte[] cardNonce;
	byte[] cardID;
	
	boolean cardAuthenticated = false;
	
	PublicKey cardKey;
	
	
	public ReceptionCommands(CardCommunication c) {
		comm = c;
		random = new SecureRandom();
	}
	
	public void sendInitNonce() throws Exception {
		byte[] nonce = new byte[8];
		random.nextBytes(nonce);
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_RECEPTION, CMD_REC_INIT, 0x00, 0x00, nonce, 64)
		);
		
		if (response.getSW() != 0x9000) {
			throw new Exception("Got invalid response");
		}
		byte[] buf = response.getData();
		cardID = Arrays.copyOfRange(buf, 0, 2);
		cardNonce = Arrays.copyOfRange(buf, 2, 10);
		byte[] signature = Arrays.copyOfRange(buf, 10, buf.length);
		
		cardKey = ECCKeyGenerator.loadPublicKey("keys/customers", Conversions.bytesToHex(cardID));
		boolean result = ECCSignature.verifySig(nonce, cardKey, signature);
		if (!result) {
			throw new Exception("Invalid signature from the card. Authentication aborted");
		}
		
		cardAuthenticated = true;
	}
	
	byte[] sendCommand(byte command, byte[] payload) throws Exception {
		ByteArrayOutputStream dataToSign = new ByteArrayOutputStream();
		KeyPair companyKey = ECCKeyGenerator.loadKeys("keys/master", "company");
		dataToSign.write(cardNonce);
		dataToSign.write(command);
		dataToSign.write(payload);
		byte[] signature = ECCSignature.signData(dataToSign.toByteArray(), companyKey.getPrivate());
		
		byte[] nonce = new byte[8];
		random.nextBytes(nonce);
		
		ByteArrayOutputStream dataToSend = new ByteArrayOutputStream();
		dataToSend.write(nonce);
		dataToSend.write(payload.length);
		dataToSend.write(payload);
		dataToSend.write(signature.length);
		dataToSend.write(signature);
		
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_RECEPTION, command, 0x00, 0x00, dataToSend.toByteArray(), 255)
		);
		byte[] buf = response.getData();
		cardNonce = Arrays.copyOfRange(buf, 0, 8);
		byte[] result = Conversions.getChunk(buf, 8, 0);
		byte[] cardSignature = Conversions.getChunk(buf, 8, 1);
		
		ByteArrayOutputStream dataToVerify = new ByteArrayOutputStream();
		dataToVerify.write(nonce);
		dataToVerify.write(result);
		boolean verified = ECCSignature.verifySig(dataToVerify.toByteArray(), cardKey, cardSignature);
		if (!verified) {
			throw new Exception("Invalid signature from the card. The result of the command is not valid");
		}
		return result;
	}
}
