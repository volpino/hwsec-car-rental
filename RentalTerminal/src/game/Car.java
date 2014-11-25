package game;

public class Car {

	static final double ACCEL = 0.15;
	static final double DECEL = 0.09;
	static final double DRIFT = 0.20; // h�herer Wert = weniger Drift
	static final double STEER = 0.08;
	
	static final double WIDTH = 30;
	static final double HEIGHT = 50;
	
	static final int MAXSPEED=10;
	
	static final double RADIUS = Math.sqrt(WIDTH*WIDTH + HEIGHT*HEIGHT) / 2.6;

	private double angle, angle_speed, drive, distance;
	private Vector pos, speed;

	public Car(double initial_x, double initial_y, double angle) {
		pos = new Vector(initial_x, initial_y);
		speed = new Vector(0, 0);
		this.angle = angle;
		angle_speed = 0;
		drive = 0;
		distance = 0;
	}
	
	public int get_x() {
		return (int) pos.x;
	}
	
	public int get_y() {
		return (int) pos.y;
	}
	
	public Vector get_speed() {
		return speed;
	}
	
	public double get_angle() {
		return angle;
	}
	
	public void set_angle(double i_angle) {
		angle=i_angle;
	}
	
	public double get_radius() {
		return RADIUS;
	}
	
	public void goForward() {
		if(Quickgame.mode==4)
			drive = ACCEL*2;
		else
			drive = ACCEL;
	}
	public void goRelease() {
		drive = 0;
	}
	public void goBrake() {
		drive = -ACCEL;
	}
	public void steerLeft() {
		angle_speed = -STEER;
	}
	public void steerRight() {
		angle_speed = STEER;
	}
	public void steerRelease() {
		angle_speed = 0;
	}
	
	public void bounce_top(double bounce_at_y) {
		if (speed.y > 0) bounce_horiz(bounce_at_y);
	}
	public void bounce_bottom(double bounce_at_y) {
		if (speed.y < 0) bounce_horiz(bounce_at_y);
	}
	public void bounce_left(double bounce_at_x) {
		if (speed.x > 0) bounce_vert(bounce_at_x);
	}
	public void bounce_right(double bounce_at_x) {
		if (speed.x < 0) bounce_vert(bounce_at_x);
	}
	private void bounce_horiz(double bounce_at_y) {
		pos.y -= 2 * (pos.y - bounce_at_y);
		if(Quickgame.mode==2)
			speed.y = -speed.y*2.0;
		else
			speed.y = -speed.y;
	}
	private void bounce_vert(double bounce_at_x) {
		pos.x -= 2 * (pos.x - bounce_at_x);
		if(Quickgame.mode==2)
			speed.x = -speed.x*2.0;
			final double v = speed.length();
			if (v > 20) speed.scale(20 / v);
		else
			speed.x = -speed.x;
	}
	public double get_forwardspeed() {
		return speed.product(new Vector(Math.sin(angle), -Math.cos(angle)));
	}
	
	public void move() {
		angle += angle_speed;
		
		final Vector direction = new Vector(Math.sin(angle), -Math.cos(angle));
		final Vector normal = direction.normal();
		final double forwardspeed = speed.product(direction);
		
		if (forwardspeed > DECEL) {
			speed.add(-DECEL * direction.x, -DECEL * direction.y);
		} else if (forwardspeed < -DECEL) {
			speed.add(DECEL * direction.x, DECEL * direction.y);
		} else {
			speed.add(-forwardspeed * direction.x, -forwardspeed * direction.y);
		}
		final double driftspeed = speed.product(normal);

		double drift;
		if(Quickgame.mode==3)
			drift=DRIFT/10.0;
		else
			drift=DRIFT;

		if (driftspeed > drift) {
			speed.add(-drift * normal.x, -drift * normal.y);
		} else if (driftspeed < -drift) {
			speed.add(drift * normal.x, drift * normal.y);
		} else {
			speed.add(-driftspeed * normal.x, -driftspeed * normal.y);
		}

		if(Math.abs(forwardspeed) < MAXSPEED) speed.add(drive * direction.x, drive * direction.y);

		pos.add(speed);
		distance += speed.length();
	}

	public Double get_distance() {
		// TODO Auto-generated method stub
		return distance;
	}
}