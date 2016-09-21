/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.reduction;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Xentrics
 * Date: 17.12.13
 * Time: 12:34
 * To change this template use File | Settings | File Templates.
 */
public class TGraphicalOutput {

    final static int DRAW_EDGES = 0;
    final static int DRAW_UPPERBOUNDS = 1;

    // file related
	final static int imageHeight = 1080; //1080
    static int hMod = 15;
	final static int imageWidth = 1920; // 2560
    static int wMod = 60;
    final static int rad = 5;

    static int drawMode = TGraphicalOutput.DRAW_EDGES;

    static int[] colorEntries;
	static int colorCount;

	static float gAlpha = 0.05f;

    private static AlphaComposite makeAlpha( float alpha ) {
        int type = AlphaComposite.SRC_OVER;
        return (AlphaComposite.getInstance(type, alpha));
    }

	/**
	 * TODO: implement
	 * @param graph
	 * @return
	 */
	private static int[] getColorEntries( FGraph graph ) {

		int[] colorEntries = new int[ graph.maxColor()+1 ];
		return colorEntries;
	}

    /**
     * - method, that is called, when the mode and every necessary thing for the specific method is provided
     * @param red: the graph itself, not the reduction.
     * @param name
     */
    public static void drawGraph( TReduce red, String name ) {

		System.out.println("______________________");
		System.out.println("||-|-|-|-|-|-|-|-|-|-|");
		System.out.println("||- creating output -|");

		colorCount = red.gGraph.maxColor()+1;

        //allocate height
        final int height = imageHeight;
		hMod = height / ( colorCount + 1 );

        // allocate/approximate width
        colorEntries = getColorEntries( red.gGraph );
        int maxColorEntry = -1;
        for( Integer I : colorEntries )
            maxColorEntry = Math.max( maxColorEntry, I );

        final int width = imageWidth;
		wMod = width / ( maxColorEntry + 1 );

		// create image plain & get necessary values from it
        BufferedImage img = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );

        Graphics gr = img.getGraphics();
        Graphics2D g2d = (Graphics2D) gr;

        switch ( drawMode ) {
            case DRAW_EDGES:
                drawMode_Edges(g2d, width, height, maxColorEntry, red.gGraph.getFragments(), gAlpha );
                break;
            case DRAW_UPPERBOUNDS:
                if ( !drawMode_UpperBounds( g2d, width, height, maxColorEntry, red.gGraph.getFragments(), red.getUB() ) ) {
                    System.out.println("||- aborted -|-|-|-|-|");
                    System.out.println("||-|-|-|-|-|-|-|-|-|-|");
                    return;
                }
                drawMode = DRAW_EDGES;
                break;
            default:
                LoggerFactory.getLogger(TGraphicalOutput.class).error("Unknown draw mode: " + drawMode );
                System.out.println("||- aborted -|-|-|-|-|");
                System.out.println("||-|-|-|-|-|-|-|-|-|-|");
                return;
        }

		// TODO TFileLoader.saveImageToFile( img, name );

