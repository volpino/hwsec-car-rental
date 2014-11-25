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


/**
 * Collection of helpers for ECC key operations
 * 
 * @author Alessio Parzian
 * @author Moritz Muller
 *
 */
public class ECCKeyGenerator {
	public final static String CURVE = "c2pnb163v1";
	
	KeyPairGenerator g;
	KeyPair pair;
	
	static {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

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
	
	/**
	 * Generate a new ECC keypair
	 * 
	 * @return the generated keypair
	 * @throws Exception
	 */
	public static KeyPair generateKeys() throws Exception{
		ECCKeyGenerator gen = new ECCKeyGenerator();
		KeyPair sc = gen.getKeyPair();
		return sc;
	}
	
	/**
	 * Save the keypair in a file
	 * 
	 * @param keys the keypair we want to store
	 * @param path path where to store the keys
	 * @param keyName filename prefix of the output files
	 * @throws IOException
	 */
	public static void saveKeys(KeyPair keys, String path, String keyName) throws IOException{
		savePublicKey(keys, path, keyName);

		// Store Private Key.
		PrivateKey privateKey = keys.getPrivate();
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
		FileOutputStream fos = new FileOutputStream(path+"/"+keyName);
		fos.write(pkcs8EncodedKeySpec.getEncoded());
		fos.close();
	}
	
	/**
	 * Save a public key in a file using X509 encoding
	 * 
	 * @param keys the keypair with the public key to save
	 * @param path path where to store the keys
	 * @param keyName filename prefix of the output files
	 * @throws IOException
	 */
	public static void savePublicKey(KeyPair keys, String path, String keyName) throws IOException{
		PublicKey publicKey = keys.getPublic();
		// Store Public Key.
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
		FileOutputStream fos = new FileOutputStream(path+"/"+keyName+".pub");
		fos.write(x509EncodedKeySpec.getEncoded());
		fos.close();
	}
	
	/**
	 * Loads a public key from a file in X509 encoding
	 * 
	 * @param path path where to load the keys
	 * @param keyName filename prefix of the input files
	 * @return a public key object
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
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
	
	/**
	 * 
	 * Decodes a byte array that contains an ECC public key in ASN.1 X9.62 format
	 * 
	 * Taken from https://bitcointalk.org/index.php?topic=2899.0
	 * 
	 * @param encodedW ECC key in ASN.1 format (0x04 || Xcoord || Ycoord)
	 * @return
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 */	
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
	
	/**
	 * Load a keypair from files
	 * 
	 * @param path path where to load the keys
	 * @param keyName filename prefix of the input files
	 * @return a keypair object
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
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

	/**
	 * Debugging method for printing key data
	 */
	private void printPair() {
		System.out.println(pair.getPrivate());
		System.out.println(pair.getPublic());
	}
	
	/**
	 * Debugging and testing method to generate keypairs and save them to a file
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		for (int i=0; i <= 5; i++) {
			try {
				ECCKeyGenerator gen = new ECCKeyGenerator();
				System.out.println("Generating cars keys...");
				gen.printPair();
				saveKeys(gen.getKeyPair(),"data/cars","car"+i);
				KeyPair test = loadKeys("data/cars","car"+i);
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
			saveKeys(gen.getKeyPair(),"data/master", "company");
			KeyPair test = loadKeys("data/master","company");
			gen.setKeyPair(test);
			System.out.println("Keys restored");
			gen.printPair();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
}
