package terminal.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;


public class ECCKeyGenerator {
	public final static String CURVE = "c2pnb163v1";
	
	KeyPairGenerator g;
	KeyPair pair;
	
	static {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

	/*
	 * Define a generator which produce a "prime192v1", size: 192bit
	 */
	public ECCKeyGenerator() throws Exception {		
		ECGenParameterSpec ecGenSpec = new ECGenParameterSpec(CURVE);
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
		ECCKeyGenerator gen = new ECCKeyGenerator();
		KeyPair sc = gen.getKeyPair();
		return sc;
	}
	
	public static void saveKeys(KeyPair keys, String path, String keyName) throws IOException{
		savePublicKey(keys, path, keyName);

		// Store Private Key.
		PrivateKey privateKey = keys.getPrivate();
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
		FileOutputStream fos = new FileOutputStream(path+"/"+keyName);
		fos.write(pkcs8EncodedKeySpec.getEncoded());
		fos.close();
	}
	
	public static void savePublicKey(KeyPair keys, String path, String keyName) throws IOException{
		PublicKey publicKey = keys.getPublic();
		// Store Public Key.
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
		FileOutputStream fos = new FileOutputStream(path+"/"+keyName+".pub");
		fos.write(x509EncodedKeySpec.getEncoded());
		fos.close();
	}
	
	public static PublicKey loadPublicKey(String path, String keyName) throws IOException, NoSuchAlgorithmException,
	InvalidKeySpecException {
		// Read Public Key
		File filePublicKey = new File(path+"/"+keyName+".pub");
		FileInputStream fis = new FileInputStream(path+"/"+keyName+".pub");
		byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
		fis.read(encodedPublicKey);
		fis.close();
		// Regenerate KeyPair.
		KeyFactory keyFactory = KeyFactory.getInstance("ECDSA");
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
		return publicKey;
	}
	
	// Taken from https://bitcointalk.org/index.php?topic=2899.0
	public static PublicKey decodeKey(byte[] encodedW) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
	    ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec(CURVE);
	    KeyFactory keyfactory = KeyFactory.getInstance("ECDSA", "BC");
	    ECCurve curve = params.getCurve();
	    EllipticCurve ellipticCurve = EC5Util.convertCurve(curve, params.getSeed());
	    ECPoint point = ECPointUtil.decodePoint(ellipticCurve, encodedW);
	    ECParameterSpec params2 = EC5Util.convertSpec(ellipticCurve, params);
	    ECPublicKeySpec keySpec = new java.security.spec.ECPublicKeySpec(point, params2);
	    return keyfactory.generatePublic(keySpec);
	}
	
	public static KeyPair loadKeys(String path, String keyName) throws IOException, NoSuchAlgorithmException,
	InvalidKeySpecException {
		PublicKey publicKey = loadPublicKey(path, keyName);

		// Read Private Key
		File filePrivateKey = new File(path+"/"+keyName);
		FileInputStream fis = new FileInputStream(path+"/"+keyName);
		byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
		fis.read(encodedPrivateKey);
		fis.close();
		// Regenerate KeyPair.
		KeyFactory keyFactory = KeyFactory.getInstance("ECDSA");
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
		PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

		return new KeyPair(publicKey, privateKey);
	}

	public void printPair() {
		System.out.println(pair.getPrivate());
		System.out.println(pair.getPublic());
	}
	
	// Testing purposes
	public static void main(String[] args) {
		for (int i=0; i <= 5; i++) {
			try {
				ECCKeyGenerator gen = new ECCKeyGenerator();
				System.out.println("Generating cars keys...");
				gen.printPair();
				saveKeys(gen.getKeyPair(),"keys/cars","car"+i);
				KeyPair test = loadKeys("keys/cars","car"+i);
				gen.setKeyPair(test);
				System.out.println("Keys restored");
				gen.printPair();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		try {
			ECCKeyGenerator gen = new ECCKeyGenerator();
			System.out.println("Generating company keys...");
			gen.printPair();
			saveKeys(gen.getKeyPair(),"keys/master", "company");
			KeyPair test = loadKeys("keys/master","company");
			gen.setKeyPair(test);
			System.out.println("Keys restored");
			gen.printPair();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
}