		System.out.println("||- finished |-|-|-|-|");
		System.out.println("||-|-|-|-|-|-|-|-|-|-|");
    }

    private static boolean drawMode_UpperBounds( Graphics2D g2d, final int width, final int height, final int maxColorEntry, final java.util.List<Fragment> vertices, final double[] ub ) {

        if ( ub.length != vertices.size() ) {
            LoggerFactory.getLogger(TGraphicalOutput.class).error(" ub length doesn't match vertices length! abort drawing graph!");
            return false;
        }

        // distances of vertices on the drawn image
        final int vdist = width / maxColorEntry; // vertical distance ( width between two neighbored vertices )
        final int hdist = height / colorCount; // height between 2 vertices of different, neighbored color

        // effective vertex positions on the image
        Point[] vertexPos = new Point[vertices.size()];

        // "pointer"-like structure telling the next vertex of a color which position to take ( at the moment )
        final int[] PosAtColor = new int[colorCount];

        // calculate the start pos of each color-row, which will be the most right position from the middle of the image,
        // so that after applying each vertex the image is somewhat symmetrical ( as long as no edges are applied )
        for( int i=0; i<PosAtColor.length; i++ )
            PosAtColor[i] = ( width/2 ) - ( colorEntries[i]/2 ) * vdist ; // initiate start position

        // get vertex position first, so that the may be drawn after drawing the edges => they stay in front of the image
        for( int i = 0; i<vertices.size(); i++ ) {

            int col = vertices.get(i).getColor();
            Point p = new Point( PosAtColor[col], col * hdist );

            vertexPos[ vertices.get(i).getVertexId() ] = p;
            PosAtColor[col] += vdist;
        }

        // get the range of bound we are considering here
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        for ( Double d : ub ) {
            if ( d == Double.NEGATIVE_INFINITY || d == Double.POSITIVE_INFINITY )
                continue; // we do not want does values - how could we calculate a range then :o?

            if ( d > max )
                max = d;

            if ( d < min )
                min = d;
        }

        if ( min == Double.POSITIVE_INFINITY || max == Double.NEGATIVE_INFINITY || min == max ) {
            LoggerFactory.getLogger(TGraphicalOutput.class).error(" min and max of upper bound values are invalid: infinite or equal! abort drawing.");
            return false;
        }

		System.out.println(" Max ub: " + max + " , min ub: " + min );

        // now calculate the offeset
        final double off = -min;
        /* color = ( off + val ) / ( max + off ) */

        // set the basic background and some options
        g2d.setColor( Color.black );
        g2d.fill( new Rectangle(width, height) );
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        // get a new color system!
		final float low = 0.0f;
		final float high = 240.0f;
		final float saturation = 0.0019609f; // that is the lowest value before it gets white :(
		final float brightness = 1.0f;

        Color[] colorForTargetVertex = new Color[vertices.size()];
        for( int i=0; i<vertices.size(); i++ ) {
            // this will make a value between 120 and 360 => 0.33.. and 1.0
            Color c = SystemColor.getHSBColor( ( ( low + high * (float) ( ( off + ub[i] ) / ( off + max ) ) ) / 360.f ), saturation, brightness );
            colorForTargetVertex[i] = new Color(c.getRed()/255, c.getGreen()/255, c.getBlue()/255, gAlpha );
        }

        // draw edges ( first )
        Point p1, p2;
        for( Fragment v : vertices ) {

            if ( v.isRoot() )
                continue;

            g2d.setColor( colorForTargetVertex[v.getVertexId()] );

            // we color the edge by the upper bound they lead into
            p2 = vertexPos[ v.getVertexId() ];
            for( Loss edges : v.getIncomingEdges() ) {
                if( edges == null )
                    break;

                p1 = vertexPos[ edges.getSource().getVertexId() ];
                g2d.drawLine( p1.x + rad/2, p1.y + rad/2, p2.x + rad/2, p2.y + rad/2 );
            }
        }

        // draw vertices
        g2d.setColor( new Color(1.0f ,1.0f ,1.0f , 1.0f) );
        for( Fragment v : vertices ) {
            p1 = vertexPos[ v.getVertexId() ];
			if ( ( v.isRoot() ) || ( v.getIncomingEdges().size() > 0 ) ) {

				g2d.setColor( new Color( 1.0f , 0.0f , 1.0f , 1.0f ) );
				g2d.fillRect( p1.x, p1.y, rad, rad );
				g2d.setColor( new Color( 1.0f , 1.0f , 1.0f , 1.0f ) );
			} else
            	g2d.fillOval(p1.x, p1.y, rad, rad);
        }

        return true; // everything went fine.
    }

    private static void drawMode_Edges( Graphics2D g2d, final int width, final int height, final int maxColorEntry, final List<Fragment> vertices, final float alpha ) {

        // distances of vertices on the drawn image
        final int vdist = width / maxColorEntry; // vertical distance ( width between two neighbored vertices )
        final int hdist = height / colorCount; // height between 2 vertices of different, neighbored color

        // effective vertex positions on the image
        Point[] vertexPos = new Point[vertices.size()];

        // "pointer"-like structure telling the next vertex of a color which position to take ( at the moment )
        final int[] PosAtColor = new int[colorCount];

        // calculate the start pos of each color-row, which will be the most right position from the middle of the image,
        // so that after applying each vertex the image is somewhat symmetrical ( as long as no edges are applied )
        for( int i=0; i<PosAtColor.length; i++ )
            PosAtColor[i] = ( width/2 ) - ( colorEntries[i]/2 ) * vdist ; // initiate start position

        // get vertex position first, so that the may be drawn after drawing the edges => they stay in front of the image
        for( int i = 0; i<vertices.size(); i++ ) {

            int col = vertices.get( i ).getColor();
            Point p = new Point( PosAtColor[col], col * hdist );

            vertexPos[ vertices.get( i ).getVertexId() ] = p;
            PosAtColor[col] += vdist;
        }

        // set the basic background and some options
        g2d.setColor( Color.black );
        g2d.fill( new Rectangle(width, height) );
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

		final float saturation = 0.0019609f;

        // get a new color system!
        Color[] colorForColor = new Color[colorCount];
        for( int i=0; i<colorCount; i++ ) {
            Color c = SystemColor.getHSBColor( ( ( 120.f + (120.f/colorCount) * i ) / 360.f ), saturation, 1.0f );
            colorForColor[i] = new Color(c.getRed()/255, c.getGreen()/255, c.getBlue()/255, gAlpha );
        }


        // draw edges ( first )
        Point p1, p2;
        for( Fragment v : vertices ) {

            g2d.setColor( colorForColor[v.getColor()] );

            p1 = vertexPos[ v.getVertexId() ];
            for( Loss edges : v.getOutgoingEdges() ) {
                if( edges == null )
                    break;

                p2 = vertexPos[ edges.getTarget().getVertexId() ];
                g2d.drawLine( p1.x + rad/2, p1.y + rad/2, p2.x + rad/2, p2.y + rad/2 );
            }
        }

        // draw vertices
		final Color linkedColor = new Color(1.0f ,1.0f ,1.0f , 1.0f);
		final Color dislinkedColor = new Color( 1.0f, 0.0f, 1.0f, 1.0f );
        for( Fragment v : vertices ) {
            p1 = vertexPos[ v.getVertexId() ];
			if ( (v.isRoot()) || (v.getIncomingEdges().size() > 0) ) {
				g2d.setColor( dislinkedColor );
				g2d.fillRect( p1.x, p1.y, rad, rad );
			} else {
				g2d.setColor( linkedColor );
				g2d.fillOval( p1.x, p1.y, rad, rad );
			}
        }
    }

    public static void setModeUpperBounds( double[] ubs ) {

        if ( ubs == null ) {
            LoggerFactory.getLogger(TGraphicalOutput.class).error(" Cannot use ubs to draw upper bounds, if ubs is null! Gonna use default draw mode instead...");
            return;
        }

        drawMode = DRAW_UPPERBOUNDS;
    }

	public static void setAlpha( float newAlpha ) {
		if ( newAlpha < 0.0f || newAlpha > 1.0f ) {
			throw new IndexOutOfBoundsException( "Alpha must be a value between 0.0 and 1.0!" );
		}

		gAlpha = newAlpha;
	}

}
