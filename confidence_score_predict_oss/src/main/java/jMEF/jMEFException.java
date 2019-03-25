package jMEF;


/**
 * @author  Vincent Garcia
 * @author  Frank Nielsen
 * @version 1.0
 *
 * @section License
 * 
 * See file LICENSE.txt
 *
 * @section Description
 *
 * This class provides exception for the jMEF library.
 */
public class jMEFException extends Exception {

	
	/**
	 * Constant for serialization.
	 */
	private static final long serialVersionUID = 1L;

	
	/**
	 * Exception message.
	 */
	String msg;

	
	/**
	 * Class constructor.
	 * @param s exception message
	 */
	public jMEFException(String s){
		msg = s;
	}

	
	/**
	 * Method toString.
	 * @return string describing the exception
	 */
	public String toString(){
		return new String(super.toString() + " --> " + msg);   
	}

	
	/**
	 * Returns the detail message string of this exception.
	 * @return the detail message string of this exception
	 */
	public String getMessage(){
		return msg + "|" + super.getMessage();	
	}     

}
