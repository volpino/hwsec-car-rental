package game;


public class Bumper {
	public int x;
	public int y;
	public int width;
	public int height;
	public int time=0;
	
	public Bumper(int ix, int iy, int iwidth, int iheight)
	{
		x=ix;
		y=iy;
		width=iwidth;
		height=iheight;
	}
	
	public void set_time(int i)
	{
		time=i;
	}
	
	public int get_time()
	{
		return time;
	}

}


//