package game;

//import javax.sound.sampled.*;
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Frame;
import java.text.DecimalFormat;

public class Sound {

	public static final int SONG_DEFAULT = 0;
	public static final int SONG_WALZER = 1;

	private static AudioClip song_default, sound_honk;
	private static AudioClip[] sound_bump = new AudioClip[20];

	public static void init(Frame applet) {
		/*
		System.out.println("Sound init...");
		song_default = applet.getAudioClip(applet.getCodeBase(), "sounds/music.wav");
		sound_honk = applet.getAudioClip(applet.getCodeBase(), "sounds/honk.wav");
		DecimalFormat df = new DecimalFormat("00");
		for (int i=0; i<=19; i++) {
			sound_bump[i] = applet.getAudioClip(applet.getCodeBase(), "sounds/bump"+df.format(i)+".wav");
		}
		*/
	}

	public static void start_music() {
		song_default.loop();
	}
	
	public static void start_motor() {
	}
	
	public static void stop_motor() {
	}
	
	public static void change_song(int new_song) {
		//if (new_song==SONG_WALZER) {
		//}
	}
	
	public static void countdown() {
	}

	public static void bounce() {
		int n = (int)(Math.random() * 20);
		sound_bump[n].play();
	}
	
	public static void honk() {
		sound_honk.play();
	}
}