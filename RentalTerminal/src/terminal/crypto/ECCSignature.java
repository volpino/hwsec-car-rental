package terminal.crypto;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

/**
 * Collection of helpers for ECC signature operations
 * 
 * @author Alessio Parzian
 *
 */
public class ECCSignature {
	/**
	 * Sign a byte array using ECDSA
	 * 
	 * @param data the data to sign
	 * @param key the key used for signing
	 * @return a byte array with the signature
	 * @throws Exception
	 */
	public static byte[] signData(byte[] data, PrivateKey key) throws Exception{
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		Signature signer = Signature.getInstance("SHA1withECDSA", "BC");
		signer.initSign(key);
		signer.update(data);
		return signer.sign();
	}
	
	/**
	 * Verify an ECDSA signature
	 * 
	 * @param data the data to verify
	 * @param key the key used for verification
	 * @param sig the signature to verify
	 * @return a boolean value that confirms the validity of the signature
	 * @throws Exception
	 */
	public static boolean verifySig(byte[] data, PublicKey key, byte[] sig) throws Exception {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		Signature signer = Signature.getInstance("SHA1withECDSA", "BC");
	    signer.initVerify(key);
	    signer.update(data);
	    return signer.verify(sig);
	 }
	
	/**
	 * Testing method for signature generation and verification
	 * 
	 * @param args
	 */
	public static void main(String[] args){
		
		String test = "HeyJustTryIt";
		byte[] data = test.getBytes();
		byte[] signedData;
		
		// Try to verify with a wrong data to make sure it works
		//String test1 = "HHHeyJustTryIt";
		//byte[] data1 = test1.getBytes();
			
		try{
			//System.out.println("Creating key pair..");
			//KeyPair keys = EECKeyGenerator.generateKeys();
			System.out.println("loading key pair..");
			KeyPair keys = ECCKeyGenerator.loadKeys("keys/cars", "car1");

			System.out.println("DONE");
			System.out.println("Data: " + Arrays.toString(data));
			
			System.out.println("Signing..");
			signedData = signData(data, keys.getPrivate());
			System.out.println("DONE");
			System.out.println("The signature is: "+ Arrays.toString(signedData));
			System.out.println("Signature length: "+ signedData.length);
			System.out.println("Verifying Signature..");
			boolean sigIsValid = verifySig(data, keys.getPublic(),signedData);
			System.out.println("Signature verified: "+sigIsValid);	
		} catch (NoSuchAlgorithmException e) {
			 System.err.println("Caught exception " + e.toString());
		} catch (NoSuchProviderException e) {
			 System.err.println("Caught exception " + e.toString());
		} catch (InvalidKeyException e) {
			 System.err.println("Caught exception " + e.toString());
		} catch (SignatureException e) {
			 System.err.println("Caught exception " + e.toString());
		} catch (Exception e) {
			 System.err.println("Caught exception " + e.toString());
		}
		
	}

}
