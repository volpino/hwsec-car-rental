package terminal.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import terminal.crypto.ECCKeyGenerator;
import terminal.crypto.ECCSignature;
import terminal.utils.CertCounter;
import terminal.utils.Conversions;

import terminal.utils.LogToFile;

/**
 * Class that handles the communication between the vehicle and the card
 * 
 * @author Federico Scrinzi
 * @author Nils Rodday
 *
 */
public class VehicleCommands {
	public static final byte CLA_VEHICLE = (byte) 0xB2;
	public static final byte CMD_VEH_INIT = (byte) 0x00;
	public static final byte CMD_VEH_START = (byte) 0x01;
	public static final byte CMD_VEH_SAVEKM = (byte) 0x02;
	
	public static final int SW_CONDITIONS_NOT_SATISFIED = 0x6985;

	
	public static final short NONCE_LENGTH = 8;
	
	CardCommunication comm;
	SecureRandom random;
	
	byte[] cardNonce;

	ECPublicKey cardKey;
	ECPublicKey companyKey;
	KeyPair vehicleKeypair;
	
	String carID;
	
	public VehicleCommands(CardCommunication c, String id) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		comm = c;
		random = new SecureRandom();
		carID = id;
		vehicleKeypair = ECCKeyGenerator.loadKeys("data/cars", carID);
		companyKey = (ECPublicKey) ECCKeyGenerator.loadPublicKey("data/master", "company");
	}
	
	/**
	 * Initializes the communication between the vehicle and the card
	 * 
	 * @throws Exception
	 */
	private void sendInit() throws Exception {
		byte[] nonce = new byte[NONCE_LENGTH];
		random.nextBytes(nonce);
		
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_VEHICLE, CMD_VEH_INIT, 0x00, 0x00, nonce, 255)
		);
		if (response.getSW() != 0x9000) {
			throw new Exception("Got invalid response");
		}
		byte[] buffer = response.getData();
	
		//Receive nonceB
		cardNonce = Arrays.copyOfRange(buffer, 0, NONCE_LENGTH);

		//Public card key
		cardKey = (ECPublicKey) ECCKeyGenerator.decodeKey(Conversions.getChunk(buffer, NONCE_LENGTH, 0));
		
		//Get Cert counter
		long certCounter = Conversions.bytesToLong(Conversions.getChunk(buffer, NONCE_LENGTH, 1));
		
		//Get Sc
		byte[] vehicleCert = Conversions.getChunk(buffer, NONCE_LENGTH, 2);
		
		//Get validation Certificate
		byte[] validationCert = Conversions.getChunk(buffer, NONCE_LENGTH, 3);
		
		//Check that new counter is higher than old one
		long currentCounter = CertCounter.getCarCounter(carID);
		if (certCounter >= currentCounter) {
			CertCounter.setCarCounter(carID, certCounter);
		}
		else {
			throw new SecurityException("CertCounter is lower than vehicle's one. Rejecting card");
		}
		
		ByteArrayOutputStream dataToVerify = new ByteArrayOutputStream();
		
		//Verify Signature Sc
		dataToVerify.write(Conversions.encodePubKey((ECPublicKey) vehicleKeypair.getPublic()));
		dataToVerify.write(Conversions.encodePubKey(cardKey));
		dataToVerify.write(Conversions.longToBytes(certCounter));
		
		boolean verified = ECCSignature.verifySig(dataToVerify.toByteArray(), companyKey, vehicleCert);
		if (!verified) {
			throw new SecurityException("Vehicle certificate is not valid. Rejecting card");
		}
		
		dataToVerify.reset();
		
		//Verify Signature of NonceA and Sc
		dataToVerify.write(nonce);
		dataToVerify.write(vehicleCert);
		
		verified = ECCSignature.verifySig(dataToVerify.toByteArray(), cardKey, validationCert);
		if (!verified) {
			throw new SecurityException("Signature of Nonce and Signature Sc is not valid!");
		}
	}
	
	/**
	 * Sends a command to the card for starting the vehicle.
	 * If the operation is successful, it means that the card was authorized for this vehicle and
	 * the inUse flag on the card was set. 
	 * 
	 * @throws Exception
	 */
	private void sendStart() throws Exception {
		byte[] nonce = new byte[NONCE_LENGTH];
		random.nextBytes(nonce);
		byte[] signature = ECCSignature.signData(cardNonce, vehicleKeypair.getPrivate());
		
		ByteArrayOutputStream dataToSend = new ByteArrayOutputStream();
		dataToSend.write(nonce);
		dataToSend.write(signature.length);
		dataToSend.write(signature);
		
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_VEHICLE, CMD_VEH_START, 0x00, 0x00, dataToSend.toByteArray(), 255)
		);
		
		if (response.getSW() == SW_CONDITIONS_NOT_SATISFIED) {
			throw new SecurityException(
				"InUse flag is set. The vehicle cannot be started before the previous km " +
				"value will be saved on the card"
			);
		}
		
		if (response.getSW() != 0x9000) {
			throw new Exception("Got invalid response");
		}
		
		byte[] buffer = response.getData();
		cardNonce = Arrays.copyOfRange(buffer, 0, NONCE_LENGTH);
		signature = Arrays.copyOfRange(buffer, NONCE_LENGTH, buffer.length);
		boolean verified = ECCSignature.verifySig(nonce, cardKey, signature);
		if (!verified) {
			throw new SecurityException("Card signature is not valid. Rejecting card and not starting vehicle");
		}
	}
	
	/**
	 * Sends a command to write the driven kilometers to the card.
	 * If the operation was successful a non-repudiation proof of the writing on the card is sent
	 * to the terminal and saved in the terminal log 
	 * 
	 * @param kilometers number of driven kilometers
	 * @throws Exception
	 */
	void writeKilometers(long kilometers) throws Exception {
		byte[] nonce = new byte[NONCE_LENGTH];
		random.nextBytes(nonce);
		
		byte[] kmBytes = Conversions.longToBytes(kilometers);
		
		ByteArrayOutputStream dataToSign = new ByteArrayOutputStream();
		dataToSign.write(cardNonce);
		dataToSign.write(kmBytes);
		byte[] signature = ECCSignature.signData(dataToSign.toByteArray(), vehicleKeypair.getPrivate());
		
		ByteArrayOutputStream dataToSend = new ByteArrayOutputStream();
		dataToSend.write(nonce);
		dataToSend.write(kmBytes.length);
		dataToSend.write(kmBytes);
		dataToSend.write(signature.length);
		dataToSend.write(signature);
		
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_VEHICLE, CMD_VEH_SAVEKM, 0x00, 0x00, dataToSend.toByteArray(), 255)
		);
		if (response.getSW() != 0x9000) {
			throw new Exception("Got invalid response");
		}
		
		ByteArrayOutputStream dataToVerify = new ByteArrayOutputStream();
		dataToVerify.write(nonce);
		dataToVerify.write(CLA_VEHICLE);
		dataToVerify.write(CMD_VEH_SAVEKM);
		dataToVerify.write(kmBytes);
		
		byte[] buffer = response.getData();
		boolean verified = ECCSignature.verifySig(dataToVerify.toByteArray(), cardKey, buffer);
		if (!verified) {
			LogToFile.write("Card signature is not valid. Writing of kilometers was not successful", carID);
			throw new SecurityException("Card signature is not valid. Writing of kilometers was not successful");
			
		}
		
		LogToFile.write(
			"Nonce: " + Arrays.toString(nonce) + "\n" +
			"Kilometers: " + Arrays.toString(kmBytes) + "\n" +
			"Signature: " + Arrays.toString(buffer),
		carID);
		
	}
	
	/**
	 * Sends initialization command and checks if the card is allowed to start the vehicle
	 * 
	 * @throws Exception
	 */
	public void startVehicle() throws Exception {
		sendInit();
		sendStart();
	}
	
	/**
	 * Sends initialization command and sends the amount of driven kilometers to the card
	 * 
	 * @param kilometers number of driven kilometers
	 * @throws Exception
	 */
	public void stopVehicle(long kilometers) throws Exception {
		sendInit();
		writeKilometers(kilometers);
	}
}
