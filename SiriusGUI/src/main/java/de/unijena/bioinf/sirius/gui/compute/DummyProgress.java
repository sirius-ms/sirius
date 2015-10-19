package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.sirius.Progress;

public class DummyProgress implements Progress{

	public DummyProgress() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void finished() {
		System.out.println("finished");
	}

	@Override
	public void info(String s) {
		System.out.println("info "+s);
	}

	@Override
	public void init(double d) {
		System.out.println("init "+d);
	}

	@Override
	public void update(double d1, double d2, String s3) {
		System.out.println("update "+d1+" "+d2+" "+s3);
	}

}
