package game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class Console {
	private static int pos_y;
	private static Info[] info;
	private static int[] infotime;
	private final static int infodelay = 300; //60~1 sec
	private final static int infoamount = 20;

	public static boolean open = false;

	// evtl current info amount - schont die for schleife

	public Console(int y) {
		pos_y = y;
		info = new Info[infoamount];
		infotime = new int[infoamount];
	}

	public static void print(String str) {
		for (int i = infoamount - 1; i >= 0; --i) {
			if (i + 1 < infoamount) {
				// Verschiebe alles nach oben, wenn neue Nachricht
				
				info[i + 1] = info[i];
				if(info[i]!=null && info[i+1]!=null)
					if(info[i].getType()==false)
					info[i+1].setY(pos_y - 20 * (i+1) - 11);
			}
		}
		info[0] = new Info(str, 9, pos_y -11, 12, infodelay, false);
	}
	
	public static void print(String str, int x, int y, int fontsize, int time)
	{
		for (int i = infoamount - 1; i >= 0; --i) {
			if (i + 1 < infoamount) {
				info[i + 1] = info[i];
			}
		}
		info[0] = new Info(str, x, y, fontsize, time, true);
		
	}
	
	
	public static void update()
	{
		for (int i = 0; i < infoamount; i++) {
			if (info[i] != null)
			info[i].time();}
	}

	public static void paint(Graphics g) {

		for (int i = 0; i < infoamount; i++) {
			//infotime[i]--;
			if (info[i] != null)
			if (info[i].getTime() > 0)
				 {
				    g.setFont(new Font("Arial", Font.PLAIN, info[i].getFontSize()));
					g.setColor(Color.black);
					g.drawString(info[i].getText(), info[i].getX() , info[i].getY());
					g.setColor(Color.white);
					g.drawString(info[i].getText(), info[i].getX() , info[i].getY()+1);
				}
		}

	}
}

class Info
{
	private String text;
	private int x;
	private int y;
	private int fontsize;
	private int time;
	private Boolean type;
	
	public Info(String itext, int ix, int iy, int ifontsize, int itime, Boolean itype)
	{
		text=itext;
		x=ix;
		y=iy;
		fontsize=ifontsize;
		time=itime;
		type=itype;
	}
	
	public String getText()
	{
		return text;
	}
	
	public int getTime()
	{
		return time;
	}
	
	public void time()
	{
		time--;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public void setY(int iy)
	{
		y=iy;
	}
	
	public int getFontSize()
	{
		return fontsize;
	}
	
	public Boolean getType()
	{
		return type;
	}

}
