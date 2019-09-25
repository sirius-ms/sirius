package de.unijena.bioinf.babelms.load;

public class DoubleTest {

	public static void main(String[] args){
//		Pattern pattern = Pattern.compile("[-+]?(([0-9]*\\.?[0-9]+)|([0-9]+\\.{1}))([eE][-+]?[0-9]+)?");
//		System.out.println(pattern.matcher("9.e4").matches());
		
		String test = "9.67,567.4 	, 456.2";
		String[] splits = test.split("(\\s|[;,])");
		for(String s : splits) System.out.println("\""+s+"\"");
	}

}
