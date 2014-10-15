/**
 * 
 */
package vehicle.applet;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.Applet;
import javacard.framework.ISOException;
import javacard.framework.Util;

/**
 * @author javacard
 *
 */
public class VehicleApplet extends Applet {
	public static final byte STATUS_UNINITIALIZED = 0;
	public static final byte STATUS_INITIALIZED   = 1;

	private byte status = STATUS_UNINITIALIZED;

	public static final byte CLA_ISSUE           = (byte) 0xB0;
	public static final byte CLA_AUTH            = (byte) 0xB1;

	public static final byte CMD_INIT            = 0x00;
	public static final byte CMD_RESET           = 0x41;
	
	public static final byte CMD_READ_CARD_ID    = 0x50;
	public static final byte CMD_READ_VEHICLE_ID = 0x51;
	public static final byte CMD_READ_KILOMETERS = 0x52;
	public static final byte CMD_ADD_KILOMETERS  = 0x53;
	
	private short cardID     = 0;
	private short vehicleID  = 1337;
	private short kilometers = 9999;
	
	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new vehicle.applet.VehicleApplet().register(bArray,
				(short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		if(status == STATUS_UNINITIALIZED && buf[ISO7816.OFFSET_CLA] == CLA_ISSUE) {
			if(buf[ISO7816.OFFSET_INS] == CMD_INIT) {
				cardID = Util.getShort(buf, ISO7816.OFFSET_CDATA);
				status = STATUS_INITIALIZED;
			} else {
				// good practice: If you don't know the INStruction, say so:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			}
		} else {
			switch (buf[ISO7816.OFFSET_INS]) {
			case CMD_RESET:
				status = STATUS_UNINITIALIZED;
				cardID = 0;
				kilometers = 0;
				break;
			case CMD_READ_CARD_ID:
				Util.setShort(buf, (short) 0, cardID);
				apdu.setOutgoingAndSend((short) 0, (short) 2);
				break;
			case CMD_READ_VEHICLE_ID:
				Util.setShort(buf, (short) 0, vehicleID);
				apdu.setOutgoingAndSend((short) 0, (short) 2);
				break;
			case CMD_READ_KILOMETERS:
				Util.setShort(buf, (short) 0, kilometers);
				apdu.setOutgoingAndSend((short) 0, (short) 2);
				break;
			default:
				// good practice: If you don't know the INStruction, say so:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			}
		}
	}
}