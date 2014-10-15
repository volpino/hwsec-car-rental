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

public class EECSignature {
	
	public static byte[] signData(byte[] data, PrivateKey key) throws Exception{
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		Signature signer = Signature.getInstance("SHA256withECDSA", "BC");
		signer.initSign(key);
		signer.update(data);
		return signer.sign();
	}
	
	public static boolean verifySig(byte[] data, PublicKey key, byte[] sig) throws Exception {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		Signature signer = Signature.getInstance("SHA256withECDSA", "BC");
	    signer.initVerify(key);
	    signer.update(data);
	    return signer.verify(sig);
	 }
	
	// Testing purpose	
	public static void main(String[] args){
		
		String test = "HeyJustTryIt";
		byte[] data = test.getBytes();
		byte[] signedData;
		
		// Try to verify with a wrong data to make sure it works
		String test1 = "HHHeyJustTryIt";
		byte[] data1 = test1.getBytes();
			
		try{
			System.out.println("Creating key pair..");
			KeyPair keys = EECKeyGenerator.generateKeys();
			System.out.println("DONE");
			System.out.println("Signing..");
			signedData = signData(data, keys.getPrivate());
			System.out.println("DONE");
			System.out.println("The signature is: "+signedData);
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
