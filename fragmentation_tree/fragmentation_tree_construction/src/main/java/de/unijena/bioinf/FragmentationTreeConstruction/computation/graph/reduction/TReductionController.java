package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.reduction;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.GraphReduction;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: B. Seelbinder
 * UID:  ga25wul
 * Date: 30.11.13
 * Time: 02:32
 * *
 */

/**
 * USAGE AND IMPLEMENTATION INFO:
 * - the Reduction controller was coded to ease readability of the performed reduction methods and to make it easier
 *   for new reductions to be implemented
 * - use 'cmd getall' to see the full list of implemented commands
 * - use 'cmd "cmd"' or 'help "cmd"' to see the description of the command 'cmd'
 *
 * CLASS: ACOMMANDABLE
 * - the class "ACommandable" is used like an interface, but it will automatically allow measuring times for each
 *   class it was derived from. In that case, the new derived class needs to call the ACommandable constructure with
 *   'super(true)'
 * - deriving from 'ACommandable' will force you to implement "executeMethod" and "Description".
 * - Description is self-explanatory and will only be used for debugging or if "help" was used
 *
 * METHOD: EXECUTE_METHODS
 * - executeMethods is the body that will be executed by the time the command parser has reached the command
 * - it will get a list of commands ( which are ALL commands between two round braces or all, if no braces used ) and
 *   an int value "cmdIndex", that will tell you the current index inside that array
 * - that way, the parser allows you to pull entries from that array, which will be used for additional arguments
 *
 * CUSTOM COMMAND ARGUMENTS
 * - what these arguments are is up to you as long as you pull them. I currently either had a fixed set of arguments
 *   to pull from ( and assured that they are present ) or let my methods look for braces behind my command
 * - in case of braces, there is a function to automatically split that into a command array
 *
 * - IF YOU USE ARGUMENTS, YOU NEED TO TELL THEM HOW MANY!
 *   And that is simple: return cmdIndex + 'number of arguments used from the array'.
 *   If you didn't need any, just return cmdIndex.
 *   Simple :)
 *
 * COMMAND PARSER
 * - the parser works on an array of strings. Each string is a space-free command and usually written as lowercase
 * - the parser will not brake if you mistype something, be careful. It will tell you, though.
 * - You can call the parser with a String 'executeCommands', an array of String 'executeCommands'
 * - The parser works iterative and recursively, meaning that it will iterate from the first to the last command by
 *   default, unless you use multiplications. In that case, it will copy the braces commands and perform them in a
 *   separate "executeCommands" methods with independent cmdIndex values. Therefore, you cannot simple terminate them.
 *
 * REPEATED COMMANDS, BRACES AND FULL TERMINATION
 * - you can make the parser repeat commands a certain time by using round braces around the commands you want to repeat
 * - YOU NEED TO TELL THEM HOW OFTEN!
 * - 1. You can write numbers before the left brace ( with space! )
 * - 2. You can use '*' as a special repeat operator. It will cause the braced commands to be repeated, until no edge
 *      has been removed. Can be handy, but be careful with that
 *
 * - So, how to terminate that stuff - easy: call 'terminateCommandChain'.
 * - terminateCommandChain wants to have a simple error message that will display the reason for the termination and
 *   will cause each instance of the parser to collapse clean and without braking anything. But remember: it terminates
 *   ALL commands, regardless of the depth it was called.
 * - if you want a certain braces command chain to collapse before its complete execution, use 'exit' as command.
 * - exit will exit the current command-execution and causes the parser to go "one level up".
 */

public class TReductionController implements GraphReduction {

	final static int EXECUTE_LOOP = 2;
	final static int EXIT_LOOP = -2;

	public boolean bTerminateCurrentCommands = false;
	boolean comparisonFailed = false;

    TReduce gReduce;
	ArrayList<TReduce> gReduces = new ArrayList<TReduce>(  );
	LinkedList<String> cmdNames = new LinkedList<String>(  );
	HashMap<String,ACommandable> CMD = new HashMap<String, ACommandable>(  );

    String[] rememberedCommand = new String[0];

	boolean gDebug = false;

    @Override
    public FGraph reduce(FGraph graph, double lowerbound) {

        if ( graph == null )
        {
			LoggerFactory.getLogger(this.getClass()).error("Cannot reduce graph: given graph is 'NULL'!");
        } else {

            // create new reduction instance
            this.gReduce = new TReduce( graph );
            this.gReduces.add(gReduce);

            executeCommands("reduce");

			this.gReduces.clear(); // TODO: -_- FIX that
        }

        return graph;
    }

    public void command(String cmd) {
        executeCommands(cmd);
    }

    /**
     * - this will try to parse arguments as type integer
     * - those values are returned in form of an array in the order they are inside 'args'
     * - it return NULL, if there is any kind of error inside this code
     * @param args
     * @param useBrackets
     * @return
     */
    private int[] parseIntArguments( String[] args, boolean useBrackets ) {

        if( args == null || args.length == 0 ) {
			LoggerFactory.getLogger(this.getClass()).error(" Cannot parse int arguments! No arguments! ");
            return null;
        }

        if( useBrackets ) {
            if( args[0].matches( "\\(" ) && args[args.length-1].matches("\\)") ) {
                return parseIntArguments( Arrays.copyOfRange( args, 1, args.length-1 ), false );
            } else {
				LoggerFactory.getLogger(this.getClass()).error(" Cannot parse int arguments: no brackets!");
                return null;
            }
        } else {

            int[] ints = new int[args.length];
            try {

                for( int i = 0; i<ints.length; i++ ) {
                    ints[i] = Integer.parseInt( args[i] );
                }

                return ints;
            } catch ( NumberFormatException e ) {
				LoggerFactory.getLogger(this.getClass()).error(" Cannot parse int arguments: not a number! ");
				bTerminateCurrentCommands = true;
				return null;
            }
        }
    }

	protected int terminateCommandChain( String termMsg ) {

		if ( termMsg == null )
			termMsg = " terminate current commands. ";

		bTerminateCurrentCommands = true;
		LoggerFactory.getLogger(this.getClass()).error(termMsg);
		return 0;
	}

    //////////////////////
	///--- COMMANDS ---///

	/**
	 * commands as class declarations
	 **/

	protected class CmdCalcAnchorToColorLowerBound extends ACommandable {


