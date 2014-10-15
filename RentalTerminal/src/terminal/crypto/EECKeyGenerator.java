package terminal.crypto;

import java.security.*;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec; 

public class EECKeyGenerator {

	KeyPairGenerator g;
	KeyPair pair;

	public EECKeyGenerator() {
		try {
			ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("prime192v1");
			KeyPairGenerator g = KeyPairGenerator.getInstance("ECDSA", "BC");
			g.initialize(ecSpec, new SecureRandom());
			pair = g.generateKeyPair();

			
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Testing purpose
	 * 
	 * @return Returns EEC keyPair
	 */
	public KeyPair getKeyPair(){
		return pair;
	}

}
