package game;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import terminal.gui.VehicleTerminal;

/*
 * TODO:
 * - Kollision
 * - For Each e: in Paint bumper, kann falsch sein, da gerade die Liste modifiziert wird - anderer thread
 * - countdown kommt auch in terrainpaint vor - da updaten
 *  */


public class Quickgame extends Frame implements Runnable, KeyListener {

	private static final long serialVersionUID = -6440197294597461669L;
	
	private long delaytime;
	
	public static int mode;
	
	public static int countdown;
	
	public static Console console;

	public static int appletsize_x = 800; // Startgr��e des Applets in x - Richtung
	public static int appletsize_y = 640; // Gr��e des Applets in y - Richtung
	
	private Image dbImage; // Double Buffer
	private Graphics2D dbg;

	private int apl_width;

	private int apl_height;
	
	public Image i_hintergrund;
	
	public Image i_bumper;
	
	LinkedList<Bumper> list;

	public BufferedImage i_car1;
	public BufferedImage i_car2;
	
	public int bumperpos;	
	
	private int cam_pos_y;

	private Car car1, car2;
	
	private int score1, score2;
	
	private int m11_randomtime=0;
	private Boolean m14_randommode=false;
	
	private boolean running = true; 
	
	private VehicleTerminal terminal;
	public Quickgame(VehicleTerminal t) {
		terminal = t;
	}
	
