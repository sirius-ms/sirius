
package fragtreealigner.domainobjects.util;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Coordinate implements Serializable {
	private double x;
	private double y;
	
	public Coordinate(double x, double y) {
		this.setX(x);
		this.setY(y);
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getX() {
		return x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getY() {
		return y;
	}
	
	@Override
	public String toString() {
		return (x + "," + y);
	}
}
