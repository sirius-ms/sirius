
package fragtreealigner.domainobjects.chem.basics;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Element implements Serializable {
	private String symbol;
	private String name;
	private double mass;
	private int numberOfBondingElectrons;
	
	public Element(String symbol, String name, double mass, int numberOfBondingElectrons) {
		super();
		this.symbol = symbol;
		this.name = name;
		this.mass = mass;
		this.numberOfBondingElectrons = numberOfBondingElectrons;
	}

	public Element(String symbol) {
		super();
		this.symbol = symbol;
	}

	public String getSymbol() {
		return symbol;
	}
	
	public String getName() {
		return name;
	}
	
	public double getMass() {
		return mass;
	}

	public int getNumberOfBondingElectrons() {
		return numberOfBondingElectrons;
	}
}
