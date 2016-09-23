package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.reduction;

/**
 * Created with IntelliJ IDEA.
 * User: B. Seelbinder
 * UID:  ga25wul
 * Date: 03.04.2014
 * Time: 01:56
 * *
 */

import org.slf4j.LoggerFactory;

import java.util.LinkedList;

/**
 * Interface holding some method declarations
 *
 * USAGE OF IMPLEMENTING COMMANDS IN THIS COMMAND-PARSER
 * ~ executeMethod MUST return Ci + relative, relative {@literal >=} 0
 * ~ Ci shall be increase equally by the amount of arguments a command used {@literal =>} increase relative by every argument used
 * ~ check, if the argument list 'arg' is large enough to contain arguments, if you need them
 * ~ description is the method that is called whenever 'help' is used or you feel like telling the user how to use the command
 */
public abstract class ACommandable {

	protected static boolean bDebug = false;
	private static LinkedList<ACommandable> CMDS = new LinkedList<ACommandable>(  );

	protected final boolean bMeasureTime;
	private LinkedList<Long> runtimes = new LinkedList<Long>(  );
	private long runtime = 0; // in milliseconds

	////////////////////////////
	/// --- CONSTRUCTORS --- ///

	public ACommandable() {
		CMDS.add( this );
		bMeasureTime = false;
	}

	public ACommandable( boolean doMeasureTime ) {
		CMDS.add( this );
		bMeasureTime = doMeasureTime;
	}

	///////////////////////////
	/// --- METHOD AREA --- ///

	protected final int execute( String[] arg, int Ci ) {

		// execution area
		if ( bMeasureTime ) {

			long start = System.currentTimeMillis();
			int r = executeMethod( arg, Ci ); // must be overridden in sub-classes!
			long after = System.currentTimeMillis();

			// time calculation and saving
			runtimes.add( after - start );
			runtime += after - start;

			if ( ACommandable.bDebug ) {

				if ( after - start == 0 )
					LoggerFactory.getLogger(this.getClass()).error( " Some command has a runtime of 0 ms. It will not be recognized for speed! " );

				if ( !isConsistent() ) {
					LoggerFactory.getLogger(this.getClass()).error( getInconsistencyErrorMessage() );
					throw new RuntimeException( " Inconsistency exception! " );
				}
			}

			return r;
		} else {

			// no time measure
			int r = executeMethod( arg, Ci ); // must be overridden in sub-classes!
			if ( ACommandable.bDebug ) {
				// it is still possible that there must be a consistency check
				if ( !isConsistent() ) {
					LoggerFactory.getLogger(this.getClass()).error( getInconsistencyErrorMessage() );
					throw new RuntimeException( " Inconsistency exception! " );
				}
			}

			return r;
		}
	}

	/**
	 * - this might be overridden by commands that act on the graph and that might use things that can change over time
	 * - do consider unexpected changed from different programmers, too!
	 * @return: true, if the test is successful
	 */
	protected boolean isConsistent() {
		return true;
	}

	/**
	 * - if useDebug is TRUE, this will cause some additional, normally unnecessary checkups
	 * @param useDebug
	 */
	protected static void setDebugging( boolean useDebug ) {
		ACommandable.bDebug = useDebug;
	}

	protected static boolean isbDebugging() {
		return ACommandable.bDebug;
	}

	/**
	 * Message that will be printed when 'isConsistent' returned 'false'
	 * @return
	 */
	protected String getInconsistencyErrorMessage() {
		return " >|< Command consistency check failed! >|< ";
	}

	static LinkedList<ACommandable> getCMDS() {
		return ACommandable.CMDS;
	}

	/////////////////////////////
	/// --- ABSTRACT AREA --- ///

	/**
	 * that is the actual execution that will be changed in sub-classes
	 * @param arg: argument list; provided by command parser ( by default )
	 * @param Ci: command index, 0 {@literal <=} Ci {@literal <} arg.length; provided by command parser ( by default )
	 * @return: return Ci, if no additional command is used; else, increase Ci for every argument you take from 'arg'
	 *          and return the result
	 */
	protected abstract int executeMethod( String[] arg, int Ci );

	protected abstract String description( );

	//////////////////////////////
	/// --- RUNTIME ACCESS --- ///

	/**
	 * return every runtime from every single execution
	 * @return
	 */
	final long[] getIndependentRuntimes() {

		long[] res = new long[runtimes.size()];
		int i=0;
		for ( Long I : runtimes )
			res[i++] = I;

		return res;
	}

	/**
	 * return full runtime
	 * @return
	 */
	final long getRuntime() {
		return runtime;
	}

	/**
	 * self-explanatory
	 * @return
	 */
	final boolean wasExecuted() {
		return bMeasureTime && ( runtime != 0 );
	}

	/**
	 * reset runtime of every command that is stored here
	 */
	final static void resetRuntime() {

		System.out.println(" Resetting runtime of every commandable object instance! ");

		for ( ACommandable cmd : ACommandable.CMDS ) {
			cmd.reset();
		}

	}

	/**
	 * reset to initial values
	 */
	private final void reset() {

		runtimes = new LinkedList<Long>(  );
		runtime = 0;
	}
}
