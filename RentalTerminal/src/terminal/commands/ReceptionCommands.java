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

/**
 * Class that handles the communication between the reception terminal and the card
 * 
 * @author Federico Scrinzi
 * @author Moritz Muller
 *
 */
public class ReceptionCommands {
	public static final byte CLA_RECEPTION = (byte) 0xB1;
	public static final byte CMD_REC_INIT = (byte) 0x00;
	public static final byte CMD_REC_GET_KM = (byte) 0x01;
	public static final byte CMD_REC_RESET_KM = (byte) 0x02;
	public static final byte CMD_REC_CHECK_INUSE = (byte) 0x03;
	public static final byte CMD_REC_ADD_CERT = (byte) 0x04;
	public static final byte CMD_REC_DEL_CERT = (byte) 0x05;
	public static final byte CMD_REC_ISASSOCIATED = (byte) 0x06;

	public static final short NONCE_LENGTH = 8;
	
	CardCommunication comm;
	SecureRandom random;
	
	byte[] cardNonce;
	byte[] cardID;
		
	ECPublicKey cardKey;
	
	
	public ReceptionCommands(CardCommunication c) {
		comm = c;
		random = new SecureRandom();
	}
	
	/**
	 * This initializes the communication between the reception and the card 
	 * 
	 * @throws Exception
	 */
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
		
		cardKey = (ECPublicKey) ECCKeyGenerator.loadPublicKey("data/customers", Conversions.bytesToHex(cardID));
		boolean result = ECCSignature.verifySig(nonce, cardKey, signature);
		if (!result) {
			throw new SecurityException("Invalid signature from the card. Authentication aborted");
		}
	}
	
	/**
	 * Generic method to send reception commands to the card
	 * 
	 * @param command INS code of the requested command
	 * @param payload arguments of the command
	 * @return response of the command
	 * @throws Exception
	 */
	private byte[] sendCommand(byte command, byte[][] payload) throws Exception {
		ByteArrayOutputStream dataToSign = new ByteArrayOutputStream();
		KeyPair companyKey = ECCKeyGenerator.loadKeys("data/master", "company");
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
		if (response.getSW() != 0x9000) {
			throw new Exception("Got invalid response");
		}
		
		byte[] buf = response.getData();
		cardNonce = Arrays.copyOfRange(buf, 0, NONCE_LENGTH);

		ByteArrayOutputStream dataToVerify = new ByteArrayOutputStream();
		dataToVerify.write(nonce);
		dataToVerify.write(CLA_RECEPTION);
		dataToVerify.write(command);
		byte[] result = null;
		
		if (buf[NONCE_LENGTH] != 0) {
			result = Conversions.getChunk(buf, NONCE_LENGTH, 0);
			dataToVerify.write(result);
		}
		
		byte[] cardSignature = Conversions.getChunk(buf, NONCE_LENGTH, 1);
		
		boolean verified = ECCSignature.verifySig(dataToVerify.toByteArray(), cardKey, cardSignature);
		if (!verified) {
			throw new SecurityException(
				"Invalid signature from the card. The result of the command is not valid"
			);
		}
		return result;
	}
	
	/**
	 * Get the kilometer counter
	 * 
	 * @return kilometer counter as long
	 * @throws Exception
	 */
	public long getKilometers() throws Exception{
		byte[] kilometers = sendCommand(CMD_REC_GET_KM, null);
		return Conversions.bytesToLong(kilometers);
	}
	
	/**
	 * Reset the kilometer counter
	 * 
	 * @throws Exception
	 */
	public void resetKilometers() throws Exception{
		sendCommand(CMD_REC_RESET_KM, null);
	}
	
	/**
	 * Check the status of the inUse flag
	 * 
	 * @return the value of the inUse flag
	 * @throws Exception
	 */
	public boolean checkInUseFlag() throws Exception{
		byte[] response = sendCommand(CMD_REC_CHECK_INUSE, null);
		return (response[0] == (byte) 1);
	}
	
	/**
	 * Sends a certificate that allows the card to interact with a vehicle, together with
	 * the vehicle public key and the certificate counter.
	 * 
	 * @param publicVehicleKey public key of the chosen vehicle
	 * @throws Exception
	 */
	public void addVehicleCert(ECPublicKey publicVehicleKey) throws Exception {
		byte[] counter = Conversions.longToBytes(CertCounter.getNewCounter());
		ByteArrayOutputStream dataToSign = new ByteArrayOutputStream();
		dataToSign.write(Conversions.encodePubKey(publicVehicleKey));
		dataToSign.write(Conversions.encodePubKey(cardKey));
		dataToSign.write(counter);
		
		KeyPair companyKey = ECCKeyGenerator.loadKeys("data/master", "company");
		byte[] signature = ECCSignature.signData(dataToSign.toByteArray(), companyKey.getPrivate());

		byte[][] data = new byte[3][];
		data[0] = signature;
		data[1] = Conversions.encodePubKey(publicVehicleKey);
		data[2] = counter;
		
		sendCommand(CMD_REC_ADD_CERT, data);
	}
	
	/**
	 * Deletes the vehicle certificate from the card, de-associating the card from the vehicle
	 * 
	 * @throws Exception
	 */
	public void deleteVehicleCert() throws Exception{
		sendCommand(CMD_REC_DEL_CERT, null);
	}
	
	/**
	 * Get the association status of the card
	 * 
	 * @return the public key of the vehicle the card is associated with. null if it's not associated
	 * @throws Exception
	 */
	public byte[] associationStatus() throws Exception{
		byte[] vehiclePubKey = sendCommand(CMD_REC_ISASSOCIATED, null);
		return vehiclePubKey;
	}
}
