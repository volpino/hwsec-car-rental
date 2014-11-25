package terminal.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Collection of helpers for getting and storing certificate counters
 * 
 * @author Federico Scrinzi
 *
 */
public class CertCounter {
	/**
	 * Generates a new company certificate counter
	 * 
	 * @return the newly generated counter
	 * @throws IOException
	 */
	public static long getNewCounter() throws IOException {
		String path = "data/master/company.counter";
		long result;
		try {
			Scanner in = new Scanner(new FileReader(path));
			result = in.nextLong();
			in.close();
		} catch (FileNotFoundException e) {
			result = 0;
		}
		FileWriter wr = new FileWriter(path);
		wr.write(String.valueOf(result + 1));
		wr.close();
		return result;
	}
	
	/**
	 * Gets the last seen certcounter for the given vehicle
	 * 
	 * @param carID
	 * @return
	 */
	public static long getCarCounter(String carID) {
		Scanner in;
		try {
			in = new Scanner(new FileReader("data/cars/" + carID + ".counter"));
		} catch (FileNotFoundException e) {
			return 0;
		}
		long ret = in.nextLong();
		in.close();
		return ret;
	}
	
	/**
	 * Sets the last seen certcounter for the given vehicle
	 * 
	 * @param carID
	 * @param counter
	 * @throws IOException
	 */
	public static void setCarCounter(String carID, long counter) throws IOException {
		FileWriter wr = new FileWriter("data/cars/" + carID + ".counter");
		wr.write(String.valueOf(counter));
		wr.close();
	}
}
