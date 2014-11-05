package terminal.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class EECKeyGenerator {

	KeyPairGenerator g;
	KeyPair pair;

	/*
	 * Define a generator which produce a "prime192v1", size: 192bit
	 */
	public EECKeyGenerator() throws Exception {
		
			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
			ECGenParameterSpec ecGenSpec = new ECGenParameterSpec("c2pnb163v1");
			g = KeyPairGenerator.getInstance("ECDSA", "BC");
			g.initialize(ecGenSpec, new SecureRandom());
			pair = g.generateKeyPair();
	}

	public KeyPair getKeyPair() {
		return pair;
	}
	
	public PublicKey getPublicKey() {
		return pair.getPublic();
	}
	
	public PrivateKey getPrivateKey() {
		return pair.getPrivate();
	}
	
	public void setKeyPair(KeyPair pair){
		this.pair = pair;
	}
	
	public static KeyPair generateKeys() throws Exception{
		EECKeyGenerator gen = new EECKeyGenerator();
		KeyPair sc = gen.getKeyPair();
		return sc;
	}
	
	public static void saveKeys(KeyPair keys, String path, String carID) throws IOException{
		PrivateKey privateKey = keys.getPrivate();
		PublicKey publicKey = keys.getPublic();
		// Store Public Key.
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
		FileOutputStream fos = new FileOutputStream(path+"/"+carID+".pub");
		fos.write(x509EncodedKeySpec.getEncoded());
		fos.close();
		// Store Private Key.
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
		fos = new FileOutputStream(path+"/"+carID);
		fos.write(pkcs8EncodedKeySpec.getEncoded());
		fos.close();
	}
	
	public static KeyPair loadKeys(String algorithm, String path, String carID) throws IOException, NoSuchAlgorithmException,
	InvalidKeySpecException {
		// Read Public Key
		File filePublicKey = new File(path+"/"+carID+".pub");
		FileInputStream fis = new FileInputStream(path+"/"+carID+".pub");
		byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
		fis.read(encodedPublicKey);
		fis.close();
		// Read Private Key
		File filePrivateKey = new File(path+"/"+carID);
		fis = new FileInputStream(path+"/"+carID);
		byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
		fis.read(encodedPrivateKey);
		fis.close();
		// Regenerate KeyPair.
		KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
		PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
		return new KeyPair(publicKey, privateKey);
	}

	public void printPair() {
		System.out.println(pair.getPrivate());
		System.out.println(pair.getPublic());
	}
	
	// Testing purpose 
	// NOTE remember in the GUI to catch and manage "File already exists" exception -- now it is overwritten
	public static void main(String[] args) {
		for (int i=0; i <= 5; i++) {
			try {
				EECKeyGenerator gen = new EECKeyGenerator();
				System.out.println("Generating cars keys...");
				gen.printPair();
				saveKeys(gen.getKeyPair(),"keys/cars","car"+i);
				KeyPair test = loadKeys("ECDSA","keys/cars","car"+i);
				gen.setKeyPair(test);
				System.out.println("Keys restored");
				gen.printPair();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			EECKeyGenerator gen = new EECKeyGenerator();
			System.out.println("Generating company keys...");
			gen.printPair();
			saveKeys(gen.getKeyPair(),"keys/master", "company");
			KeyPair test = loadKeys("ECDSA","keys/master", "company");
			gen.setKeyPair(test);
			System.out.println("Keys restored");
			gen.printPair();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
}
