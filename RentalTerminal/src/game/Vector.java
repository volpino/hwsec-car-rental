package game;

public class Vector {
	public double x, y;
	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}
	public void add(Vector v) {
		this.add(v.x, v.y);
	}
	public void add(double add_x, double add_y) {
		x += add_x;
		y += add_y;
	}
	public void scale(double k) {
		x *= k;
		y *= k;
	}
	public double length() {
		return Math.sqrt(x*x + y*y);
	}
	public double product(Vector v) {
		return this.x * v.x + this.y * v.y;
	}
	public Vector normal() {
		return new Vector(-y, x);
	}
}