	// Renderdata
	// Init
	public void init() {

		repaint(0, 0, 0, 300, 300);
		
		setBackground(new Color(30, 30, 30));
		// Gr��e
		//resize(appletsize_x, appletsize_y);
		
		// KeyListener
		addKeyListener(this);
		
		console = new Console(appletsize_y);
		//CLIENT=new Pclient(host);
		
		//Objekte laden
		
		
		i_hintergrund = new ImageIcon("res/hintergrund.gif").getImage();

		i_car1 = loadImages("car1.gif");
		i_car2 = loadImages("car2.gif");
		i_bumper = loadImages("roadblock.gif");
		
		//Sound.init(this);
		//Sound.start_music();
		this.setSize(800, 640);
		this.setVisible(true);
		final Quickgame frame = this;
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				frame.running = false;
				frame.setVisible(false);
				System.out.println(car1.get_distance());
				terminal.setKilometers((long)(car1.get_distance()/1000));
				//System.exit(0);
			}
		});
		newRound();
	}


	public BufferedImage loadImages(String input){
		URL url;
		BufferedImage img;
		try {
			// url = new URL(getCodeBase()+input);
			System.out.println("trying to load: " + input);
			img = ImageIO.read(new File("res/" + input));
			return img;
		} 
		catch (IOException e) { 
			System.out.print("could not load image");
			e.printStackTrace();
		}
		return null;
	}
	
	public void start() {
		resize(appletsize_x, appletsize_y);
		// Neuer Thread
		Thread th = new Thread(this);

		th.start();
	}



	public static void main(String[] args) {
		Quickgame g = new Quickgame(null);
		g.startCar();
	}

	public void startCar() {
		init();
		start();		
	}
	public void run() {
		while (running) { //MAIN LOOP
			// delay calculation startpoint
			long delay = System.nanoTime();
			
			move_cars();
			
			spawnBlocks(200);
			
			collision(car1);
			//collision(car2);
			
			if(mode==11)
			{
				randomLenk(car1);
				randomLenk(car2);
				m11_randomtime--;
			}
			
			checkCountdown();
			
			Console.update();
			
			repaint(0, 0, 0, appletsize_x, appletsize_x);
			
			checkVictory();

			delaytime = System.nanoTime() - delay;
			
			try { //SChLAF
				
				long delaysmall = delaytime / 1000000;
				if (delaysmall >= 0) {
					if (delaysmall < 16) {
						Thread.sleep(16 - delaysmall);
					}
				}
			}

			catch (InterruptedException ex) {
				// Do nothing
			}
		}
	}

	public void newRound()
	{
		double angle = Math.random() * Math.PI/2 - Math.PI/4;
		car1 = new Car(200, 500, angle);
		car2 = new Car(600, 500, -angle);
		bumperpos = 0;
		cam_pos_y = 0;
		list = new LinkedList<Bumper>();
		countdown = 180;
	}
	
	public void randomLenk(Car i_car)
	{
		if(m11_randomtime==0)
		{
			m11_randomtime= (int) (Math.random()*120+50);
		}
		if(m11_randomtime==1)
		{
			double j=(Math.random()*Math.PI-Math.PI/2.0);
			i_car.set_angle(i_car.get_angle()+j);
		}
	}
	
	public void Victory(int i)
	{
		if(i==1)
			score1++;
		else if(i==2)
			score2++;
		else
			return;
		Console.print(score1+" : "+score2, appletsize_x/2-60, 120, 30, 80);
		//Sound.stop_motor();
		newRound();
	}
	
	public void checkVictory()
	{
		/*
		//if(Math.abs(car1.get_y()-car2.get_y()) >appletsize_y )
		if(cam_pos_y+car1.get_y()>appletsize_y+20 || cam_pos_y+car2.get_y()>appletsize_y+20)
		{
			if(car1.get_y()<car2.get_y())
				Victory(1);
			else
				Victory(2);
		}
		*/
	}
	
	public void newMode(){
		/*
		int nextmode = mode;
		while(nextmode==mode)
		{
			nextmode=(int) Math.round(Math.random()*15.5);
		}
		mode = nextmode;
		//mode=14;
		if (!m14_randommode)
		{
			if (mode==3) Sound.change_song(Sound.SONG_WALZER);
			else Sound.change_song(Sound.SONG_DEFAULT);
			switch(mode) {
			case 0:
				Console.print("Standard", appletsize_x/2-180, 80, 50, 50);
				break;
			case 1:
				Console.print("Back to the future", appletsize_x/2-250, 80, 50, 50);
				break;
			case 2:
				Console.print("MAX Boing!", appletsize_x/2-140, 80, 50, 50);
				break;
			case 3:
				Console.print("Ice ice baby", appletsize_x/2-140, 80, 50, 50);
				break;
			case 4:
				Console.print("Wohoo See Ya!", appletsize_x/2-180, 80, 50, 50);
				break;
			case 5:
				Console.print("Doppelganger", appletsize_x/2-180, 80, 50, 50);
				break;
			case 6:
				Console.print("Left yields to right!", appletsize_x/2-250, 80, 50, 50);
				break;
			case 7:
				Console.print("Right, oh wait...!", appletsize_x/2-200, 80, 50, 50);
				break;
			case 8:
				Console.print("Standard again", appletsize_x/2-220, 80, 50, 50);
				break;
			case 9:
				Console.print("Pedal to the metal", appletsize_x/2-220, 80, 50, 50);
				break;
			case 10:
				Console.print("The elder scrolls", appletsize_x/2-220, 80, 50, 50);
				break;
			case 11:
				Console.print("Let's play a little game...", appletsize_x/2-280, 80, 50, 50);
				break;
			case 12:
				Console.print("Brickbraker", appletsize_x/2-100, 80, 50, 50);
				break;
			case 13:
				Console.print("Ballet of cars", appletsize_x/2-200, 80, 50, 50);
				break;
			case 14:
			case 15:
				Console.print("You don't know, Jack", appletsize_x/2-250, 80, 50, 50);
				m14_randommode=true;
				newMode();
				//Console.print("mode "+mode);
				m14_randommode=false;
				break;
			}
		}
		*/
	}
	
	public void checkCountdown()
	{
		if(countdown==180) 
		{
			//Sound.countdown();
			Console.print("3", appletsize_x/2-10, 300, 50, 29);
			mode=0;
		}
		if(countdown==120) 
			Console.print("2", appletsize_x/2-10, 300, 50, 29);
		if(countdown==60) 
			Console.print("1", appletsize_x/2-10, 300, 50, 29);
		if(countdown==1) 
		{
			//Sound.start_motor();
			Console.print("Go!", appletsize_x/2-20, 300, 50, 30);
			newMode();
			if(mode==5)
			{
				double angle=car1.get_angle();
				car1=new Car(600, 500, -angle);
				car2=new Car(200, 500, angle);
			}	
		}
		if(countdown>0) countdown--;
	}
	
	public void spawnBlocks(int frequency) //NEU
	{
		if(bumperpos==0)
		{
			bumperpos=cam_pos_y-frequency+10;
			//Console.print(""+bumperpos);
		}
		if(cam_pos_y-bumperpos>=frequency)
		{
			int type=(int) Math.round(Math.random()*5.2);
			switch (type){
			
			case 0:
				{
					list.push(new Bumper((int) (Math.random()*800), -cam_pos_y-200, (int) (Math.random()*100+50), (int) (Math.random()*100+50)));
					bumperpos+=frequency/2;
					break;
				}
			case 1:
				{
					list.push(new Bumper((int) (Math.random()*300), -cam_pos_y-200, (int) (Math.random()*100+50), (int) (Math.random()*100+50)));
					list.push(new Bumper((int) (Math.random()*200)+300, -cam_pos_y-200, (int) (Math.random()*100+50), (int) (Math.random()*100+50)));
					list.push(new Bumper((int) (Math.random()*300)+500, -cam_pos_y-200, (int) (Math.random()*100+50), (int) (Math.random()*100+50)));
					bumperpos+=frequency;
					break;
				}
				
			case 2:
				{
					for(int i=0; i<4; i++)
					{
						list.push(new Bumper((int) (Math.random()*80)+200*i, -cam_pos_y-200, 80, (int) (Math.random()*20+50)));
					}
					bumperpos+=frequency;
					break;
				}
			case 3:
				{
					int size=(int) (Math.random()*100+50);
					list.push(new Bumper(0, -cam_pos_y-200, size , size));
					list.push(new Bumper(appletsize_x-size, -cam_pos_y-150, size , size));
					bumperpos+=frequency;
					break;
				}
			case 4:
				{
					int size=(int) (Math.random()*200+150);
					list.push(new Bumper(size, -cam_pos_y-220, appletsize_x-2*size , size/2));
					bumperpos+=frequency*1.5;
					break;
				}
			case 5:
				{
					list.push(new Bumper((int) (Math.random()*220), -cam_pos_y-200, (int) (Math.random()*100+80), (int) (Math.random()*100+50)));
					list.push(new Bumper((int) (Math.random()*220)+400, -cam_pos_y-200, (int) (Math.random()*100+80), (int) (Math.random()*100+50)));
					bumperpos+=frequency;
					break;
				}
			
			//Console.print("spawn "+bumperpos);
			}
		}
	}

	public void collision(Car i_car)
	{
		int box_m_x;
		int box_m_y;
		
		for(Bumper e: list)
		{
			if(i_car.get_x()>e.x-i_car.get_radius() && i_car.get_x()<e.x+e.width+i_car.get_radius())
				if(i_car.get_y()>e.y-i_car.get_radius() && i_car.get_y()<e.y+e.height+i_car.get_radius())
			{
				//Console.print("bounce!", i_car.get_x(), i_car.get_y()+cam_pos_y, 12, 50);
				//Sound.bounce();
				box_m_x=e.x+e.width/2;
				box_m_y=e.y+e.height/2;
				
				if(Math.floor(i_car.get_y()-box_m_y)==0 || Math.abs((i_car.get_x()-box_m_x)/(i_car.get_y()-box_m_y)) > (e.width/e.height)) //Seitlich
				{
					if (i_car.get_x() < box_m_x) i_car.bounce_left(e.x-i_car.get_radius()); // von links
					else i_car.bounce_right(e.x+e.width+i_car.get_radius()); //von rechts
				}
				else
				{
					if (i_car.get_y() < box_m_y) i_car.bounce_top(e.y-i_car.get_radius()); // von oben
					else i_car.bounce_bottom(e.y+e.height+i_car.get_radius()); //von unten
				}
				
				if(mode==12) // BRICKBRAKER modus
				{
					if(e.get_time()==0) e.set_time(60);
				}
				break;
			}
		}
		
		if(i_car.get_x()<i_car.get_radius()) {
			//Sound.bounce();
			i_car.bounce_right(i_car.get_radius());
		} else if (i_car.get_x()>appletsize_x-i_car.get_radius()) {
			//Sound.bounce();
			i_car.bounce_left(appletsize_x-i_car.get_radius());
		}
	}

	public void paint(Graphics2D g) {
		terrainpaint(g);

		bumperpaint(g);
		
		debugpaint(g);
		
		AffineTransform tx;
		AffineTransformOp op;
		
		tx = AffineTransform.getRotateInstance(car1.get_angle());
		op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
		//if(mode!=5)
			g.drawImage(i_car1, op, car1.get_x() + (int)(Math.sin(car1.get_angle() - 0.5404) * 29.15), car1.get_y() - (int)(Math.cos(car1.get_angle() - 0.5404) * 29.15)+cam_pos_y);
		//else
		//	g.drawImage(i_car2, op, car1.get_x() + (int)(Math.sin(car1.get_angle() - 0.5404) * 29.15), car1.get_y() - (int)(Math.cos(car1.get_angle() - 0.5404) * 29.15)+cam_pos_y);
		/*
		tx = AffineTransform.getRotateInstance(car2.get_angle());
		op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
		if(mode!=5)
			g.drawImage(i_car2, op, car2.get_x() + (int)(Math.sin(car2.get_angle() - 0.5404) * 29.15), car2.get_y() - (int)(Math.cos(car2.get_angle() - 0.5404) * 29.15)+cam_pos_y);
		else
			g.drawImage(i_car1, op, car2.get_x() + (int)(Math.sin(car2.get_angle() - 0.5404) * 29.15), car2.get_y() - (int)(Math.cos(car2.get_angle() - 0.5404) * 29.15)+cam_pos_y);
		*/
		Console.paint(g);
	}

	public void terrainpaint(Graphics2D g)
	{
		Car car2 = car1;
		int y= (int) ((car1.get_y()+car2.get_y())/2);
		if(mode==10) 
		{
			if(countdown==180) cam_pos_y=-y+450 - (int) (120*(Math.abs((car1.get_y()-car2.get_y())/600.0)));
			if(countdown==0) 
				{
				if(-car1.get_y()>cam_pos_y-40) cam_pos_y=-car1.get_y()+40;
				else if(-car2.get_y()>cam_pos_y-40) cam_pos_y=-car2.get_y()+40;
				else cam_pos_y+=2;
				}
		}
		else
		if(cam_pos_y>-appletsize_y) cam_pos_y=-y+450 - (int) (120*(Math.abs((car1.get_y()-car2.get_y())/600.0)));
		
		int y2=(cam_pos_y+2*appletsize_y)%appletsize_y;
		g.drawImage(i_hintergrund, 0, y2, this);
		g.drawImage(i_hintergrund, 0, y2-appletsize_y, this);
		
	}
	
	public void debugpaint(Graphics g) //NEU
	{
		g.setFont(new Font("Arial", Font.PLAIN, 12));
//		g.setColor(Color.white);
//		g.drawString("Pos y: "+cam_pos_y, 20, 20);
//		g.drawString("bumper y: "+bumperpos, 90, 20);
//		g.drawString("no."+list.size(), 180, 20);
//		g.drawString("delay "+(delaytime / 1000), 220, 20 );
		g.drawString("delay "+(delaytime / 1000), 20, 20 );
		//g.drawString(" "+score1+" : "+score2, 20, 60);
		g.drawString((int)(car1.get_distance()/1000) + "km", 20, 60);
	}
	
	public void bumperpaint(Graphics g) //NEU
	{
		try {
		for(Bumper e: list)
		{
			
			if(-e.y > cam_pos_y-appletsize_y ) //Teste ob es noch im bildschirm ist
			{
				g.drawImage(i_bumper, e.x, e.y+cam_pos_y, e.width, e.height, this);
				
				if(mode==12 && e.get_time()!=0)
				{
					int i=e.get_time();
					i--;
					e.set_time(i);
					
					if(i==1) 
						{
						list.remove(e);
						Console.print("pop!",e.x+e.width/2-8, e.y+e.height/2-2+cam_pos_y,12,50);
						}
				}
			}
			// g.drawString(""+e.y, e.x, e.y+cam_pos_y+20); //Debug, zeichen position
			else
			{
				list.remove(e);
			}
		}
		} catch (java.util.ConcurrentModificationException e) {
			// don't care
		}
	}	

	private void move_cars() {
		car1.move();
		//car2.move();
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		if(countdown==0)
		{
			switch(arg0.getKeyChar()) {
			case 'w':
				if(mode!=1) car1.goForward(); 
				else car1.goBrake();
				break;
			case 'a':
				if(mode==7) car1.steerRight();
				else car1.steerLeft();
				break;
			case 's':
				if(mode!=1) car1.goBrake();
				else car1.goForward();
				break;
			case 'd':
				if(mode==7) car1.steerLeft();
				else if(mode!=6) car1.steerRight();
				break;
/*				
			case 'i':
				if(mode!=1) car2.goForward();
				else car2.goBrake();
				break;
			case 'j':
				if(mode==7) car2.steerRight();
				else car2.steerLeft();
				break;
			case 'k':
				if(mode!=1) car2.goBrake();
				else car2.goForward();
				break;
			case 'l':
				if(mode==7) car2.steerLeft();
				else if(mode!=6) car2.steerRight();
				break;
*/
			case 'h':
				//Sound.honk();
				break;
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		switch(arg0.getKeyChar()) {
		case 'w':
			if(mode==9 && car1.get_forwardspeed()>1)
			{
				Console.print("Player 1 left the pedal!", appletsize_x/2-120, 500, 18, 50);
				Victory(2);
			}
		case 's':
			car1.goRelease();
			break;
		case 'a':
		case 'd':
			if(mode!=13) car1.steerRelease();
			break;
/*			
		case 'i':
			if(mode==9 && car2.get_forwardspeed()>1)
			{
				Console.print("Player 2 left the pedal!", appletsize_x/2-120, 500, 18, 50);
				Victory(1);
			}
		case 'k':
			car2.goRelease();
			break;
		case 'j':
		case 'l':
			if(mode!=13) car2.steerRelease();
			break;
*/
		}
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
	}
	
	public void update(Graphics g) {
		
		if (dbImage == null) {
			
			// Gr��e des Applets
			apl_width = getSize().width;
			apl_height = getSize().height;
			dbImage = createImage(apl_width, apl_height);
			dbg = (Graphics2D) dbImage.getGraphics();
			

			RenderingHints rh = new RenderingHints(
		             RenderingHints.KEY_TEXT_ANTIALIASING,
		             RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		    dbg.setRenderingHints(rh);

		}
		//dbg.setColor(getBackground());
		//dbg.fillRect(0, 0, apl_width, apl_height);

		//dbg.setColor(getForeground());
		paint(dbg);

		g.drawImage(dbImage, 0, 0, this);
		//paint(g);
	}
}
