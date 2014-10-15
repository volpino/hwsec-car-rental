package terminal.crypto;

import java.security.*;
import java.security.spec.*;

import javax.crypto.KeyAgreement;

public class EECKeyGenerator {

	KeyPairGenerator kpg;
	EllipticCurve curve;
	ECParameterSpec ecSpec;
	KeyPair aKeyPair;
	KeyAgreement aKeyAgree;
	KeyPair bKeyPair;
	KeyAgreement bKeyAgree;
	KeyFactory keyFac;

	public EECKeyGenerator() {
		try {

			aKeyPair = this.kpg.generateKeyPair();
			aKeyAgree = KeyAgreement.getInstance("ECDH", "BC");
			aKeyAgree.init(aKeyPair.getPrivate());

			aKeyAgree.doPhase(bKeyPair.getPublic(), true);
			
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