		protected CmdCalcAnchorToColorLowerBound() {
			super(true);
		}

		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println(" ~~~ CMD: calc-anc-col-lbs ");
			gReduce.calcAnchorToColorLowerBounds();
			return Ci;
		}

		
		protected String description() {

			return " ~ calc-anc-col-lbs \n" +
					" ~ no arguments expected ";
		}
	}

    /*
	protected class CmdCalcImpliedEdges extends ACommandable {


		protected CmdCalcImpliedEdges() {
			super(true);
		}

		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println( " ~~~ CMD: calc-implied-edges " );
			gHasDeletedEdgeLastTime = gReduce.calcReduceImpliedEdges();
			return Ci;
		}

		
		protected String description() {

			return " ~ calc-implied \n" +
					" ~ no arguments";
		}
	}

	protected class CmdCalcRecursiveSlideLowerBound extends ACommandable {


		protected CmdCalcRecursiveSlideLowerBound() {
			super(true);
		}

		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println( " ~~~ CMD: calc-rec-slide-lbs " );
			gReduce.calcRecursiveSlideLowerBound();
			return Ci;
		}

		
		protected String description() {

			return " ~ calc-rec-slide-lbs" +
					" ~ ";
		}
	}
	*/

	protected class CmdClearVertexUpperBounds extends ACommandable {


		protected CmdClearVertexUpperBounds() {
			super(true);
		}

		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println(" ~~~ CMD: clear-vertex-ubs");
			gReduce.clearVertexUpperBounds(Double.POSITIVE_INFINITY);
			return Ci;
		}

		
		protected String description( ) {

			return " ~ clear-vertex-ubs \n" +
					" ~ no arguments expected \n" +
					" ~ this will clear any existing upper bound scores";
		}
	}

	protected class CmdCmd extends ACommandable {

		HashMap<String, Integer> arg = new HashMap<String, Integer>(  );
		final static int getall = 1;
		final static int help = 2;

		protected CmdCmd() {
			arg.put( "getall", CmdCmd.getall );
			arg.put( "help", CmdCmd.help );
		}

		
		protected int executeMethod( String[] arg, int Ci ) {

			if( Ci >= arg.length ) {
				// there are no more commends!
				System.out.println(this.description( ));
				return Ci;
			}

			Integer i = this.arg.get( arg[Ci] ); // get first param
			if( i != null ) {

				Ci++;
				switch ( i ) {
					case CmdCmd.getall:

						System.out.println( " ~~~ CMD: CommandList . . . " );
						for( String s : cmdNames ) {
							System.out.println(" * " + s );
						}
						System.out.println();
						break;
					case CmdCmd.help:

						if( Ci < arg.length ) {
							ACommandable c = CMD.get(arg[Ci]);
							if( c != null )
								System.out.println( c.description( ) );
							else {
								return terminateCommandChain( " ! 'Cmd help' couldn't find command: " + arg[Ci] );
							}
							Ci++;
						} else
							return terminateCommandChain( this.description() );

						break;
				}

			} else {
				System.out.println(this.description( ));
			}

			return Ci;
		}

		
		protected String description( ) {

			return " ~ Command Parser. Type \"cmd getall\" to get a list of possible commands\n" +
					" ~ Type \"cmd help [command]\" to get a description of the given command\n";
		}
	}

	protected class CmdCompareEdges extends ACommandable {

		
		protected int executeMethod( String[] arg, int Ci ) {

			if ( ( Ci+3 < arg.length ) && ( arg[Ci].matches("\\(") ) && ( arg[Ci+3].matches( "\\)" )) ) {

				System.out.println(" ~~~ CMD: compareEdges: " + arg[Ci+1] + ", " + arg[Ci+2] );

				LoggerFactory.getLogger(this.getClass()).error(" ||||||||||||||||||||||||||||| \n||| COMPARE EDGES NOT REIMPLEMENTED YET! \n|||||||||||||||||||||||||||||\n");

				Ci = Ci + 4;
			} else {
				System.out.println(" ~~~ CMD: compareEdges " );
				System.out.println(this.description());
				return terminateCommandChain( " Wrong use of 'compareEdges'. " );
			}

			return Ci;
		}

		
		protected String description() {

			return " ~ compareEdges ( name1 name2 ) \n" +
					" ~ searches name1 and name2 for missing and unequal weighted edges";
		}
	}

	protected class CmdCompareEdges_alt extends ACommandable {

		
		protected int executeMethod( String[] arg, int Ci ) {

			if ( ( Ci+3 < arg.length ) && ( arg[Ci].matches("\\(") ) && ( arg[Ci+3].matches( "\\)" )) ) {

				System.out.println(" ~~~ CMD: compareEdges-alt: " + arg[Ci+1] + ", " + arg[Ci+2] );
				int id1 = -1, id2 = -1;
				try {
					id1 = Integer.parseInt( arg[Ci+1] );
					id2 = Integer.parseInt( arg[Ci+2] );

					if( id1 < 0 || id2 < 0 || id1 > gReduces.size() || id2 > gReduces.size() ) {
						throw new IndexOutOfBoundsException(  );
					}

				} catch( Exception e ) {
					LoggerFactory.getLogger(this.getClass()).error("Couldn't parse integer or out of bounds!! ");
					System.exit( 1 );
				}

				LoggerFactory.getLogger(this.getClass()).error(" ||||||||||||||||||||||||||||| \n||| COMPARE EDGES NOT REIMPLEMENTED YET! \n|||||||||||||||||||||||||||||\n");

				/*
				// whatever it is - the construction is right with "cmd ( name1 name2 )"
				if ( TComperator.compareGraphsForMissingEdges( gReduces.get(id1).gGraph, gReduces.get(id2).gGraph ) ) {
					System.err.println( "graphs " + arg[Ci + 1] + ", " + arg[Ci + 2] + " are asynchronous! " );
					comparisonFailed = true;
				} else
					System.out.println(" graph " + arg[Ci+1] + ", " + arg[Ci+2] + " are identical!");
				*/

				Ci = Ci + 4;
			} else {
				System.out.println(" ~~~ CMD: compareEdges " );
				System.out.println(this.description());
				return terminateCommandChain( " Wrong use of 'compareEdges'. " );
			}

			return Ci;
		}

		
		protected String description() {

			return " ~ compareEdges ( <int> <int> ) \n" +
					" ~ searches the graphs save at entry 1 and 2 for missing and unequal weighted edges";
		}
	}

    protected class CmdCompareLbs extends ACommandable {

        
        protected int executeMethod( String[] arg, int Ci ) {

            System.out.println(" ~~~ CMD: --compareLbs");
            // i want the ids of the graphs loaded using read-alt
            int[] ID = parseIntArguments( Arrays.copyOfRange( arg, Ci, Ci+4 ), true );
            if ( ID != null ) {
                if ( !TComperator.compareLowerBounds( gReduces.get(ID[0]), gReduces.get(ID[1]) ) ) {
                    // bounds are unequal!
					return terminateCommandChain( " Graphs at " + ID[0] + " and " + ID[1] + " are unequal!" );
                } else {
                    System.out.println(" equal. ");
                }
            } else
				return terminateCommandChain( " Some error occurred while trying to use '--compareLbs' " );

            return Ci+4;
        }

        
        protected String description() {
            return " ~ --compareLbs ( ID1 ID2 )" +
                    " ~ tries to compare lower bounds of graphs loaded using read-alt at array entry ID1, ID2";
        }

    }

	protected class CmdCompareStats extends ACommandable {

		
		protected int executeMethod( String[] arg, int Ci ) {

			if ( ( Ci+3 < arg.length ) && ( arg[Ci].matches("\\(") ) && ( arg[Ci+3].matches( "\\)" )) ) {

				System.out.println( " ~~~ CMD: compareStats: " + arg[Ci + 1] + ", " + arg[Ci + 2] );

				LoggerFactory.getLogger(this.getClass()).error(" ||||||||||||||||||||||||||||| \n||| COMPARE EDGES NOT REIMPLEMENTED YET! \n|||||||||||||||||||||||||||||\n");

				/*
				// whatever it is - the construction is right with "cmd ( name1 name2 )"
				if ( TComperator.compareStats( arg[Ci+1], arg[Ci+2] ) )
					System.out.println("Header of " + arg[Ci+1] + ", " + arg[Ci+2] + " are equal! ");
				*/

				Ci = Ci + 4;
			} else {
				System.out.println(" ~~~ CMD: compareStats " );
				System.out.println(this.description());
				return terminateCommandChain( " Wrong use of 'compareStats'. " );
			}

			return Ci;
		}

		
		protected String description() {

			return " ~ --compareStats [ ( file1 file 2 ) ]" +
					" ~ compares to header for unequal values" +
					" ~ if the are different, it will terminate the program" +
					" ~ designed for the use of FULL-TEST";
		}
	}

	protected class CmdCompareStats_alt extends ACommandable {

		
		protected int executeMethod( String[] arg, int Ci ) {

			if ( ( Ci+3 < arg.length ) && ( arg[Ci].matches("\\(") ) && ( arg[Ci+3].matches( "\\)" )) ) {

				System.out.println( " ~~~ CMD: compareStats: " + arg[Ci + 1] + ", " + arg[Ci + 2] );
				int id1 = -1, id2 = -1;
				try {
					id1 = Integer.parseInt( arg[Ci+1] );
					id2 = Integer.parseInt( arg[Ci+2] );

					if( id1 < 0 || id2 < 0 || id1 > gReduces.size() || id2 > gReduces.size() ) {
						throw new IndexOutOfBoundsException(  );
					}

				} catch( Exception e ) {
					return terminateCommandChain( " Couldn't parse integer or out of bounds!! " );
				}

				LoggerFactory.getLogger(this.getClass()).error(" ||||||||||||||||||||||||||||| \n||| COMPARE EDGES NOT REIMPLEMENTED YET! \n|||||||||||||||||||||||||||||\n");

				/*
				// whatever it is - the construction is right with "cmd ( name1 name2 )"
				if ( TComperator.compareStats( gReduces.get(id1).gGraph, gReduces.get(id2).gGraph ) )
					System.out.println("Header of " + arg[Ci+1] + ", " + arg[Ci+2] + " are equal! ");
				*/

				Ci = Ci + 4;
			} else {
				System.out.println(" ~~~ CMD: compareStats " );
				System.out.println(this.description());
				return terminateCommandChain( " Wrong use of 'compareStats'. " );
			}

			return Ci;
		}

		
		protected String description() {

			return " ~ --compareStats [ ( file1 file 2 ) ]" +
					" ~ compares to header for unequal values" +
					" ~ if the are different, it will terminate the program" +
					" ~ designed for the use of FULL-TEST";
		}
	}


	protected class CmdCompareUbs extends ACommandable {

		
		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println(" ~~~ CMD: --compareUbs");
            int[] ints = parseIntArguments( Arrays.copyOfRange( arg, Ci, Ci+4 ), true );
			if ( ints != null ) {
				TComperator.compareUpperBounds( gReduces.get(ints[0]), gReduces.get(ints[1]) );
				Ci = Ci+4;
			} else {
				System.out.println(this.description());
				return terminateCommandChain( " ~ wrong usage of command! " );
			}

			return Ci;
		}

		
		protected String description() {

			return " ~ --compareUbs [ ( file1 file2 ) ] \n" +
					" ~ compares given ub tables for differences \n" +
					" ~ they need to be of equal size to make the function work! \n";
		}
	}

	/**
	 * CMD: --test-divide-and-compare
	 *
	 * TODO
	 */
	protected class CmdTestDivideAndCompare extends ACommandable {

		protected CmdTestDivideAndCompare() { super(true); }

		@Override
		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println("\n\n   <><><> TESTING... DIVIDE AND COMPARE INITIATED...");
			int flid = -1;
			if ( Ci < arg.length ) {
				try {
					flid = Integer.parseInt( arg[Ci] );
				} catch ( Exception e ) {
					System.out.println(" Cannot run '--test-divide-and-compare' without file index!");
					System.out.println(description());
					terminateCommandChain( "Not a number!" );
					return 0;
				}
			} else {
				System.out.println(description());
				terminateCommandChain( " to few arguments! " );
				return 0;
			}

			final String cmdString = "enable-seb-vub-strength * ( clear-vertex-ubs seb-vertex-ubs tim-vertex-ubs reduce-vub reduce-unreach ) calc-anc-col-lbs DEBUG-calc-anchor-lbs calc-rec-slide-lbs reduce-dompath";

			System.out.println("   <><><> CMD-LINE: " + cmdString );
			System.out.println("   <><><> apply commands on both graphs. The first does not use 'renumber-verts' ");

			executeCommands("read-alt 0 fl2 " + flid + " " + cmdString); // executes too
			executeCommands("read-alt 1 fl2 " + flid + " renumber-verts " + cmdString + " unrenumber-verts");
			executeCommands("--select 0");

			System.out.println("   <><><> START OF DIVISION...: " + cmdString );

			int nVerts = gReduce.gGraph.numberOfVertices();
			assert ( nVerts > 0 ) : " vertex count cannot be equal or less than 0 : " + nVerts;

			// we do now split the graph into parts, let the command line on the 1. graph without additional
			// renumbering and on the 2. on with 'renumber-verts' + cmdString + 'unrenumber-verts'
			for ( int i=nVerts-1; i>0; i-- ) {

				executeCommands("--select 0");
				executeCommands("--keep-vertices-below-deepcopy " + i + " 2");
				executeCommands("--select 1");
				executeCommands("--keep-vertices-below-deepcopy " + i + " 3");
				executeCommands("--compareEdges-alt ( 2 3 )");

				if ( comparisonFailed ) {
					executeCommands("--select 2 write name G" + flid + "_SUB_GRAPH_" + i);
					executeCommands("--select 3 write name G" + flid + "_SUB_GRAPH_" + i + "_REN");
					comparisonFailed = false;
				}
			}

			System.out.println("   <><><> FINISHED" );

			return Ci+1;
		}

		@Override
		protected String description() {

			return " ~ --double-renumbering-and-split-test \n" +
					" ~ a command line gets run on 1 graph using 2 renumberings \n" +
					" ~ the input graphs should already be renumbered! \n" +
					" ~ parts of the graph that differ are printed down to a file \n";
		}
	}

    protected class CmdDebugCalcAnchorlbs extends ACommandable {


		protected CmdDebugCalcAnchorlbs() {
			super(true);
		}

		protected int executeMethod( String[] arg, int Ci ) {

            System.out.println(" ~~~ CMD: DEBUGcalc-anchor-lbs");
            gReduce.DoDEBUGcalcAnchorLowerBounds();
            return Ci;
        }

        
        protected String description() {
            return " ~ DEBUGcalc-anchor-lbs " +
                    " ~ no arguments expected ";
        }
    }


	/**
	 * - delete a series of vertices
	 */
    protected class CmdDeleteVertices extends ACommandable {

        
        protected int executeMethod( String[] arg, int Ci ) {

            System.out.println("\n  ~~~ CMD: Delete vertices");
            while( Ci < arg.length ) {
                try {
                    int id = Integer.parseInt( arg[Ci] );
                    // went smoothly. Proceed with deletion
                    Ci++;
                    gReduce.gGraph.deleteFragment(gReduce.gGraph.getFragmentAt(id));

                } catch( Exception e ) {
                    break;
                }
            }

			System.out.println();
            return Ci;
        }

		
		protected String description( ) {

			return " ~ delVert \n" +
					" ~ [a b relative ...] \n" +
				    " ~ deletes one or more vertices by the given ID (using vertex search)\n";
		}
	}

	protected class CmdDrawGraph extends ACommandable {

		protected CmdDrawGraph() {
			super(true);
		}
		
		protected int executeMethod( String[] arg, int Ci ) {

			String name = "";

			// look for additional arguments
			if ( Ci+2 < arg.length ) {
				if ( arg[Ci].matches( "\\(" ) && arg[Ci+2].matches( "\\)" ) && ( !arg[Ci+1].matches( "" )) ) {

					if( !Character.isAlphabetic(arg[Ci+1].charAt( 0 )) )
						return terminateCommandChain( " Cannot draw graph with name: " + arg[Ci+1] + " : not alphabetic!" );
					else
						name = arg[Ci+1];
				}
			}

			TGraphicalOutput.drawGraph( gReduce, name );

			Ci += 3;
			return Ci;
		}

		
		protected String description() {

			return  " drawGraph ( name ) \n" +
					" ~ 'name' for the output file \n" +
					" Write down a simple image representing the graph as jpg file.\n";
		}
	}

	protected class CmdEnableSebVubStrength extends ACommandable {

		
		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println(" ~~~ CMD: enable seb vub strength ");
			gReduce.enableSebVertexUbsStrengthening();
			return Ci;  //To change body of implemented methods use File | Settings | File Templates.
		}

		
		protected String description() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}
	}

	/**
	 * - program termination signal
	 */
	protected class CmdExit extends ACommandable {

		
		protected int executeMethod( String[] arg, int Ci ) {

			return terminateCommandChain( " ~~~ CMD: Exit called. Terminate further commands. (Why did you even get here?)" );
		}

		
		protected String description( ) {

			return " ~ Exit \n"+
					" ~ no arguments expected \n" +
				    " ~ Exit and quit the program.\n";
		}
	}

	protected class CmdHelp extends ACommandable {

		
		protected int executeMethod( String[] arg, int Ci ) {

			if( Ci < arg.length ) {

				ACommandable c = CMD.get(arg[Ci]);
				if( c != null )
					System.out.println( c.description( ) );
				else
					terminateCommandChain( " ! 'Cmd help' couldn't find command: " + arg[Ci] );

				Ci++;
			} else {
				System.out.println( this.description() );
				return terminateCommandChain( null );
			}

			return Ci;
		}

		
		protected String description() {

			return " ~ help. Use it like: 'help' 'cmd', where 'cmd' is any valid command \n" +
					" ~ you can use 'help' or '?' \n";
		}
	}


	/**
	 * CMD: --alpha
	 */
	protected class CmdOutputAlpha extends ACommandable {

		@Override
		protected int executeMethod( String[] arg, int Ci ) {

			float alpha;
			if ( Ci < arg.length ) {
				try {
					alpha = Float.parseFloat( arg[Ci] );
					TGraphicalOutput.setAlpha( alpha );
					System.out.println(" ~~~ CMD: --alpha : " + alpha );

				} catch ( NumberFormatException e ) {
					terminateCommandChain( "--setAlpha should be called with an argument!" );
					System.out.println(description());
					return 0;
				}
			} else {
				terminateCommandChain( "--setAlpha should be called with an argument!" );
				System.out.println(description());
				return 0;
			}

			return Ci+1;
		}

		@Override
		protected String description() {

			return " ~ --setAlpha FLOAT \n" +
					" ~ FLOAT is a value in [0.0, 1.0] \n";
		}
	}

    /**
     * predefined reduce chain with an array of commands.
     * Use "CmdTest" before changing stuff here.
     */
    protected class CmdReduce extends ACommandable {

        protected String cmdChain = "renumber-verts enable-seb-vub-strength tim-vertex-ubs reduce-vub * ( * ( clear-vertex-ubs seb-vertex-ubs tim-vertex-ubs reduce-vub reduce-unreach ) )";

        protected CmdReduce() { super(true); } // enable time measuring for command "reduce"

        @Override
        protected int executeMethod(String[] arg, int Ci) {

            System.out.println("\n  ~~~ Cmd: reduce \n");
            ACommandable.resetRuntime();
            executeCommands(cmdChain);
            return Ci; // no additional parameters used!
        }

        @Override
        protected String description() {
            return "~ CmdReduce: \n" +
                    " ~ a predefined list of reductions will be used on the given graph to remove unnecessary edges. ";
        }
    }

	/**
	 * give some output through the command console
	 * Arguments:
	 * 	- [vertices|leafs|ubs]
	 */
	protected class CmdPrint extends ACommandable {

		HashMap<String, Integer> arg = new HashMap<String, Integer>(  );

		final static int EDGES = 1;
		final static int FILELIST = 2;
		final static int LEAFS = 3;
		final static int LOWER_BOUNDS = 4;
		final static int UPPER_BOUNDS = 5;
		final static int VERTEX = 6;
		final static int VERTICES = 7;
		final static int M_ = 8;

		protected CmdPrint() {

			arg.put( "edges", CmdPrint.EDGES );
			arg.put( "leafs", CmdPrint.LEAFS );
			arg.put( "lbs", CmdPrint.LOWER_BOUNDS );
			arg.put( "m_", CmdPrint.M_ );
			arg.put( "ubs", CmdPrint.UPPER_BOUNDS);
			arg.put( "vertex", CmdPrint.VERTEX );
			arg.put( "vertices", CmdPrint.VERTICES );
		}

		protected int executeMethod( String[] arg, int Ci ) {

			if( Ci < arg.length ) {

				String str = arg[Ci++];
				try {

					switch(this.arg.get( str )) {
						case CmdPrint.EDGES:
							System.out.print( "\n  ~~~ CMD: print <edges> \n" );
							for ( Loss e : gReduce.gGraph.losses() ) {
								System.out.println("e:   " + e );
							}
							break;
						case CmdPrint.LEAFS:
							System.out.print( "\n ~~~ CMD: print <leafs> \n" );
							for ( Fragment v : gReduce.gGraph.getFragments() )
								if ( v.isLeaf() )
									System.out.println("v:   " + v );
							break;
						case CmdPrint.LOWER_BOUNDS:
							System.out.print( "\n ~~~ CMD: print <lbs> \n" );
							gReduce.printLowerBounds();
							break;
						case CmdPrint.M_:
							System.out.print( "\n ~~~ CMD: print <m_> \n" );
							gReduce.printm_();
							break;
						case CmdPrint.UPPER_BOUNDS:
							System.out.print( "\n ~~~ CMD: print <ubs> \n" );
							gReduce.printVertexUpperBounds();
							break;
						case CmdPrint.VERTEX:
							System.out.print( "\n ~~~ CMD: print <vertex> \n" );
							if ( Ci < arg.length ) {
								try {
							   		int id = Integer.parseInt( arg[Ci] );
									if ( id >= 0 && id <= gReduce.gGraph.numberOfVertices()-1 ) {
								   		System.out.println( gReduce.gGraph.getFragmentAt( id ) );
									} else
										return terminateCommandChain( "\n ~ CMD: print <vertex>. Couldn't run! No valid id! " + id + " \n" );

									return Ci+1;
								} catch ( Exception e ) {
									return terminateCommandChain( "\n ~ CMD: print <vertex>. Couldn't run! Next argument is not an integer number! \n" );
								}
							} else
								return terminateCommandChain( "\n ~ CMD: print <vertex>. Couldn't run! Not enough arguments \n" );
							// break. unreachable here!

						case CmdPrint.VERTICES:
							System.out.print( "\n ~~~ CMD: print <vertices> \n" );
							for ( Fragment v : gReduce.gGraph.getFragments() )
								System.out.println("v:   " + v );
							break;
					}
				} catch( Exception e ) {
					return terminateCommandChain( " ! Couldn't parse argument for command 'print': "+ str + ". Continue executing commands though." );
				}
			}

			return Ci;
		}

		
		protected String description( ) {

			return  " ~ print [edges|leafs|ubs|vertices] \n" +
					" ~ print some graph related values to the command console\n" +
					" ~ ubs: upper bounds values (DEBUG)\n";
		}
	}

	protected class CmdReachableEdges extends ACommandable {

		protected CmdReachableEdges() {
			super(true);
		}
		
		protected int executeMethod( String[] arg, int Ci ) {

			if ( Ci < arg.length ) {
				try {
					int id = Integer.parseInt( arg[Ci] );
					if ( id >= 0 && id <= gReduce.gGraph.numberOfVertices()-1 ) {
						System.out.println(" ~~~ CMD: --re. ID: " + id );
						printVerticesFromVertex( id );
						return Ci+1;
					} else
						return terminateCommandChain( " ~~~ CMD: --re. Couldn't run! Wrong ID: " + id );

				} catch ( Exception e ) {
					return terminateCommandChain( " ~~~ CMD: --re. Coudln't run! Not enough arguments!" );
				}
			}

			return Ci;
		}

		
		protected String description() {

			return " ~ --reachableEdges <int> \n" +
					" ~ print every vertex reachable from vertex at position <int> \n" +
					" ~ you can use --re <int> also";
		}
	}


    protected class CmdRemember extends ACommandable {

        
        protected int executeMethod( String[] arg, int Ci ) {

            rememberedCommand = new String[arg.length - (Ci)];
            System.arraycopy(arg,Ci,rememberedCommand,0,rememberedCommand.length);
            return Ci;
        }

        
        protected String description() {
            return " ~ remember \n" +
					" ~ saves the commands comming after 'remember', that can be reused and executed using 'repeat'. \n";
        }
    }


    protected class CmdRepeat extends ACommandable {

        
        protected int executeMethod( String[] arg, int Ci ) {

            if( (Ci + 2 < arg.length) && (arg[Ci].matches("\\(")) && (arg[Ci+2].matches("\\)")) ) {

                System.out.println(" ~~~ CMD: repeat < newFilename > ");
                // redo command but change the file!
                String newFileName = arg[Ci+1];

                int i = 0;
                while( (i<rememberedCommand.length) && ( !rememberedCommand[i].matches("read") ) ) {
                    i++;
                }

                if( i > rememberedCommand.length ) {
                    return terminateCommandChain( " Couldn't repeat command with changing the file: \"read\" command not found!" );
                } else {

                    if( rememberedCommand[i+1].matches("file") ) {
                        // valid input. change it!
                        rememberedCommand[i+2] = newFileName;
                        System.out.println( " ~ Repeat command line: " + StringArrayToArray(rememberedCommand) + " with new file: " + newFileName );
                        executeCommands(rememberedCommand);
                    } else {
                        return terminateCommandChain( "Previous command line had errors! exit!" );
                    }
                }

                return Ci+3;
            } else {

                // no additional arguments
                executeCommands( rememberedCommand );
                return Ci;
            }
        }

        
        protected String description() {
            return " ~ repeat \n" +
					" ~ executes the commands saved after calling 'remember' ";
        }
    }

	protected class CmdReduceColorSubtreeAdvantage extends ACommandable {

		protected CmdReduceColorSubtreeAdvantage() {
			super(true);
		}

		@Override
		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println( " ~~~ CMD: reduce-colsubtree-adv " );
			gHasDeletedEdgeLastTime = gReduce.doReduceColorsubtreeAdvantage();
			return Ci;
		}

		@Override
		protected String description() {

			return " ~ --reduce-colsubtree-adv \n" +
					" ~ relies on 'calc-anchor-lbs' \n" +
					" ~ upper bound reduction based on parts of seb-vertex-ubs \n" +
					" ~ no arguments expected \n";
		}
	}

	/*
	  NOT BUG-FREE

	protected class CmdReduceDominatingPath extends ACommandable {

		protected CmdReduceDominatingPath() {
			super(true);
		}

		Override
		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println( " ~~~ CMD: reduce-dompath " );
			gHasDeletedEdgeLastTime = gReduce.reduceDominatingPath();
			return Ci;
		}

		Override
		protected String description() {

			return " ~ --reduce-dompath \n" +
					" ~ no arguments expected \n" +
					" ~ requires: calc-rec-slide, calc-anchor-lbs, tim-vertex-ubs and/or seb-vertex-ubs \n";
		}
	}
	*/

	protected class CmdReduceNegativePendantEdges extends ACommandable {

		protected CmdReduceNegativePendantEdges() {
			super(true);
		}

		@Override
		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println(" ~~~ CMD: reduce-negpend ");
			gHasDeletedEdgeLastTime = gReduce.reduceNegativePendantEdges();
			return Ci;
		}

		@Override
		protected String description() {

			return " ~ reduce-negpend \n" +
					" ~ no arguments expected \n" +
					" ~ deleted negative weighted edges that are leading into leafs";
		}
	}

	protected class CmdReduceSlideStrong extends ACommandable {

		protected CmdReduceSlideStrong() {
			super(true);
		}

		@Override
		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println(" ~~~ CMD: reduce-slide-strong ");
			gHasDeletedEdgeLastTime = gReduce.reduceWithSlideStrong();
			return Ci;
		}

		@Override
		protected String description() {

			return " ~ reduce-slide-strong \n" +
					" ~ no arguments expected \n" +
					" ~ lower bounds reduction using 'm_' and 'gLB'" +
					" ~ you need to run 'calc-rec-slide-lbs' and 'DEBUGcalc-anchor-lbs' first before running this. \n";
		}
	}

	protected class CmdReduceUnreachable extends ACommandable {

		protected CmdReduceUnreachable() {
			super(true);
		}

		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println(" ~~~ reduce-unreach ");
			gHasDeletedEdgeLastTime = gReduce.reduceUnreachableEdges();
			return Ci;
		}

		
		protected String description() {

			return " ~ reduce-unreach \n" +
					" ~ delete edges from vertices, which have no source edges!" +
					" ~ takes O(nVertices) time";
		}
	}

	/**
	 * delete edges by upper bound scores
	 * no arguments
	 */
	protected class CmdReduceVub extends ACommandable {

		protected CmdReduceVub() {
			super(true);
		}

		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println( " ~~~ CMD: reduce-vub" );
			gHasDeletedEdgeLastTime = gReduce.reduceEdgesByVertexUpperBound();
			return Ci;
		}

		
		protected String description( ) {

			return " ~ Reduce vertex upper bounds\n" +
					" ~ no arguments expected\n" +
				    " ~ optimizes graph by deleting edges based on the vertex upper bounds score of each vertex \n" +
					" ~ takes O(nVertices) time, when the upper bounds are calculated";
		}
	}


	protected class CmdRuntime extends ACommandable {

		@Override
		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println( " ~~~ CMD: --runtime \n _________________________________" );

			// get all runtime-measured commands
			LinkedList<String> strl = new LinkedList<String>(  );
			for ( Map.Entry<String, ACommandable> entry : CMD.entrySet() ) {
				if ( entry.getValue().wasExecuted() ) {
					strl.add( "<> " + entry.getKey() + " | took: " + entry.getValue().getRuntime() );
				}
			}
			Collections.sort( strl ); // to make it look nicer

			// print them down :)
			for ( String s : strl )
				System.out.println(s);

			System.out.println( "_________________________________" );
			return Ci;
		}

		@Override
		protected String description() {

			return " ~ --runtime \n" +
					" ~ no arguments expected \n" +
					" ~ prints down the runtime of every command that ran during the current commandline & graph";
		}
	}

    /**
     * calculate upper bound score using sebastians method
     */
    protected class CmdSebastianVertexUpperBounds extends ACommandable {


		protected CmdSebastianVertexUpperBounds() {
			super(true);
		}

		protected int executeMethod( String[] arg, int Ci ) {

            System.out.println(" ~~~ CMD: seb-vertex-ubs");
            // make the possibility to let the user choose, if he wants to use strengthening or not
            gReduce.doSebastianVertexUpperBounds();

            return Ci;  //To change body of implemented methods use File | Settings | File Templates.
        }

        
        protected String description() {
            return " ~ seb-vertex-ubs\n" + "" +
                    " ~ no params expected \n" +
                    " ~ calculate upper bounds score with sebastians sub-tree method\n";  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

	protected class CmdSelect extends ACommandable {

		
		protected int executeMethod( String[] arg, int Ci ) {

			if ( Ci < arg.length ) {

				try {

					int id = Integer.parseInt( arg[Ci] );
					if( id >= 0 && id < gReduces.size() && gReduces.get( id ) != null ) {

						gReduce = gReduces.get( id );
                        System.out.println(" ~~~ CMD: --select " + id );
						ACommandable.resetRuntime();
					} else {
						printGraphs();
						return terminateCommandChain( "--select: id out of bounds or null value at id!" );
					}
				} catch ( Exception e ) {
					return terminateCommandChain( " The next argument should be a number!" );
				}
			} else
				return terminateCommandChain( " not enough arguments!" );

			return Ci+1;
		}

		
		protected String description() {

			return " ~ --select <ID> " +
					" ~ this will select the graph from 'gReduces', if ID is within the boundaries" +
					" ~ using id = -1 will result in showing the current size of gReduces";
		}

		private void printGraphs() {

			System.out.println(" printing gReduces . . . ");
			int i = 0;
			for( TReduce r : gReduces ) {
				System.out.println( " " + i + ": " + r.gGraph );
			}
		}
	}


	protected class CmdSourceEdges extends ACommandable {

		
		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println("~~~ CMD: --se");
			if ( Ci < arg.length ) {
				try {

					int id = Integer.parseInt( arg[Ci] );
					if ( id >= 0 && id <= gReduce.gGraph.numberOfVertices()-1 ) {

						System.out.println("< Printing source edges from " + id +" >" );
						for ( Loss e : gReduce.gGraph.getFragmentAt(id).getIncomingEdges() ) {
							if ( e == null )
								break;

							System.out.println( " - " + e);
						}
						System.out.println("< finished >");
					} else
						return terminateCommandChain( " ~CMD: --se. Couldn't run! Id not valid. " + id );

				} catch ( Exception e ) {
					return terminateCommandChain( " ~CMD: --se. Couldn't find a valid integer number as argument! " );
				}

			} else
				return terminateCommandChain( " ~CMD: --se. Couldn't run! not enough arguments." );

			return Ci+1;
		}

		
		protected String description() {

			return " ~ --sourceEdges <Int> \n" +
					" ~ prints down all source edges of the vertex at position <int> inside the graphs array \n" +
					" ~ you can use --se instead";
		}
	}

	protected class CmdTargeLosss extends ACommandable {

		
		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println("~~~ CMD: --te");
			if ( Ci < arg.length ) {
				try {

					int id = Integer.parseInt( arg[Ci] );
					if ( id >= 0 && id <= gReduce.gGraph.numberOfVertices()-1 ) {

						System.out.println("< Printing target edges from " + id +" >" );
						for ( Loss e : gReduce.gGraph.getFragmentAt(id).getOutgoingEdges() ) {
							if ( e == null )
								break;

							System.out.println( " - " + e);
						}
						System.out.println("< finished >");
					} else
						return terminateCommandChain( " ~CMD: --te. Couldn't run! Id not valid. " + id );

				} catch ( Exception e ) {
					return terminateCommandChain( " ~CMD: --te. Couldn't find a valid integer number as argument! " );
				}

			} else
				return terminateCommandChain( " ~CMD: --te. Couldn't run! not enough arguments." );

			return Ci+1;
		}

		
		protected String description() {

			return " ~ --targeLosss <Int> \n" +
					" ~ prints down all target edges of the vertex at position <int> inside the graphs array \n" +
					" ~ you can use --te instead";
		}
	}

	/**
	 * calculate upper bound score of all vertices using tims-method
	 * {}
	 */
	protected class CmdTimVertexUpperBounds extends ACommandable {


		protected CmdTimVertexUpperBounds() {
			super(true);
		}

		protected int executeMethod( String[] arg, int Ci ) {

			System.out.println(" ~~~ CMD: tim-vertex-ubs");
			gReduce.doTimVertexUpperBounds();
			return Ci;
		}

		
		protected String description( ) {

			return " ~ tim-vertex-ubs \n" +
					" ~ no arguments expected \n" +
				    " ~ tims vertex upper bounds reduction method \n" +
			        " ~ calculates the upper bounds score of every vertex \n";
		}
	}

	/**
	 * test command
	 * executes any previously defined command line
	 */
	protected class CmdTest extends ACommandable {

        private int current=0;

		protected CmdTest() {
			super(true);
		}

		protected int executeMethod( String[] arg, int Ci ) {

            String TestString = "--test-                executeCommands( \"--select 0\" );\n-and-compare 1";
			String FullTestCommands[] = new String[]{"0 READFL2 renumber-verts enable-seb-vub-strength tim-vertex-ubs reduce-vub * ( * ( clear-vertex-ubs seb-vertex-ubs tim-vertex-ubs reduce-vub reduce-unreach ) calc-implied-edges ) calc-anc-col-lbs calc-rec-slide-lbs print stats"};

            if( (Ci < arg.length) && (arg[Ci].matches("full") ) ) {

                return Ci+1; // has been increased before! so it's initial Ci+1, effectively
            } else {

                // just a normal testing then. Boring :p
                executeCommands(TestString);
                return Ci;
            }

		}

		
		protected String description( ) {

			return "";
		}
	}


	//////////////////////////
	///--- CONSTRUCTORS ---///

	public TReductionController() {
		setCommands();
	}

	///////////////////////////////
	///--- GETTER AND SETTER ---///

	/**
	 * - method for adding commands
	 * - can be extended directly or through subclasses
	 */
	protected void setCommands() {

		this.CMD.put( "--alpha", new CmdOutputAlpha() );
		this.CMD.put( "--compareEdges", new CmdCompareEdges() );
		this.CMD.put( "--compareEdges-alt", new CmdCompareEdges_alt() );
        this.CMD.put( "--compareLbs" , new CmdCompareLbs() );
		this.CMD.put( "--compareStats", new CmdCompareStats() );
		this.CMD.put( "--compareStats-alt", new CmdCompareStats_alt() );
		this.CMD.put( "--compareUbs", new CmdCompareUbs() );
		this.CMD.put( "--delVert", new CmdDeleteVertices() );
		this.CMD.put( "--drawGraph", new CmdDrawGraph() );
		this.CMD.put( "--dg", new CmdDrawGraph() );
		this.CMD.put( "--reachableEdges", new CmdReachableEdges() );
		this.CMD.put( "--re", new CmdReachableEdges() );
		this.CMD.put( "--runtime", new CmdRuntime() );
		this.CMD.put( "--sourceEdges", new CmdSourceEdges() );
		this.CMD.put( "--se", new CmdSourceEdges() );
		this.CMD.put( "--select", new CmdSelect() );
		this.CMD.put( "--targeLosss", new CmdTargeLosss() );
		this.CMD.put( "--te", new CmdTargeLosss() );
		this.CMD.put( "--test-divide-and-compare", new CmdTestDivideAndCompare() );

		this.CMD.put( "calc-anc-col-lbs", new CmdCalcAnchorToColorLowerBound() );
		//this.CMD.put( "calc-implied-edges", new CmdCalcImpliedEdges() );
		//this.CMD.put( "calc-rec-slide-lbs", new CmdCalcRecursiveSlideLowerBound() );
		this.CMD.put( "clear-vertex-ubs", new CmdClearVertexUpperBounds() );
		this.CMD.put( "cmd", new CmdCmd() );
        this.CMD.put( "DEBUG-calc-anchor-lbs", new CmdDebugCalcAnchorlbs() );
		this.CMD.put( "exit", new CmdExit() );
		this.CMD.put( "help", new CmdHelp() );
		this.CMD.put( "?", new CmdHelp() );
		this.CMD.put( "print", new CmdPrint() );
		this.CMD.put( "reduce-colsubtree-adv", new CmdReduceColorSubtreeAdvantage() );
		this.CMD.put( "reduce-slide-strong", new CmdReduceSlideStrong() );
		this.CMD.put( "reduce-unreach", new CmdReduceUnreachable() );
		this.CMD.put( "reduce-vub", new CmdReduceVub() );
        this.CMD.put( "repeat", new CmdRepeat() );
        this.CMD.put( "remember", new CmdRemember() );
        this.CMD.put( "seb-vertex-ubs", new CmdSebastianVertexUpperBounds() );
        this.CMD.put( "enable-seb-vub-strength", new CmdEnableSebVubStrength() );
		this.CMD.put( "tim-vertex-ubs", new CmdTimVertexUpperBounds() );
		this.CMD.put( "test", new CmdTest() );

		this.CMD.put( "reduce", new CmdReduce() );


		for ( String s : CMD.keySet() ) {
			this.cmdNames.add( s );
		}

		Collections.sort( cmdNames );
	}

	/////////////////////
	///--- METHODS ---///

	/**
	 * - get the String array representing the commands between two braces
	 * @param arg: global String array
	 * @param Ci: command index to start and iterate on
	 * return: command part between two braces without the braces
	 */
	protected String[] parseParenthesesCommand( String[] arg, int Ci ) {

		LinkedList<String> cmdString = new LinkedList<String>(  );
		String s = "";

		int nBraces = 0;

		while( ( Ci < arg.length ) && ( ( s = arg[Ci] ).matches( "\\(" ) || (nBraces > 0) ) ) { // looks kinda odd, but "(" is not allowed here!

			if( s.matches( "\\(" ) ) {

                if ( ++nBraces > 1 )
                    cmdString.addLast( "(" );  // internal bracket. Add it to the command block
			} else if( s.matches( "\\)" ) ) {

                if( --nBraces > 0 )
                    cmdString.addLast( ")" ); // internal bracket. Add it to the command block
			} else {
				cmdString.addLast( s );
			}

			Ci++;
		}

		String[] buf = new String[cmdString.size()];
		buf = cmdString.toArray( buf );

		return buf;
	}

    /**
     * use the hash value here!
     * @param V_ID
     */
    protected void printVerticesFromVertex( final int V_ID ) {

        if ( gReduce != null ) {

            System.out.println("< Printing edges from " + V_ID +" >" );

            final Iterator<Fragment> IT  = gReduce.gGraph.postOrderIterator( gReduce.gGraph.getFragmentAt(V_ID) );
            Fragment frag;
            while ( IT.hasNext() ) {
                frag = IT.next();
                System.out.println( " - HashID: " + frag.getVertexId() + ", Vertex: " + frag );
            }

            System.out.println("< finished >");
        } else {
            System.out.println("Cannot load edges: no graph loaded!");
            bTerminateCurrentCommands = true;
            return;
        }

    }

	/**
	 * - split 'str' between spaces and copy its parts into a linked list
	 * - executeMethod the newly created linked list
	 * @param str: valid command line
	 */
    protected void executeCommands(String str) {

        LinkedList<String> cmd = new LinkedList<String>();
        Scanner scanner = new Scanner(str);

        while( scanner.hasNext() ) {
            cmd.add(scanner.next());
        }

		String[] buf = new String[cmd.size()];
		buf = cmd.toArray( buf );

        executeCommands(buf);
    }

    protected String[] extractCommands(String str) {

        LinkedList<String> cmd = new LinkedList<String>();
        Scanner scanner = new Scanner(str);

        while( scanner.hasNext() ) {
            cmd.add(scanner.next());
        }

        String[] buf = new String[cmd.size()];
        buf = cmd.toArray( buf );

        return buf;
    }

	boolean gHasDeletedEdgeLastTime = false; // global buffer
	/**
	 * - default command console input ( commands with arguments, if necessary )
	 * - executes the commands given in 'str'
	 * @param cmds: valid command line input
	 * return: TRUE, if there has been no TERMINATION command: "exit"
	 */
	protected int executeCommands( String[] cmds ) {

		String sBuf = "";
		boolean HasDeletedEdgeLastTime = false;
		int Ci = 0; // command index

        while( Ci < cmds.length && !bTerminateCurrentCommands ) {

			ACommandable cmd = this.CMD.get( cmds[Ci] );

            if( cmd != null ) {

				if( cmd.getClass() == CmdExit.class ) // termination signal
					System.exit(1);

                Ci = cmd.execute( cmds, ++Ci ); // valid command -> executeMethod
				HasDeletedEdgeLastTime |= gHasDeletedEdgeLastTime; // the function may manipulate gHasDeletedEdgeLastTime!
                gHasDeletedEdgeLastTime = false;

			} else {

				/*
				 * i may parse stuff like * ( A B ) or 45 ( A ) or * ( A 45 ( B ) * ( C ) )
				 * - number i € N => counted loop
				 * - char = "*" => loop-execution til no edges have been deleted
				 */

				sBuf = cmds[Ci];
				if( sBuf.matches( "\\*" ) ) { // recall until not edges has been deleted

					String[] partialCommand = parseParenthesesCommand( cmds, ++Ci ); // get the command line between to round braces ( ... )
					System.out.println(" Executing: " + StringArrayToArray( partialCommand ) + " | as long as edges will not be deleted anymore.");

					int c = 1;
					while( executeCommands( partialCommand ) == TReductionController.EXECUTE_LOOP )
						c++; // the command parser will notify to executeMethod again if edges has been deleted
					System.out.println(" ~ executed " + c + " times ");

					Ci += partialCommand.length+2;
				} else if ( sBuf.matches("t") ) {

                    String[] partialCommand = parseParenthesesCommand( cmds, ++Ci );
                    long before = System.currentTimeMillis();
                    executeCommands( partialCommand );
                    long after = System.currentTimeMillis();

                    Ci += partialCommand.length+2; // magical line. woop woop

                    System.out.println(" Command block: " + StringArrayToArray( partialCommand ) + " took " + (after-before) + " ms to run. ");
                } else {
					try {  // counted execution

						int z = Integer.parseInt( sBuf ); // should be either * or a number. Else, the command-part is not identified!

                        String[] partialCommand = parseParenthesesCommand( cmds, ++Ci ); // get the command line between to round braces ( ... )
						System.out.println(" Executing: " + StringArrayToArray( partialCommand ) + " | " + z + " times."  );

						for( int i=0; i<z; i++ )
							executeCommands( partialCommand );

						Ci += partialCommand.length+2;

                    } catch ( NumberFormatException e ) {
						System.out.println(" couldn't parse command: " + sBuf );
						LoggerFactory.getLogger(this.getClass()).error(" couldn't parse command: " + sBuf );
                        return EXIT_LOOP;
					} catch( InputMismatchException e ) {
						System.out.println(" couldn't parse command: " + sBuf );
						LoggerFactory.getLogger(this.getClass()).error(" couldn't parse command: " + sBuf );
						return EXIT_LOOP;
					}
				}

			}

        }

		if ( bTerminateCurrentCommands )
			LoggerFactory.getLogger(this.getClass()).error(" Commands terminated. ");

		return ( HasDeletedEdgeLastTime ) ? TReductionController.EXECUTE_LOOP : TReductionController.EXIT_LOOP; // only necessary for "*"
	}

	////////////////////////
	/// HELPER FUNCTIONS ///

	protected String StringArrayToArray( String[] str ) {

		String out = "";
		for( String s : str )
			out += " " + s;

		return out;
	}

}
