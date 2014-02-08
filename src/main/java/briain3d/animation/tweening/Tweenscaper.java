package briain3d.animation.tweening;

import java.awt.geom.Point2D;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger; 

import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.batik.parser.DefaultPathHandler;
import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PathHandler;
import org.apache.batik.parser.PathParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 
 *
 * <P>
 * This will need some explanation...
 * </P>
 *
 * <P>
 * Thanks to these guys:
 * </P>
 *
 * <OL>
 *     <LI>http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/</LI>
 *     <LI>http://www.mkyong.com/java/how-to-create-xml-file-in-java-dom/</LI>
 *     <LI>http://viralpatel.net/blogs/java-xml-xpath-tutorial-parse-xml/</LI>
 *     <LI>Clive @ http://stackoverflow.com/questions/4520137/does-java-have-mutable-types-for-integer-float-double-long </LI>
 * </OL>
 *
 * @author Brian Hammond
 *
 */
public class Tweenscaper {
	private static Logger LOGGER = LogManager.getLogger( Tweenscaper.class.getSimpleName() );

	private XmlHelper xmlHelper_ = new XmlHelper();
	private Document document_;
	private StreamResult output_;

	////

	int LAME_TWEEN_COUNT = 6;
	int LAME_POINT_COUNT = 400;
	boolean LAME_ANGULAR = false;

	////

	public Tweenscaper() {
	}

	////

	public static void main( String[] args ) throws Exception {
		new Tweenscaper().run( args );
	}

	public void run( String[] args ) throws Exception {
		// TODO: arg parsing... may just do it with json...
		this.setInput( System.in );
		this.setOutput( System.out );
		this.run();
	}

	public void run() throws Exception {
		this.tween( this.getInput() );
		this.writeOutput();
	}

	public void writeOutput() throws Exception {
		this.getXmlHelper().write( this.getInput(), this.getOutput() );
	}

	public void tween( Document document ) throws Exception {
		this.tween( 
			document
			, this.getXmlHelper().getSortedLayers( document )
			, this.getXmlHelper().getTitles( document )
		);
	}

	public void tween( Document document, Map< Double, Node > sortedLayers, Set< String > titles ) throws Exception {
		LOGGER.info( "sorted layers:" + sortedLayers );
		LOGGER.info( "paths with titles:" + titles );

		int count = 1;
		Map< Double, Node > newLayers = new TreeMap< Double, Node >();

		double lastValue = 0;
		Node lastNode = null;
		String lastId = null;

		double firstValue = 0;
		Node firstNode = null;
		String firstId = null;

		Map< String, List< Point2D > > normalized_path_map = new HashMap< String, List< Point2D > >();

		for ( Map.Entry< Double, Node > layerEntry : sortedLayers.entrySet() ) {
			String id = this.getXmlHelper().value( layerEntry.getValue(), "id" );

			if ( null == firstNode ) {
				firstValue = layerEntry.getKey();
				firstNode = layerEntry.getValue();
				firstId = id;
			}

			for ( String title : titles ) {
				Node path = this.getXmlHelper().layersPath( document, id, title );
				if ( null == path ) {
					LOGGER.info( "there is no path titled " + title + " in layer.id:" + id + ", moving on" );
					continue;
				}

				LOGGER.info( id + "." + title + " -> " + path );

				String key = id + "\t" + title;
				List< Point2D > normalizedPath = this.normalizePath(
					LAME_POINT_COUNT // TODO: externalize this
					, this.parsePath( this.getXmlHelper().value( path, "d" ) ) // d means "path", makes perfect sense :-P
				);
				normalized_path_map.put( key, normalizedPath );
			}

			// make the inbetween layes

			this.inbetween( document, count, lastValue, lastNode, lastId, layerEntry.getKey(), layerEntry.getValue(), id, titles, newLayers, normalized_path_map );

			// put a clone of the original into newLayers 

			newLayers.put( layerEntry.getKey(), layerEntry.getValue().cloneNode( true ) ); 
			LOGGER.info( "add layer " + layerEntry.getKey() );

			// keep old values

			lastValue = layerEntry.getKey();
			lastNode = layerEntry.getValue();
			lastId = id;
		}

		// inbetween for first and last

		this.inbetween( document, count, lastValue, lastNode, lastId, lastValue + 1 /* sigh */, firstNode, firstId, titles, newLayers, normalized_path_map );

		// out with the old

		Node svg = null;
		for ( Node old : sortedLayers.values() ) {
			if ( null == svg ) {
				svg = old.getParentNode();
			}
			svg.removeChild( old ); // why not removeChildNode, you crazy long winded bastards!
		}

		// in with the new

		for ( Node newLayer : newLayers.values() ) {
			svg.appendChild( newLayer );
		}
	}

	public void inbetween( Document document, int count, double lastKey, Node lastNode, String lastId, double nextKey, Node nextNode, String nextId, Set< String > titles, Map< Double, Node > newLayers, Map< String, List< Point2D > > normalized_path_map ) throws Exception {
		if ( null == lastNode || null == nextNode ) return;

		count = LAME_TWEEN_COUNT;

		double diff = ( nextKey - lastKey );
		for ( int i = 0 ; i < count ; i++ ) {
			double percent = ( i + 1 ) / ( double ) ( count + 1 );

			double frame_id = lastKey + diff * percent;
			String newLabel = "inbetween_" + frame_id;

			Node newLayer = lastNode.cloneNode( true );

			newLayers.put( frame_id, newLayer );
			LOGGER.info( "add layer' " + frame_id + ", percent:" + percent );

			this.getXmlHelper().set( newLayer, "inkscape:label", newLabel );
			this.getXmlHelper().set( newLayer, "id", newLabel );

			LOGGER.info( "creating " + newLabel );

			for ( String title : titles ) {
				List< Point2D > previousPath = normalized_path_map.get( lastId + "\t" + title );
				if ( null == previousPath ) {
					LOGGER.info( title + " not found in " + lastId + ", skipping it" );
					continue;
				}

				List< Point2D > nextPath = normalized_path_map.get( nextId + "\t" + title );
				if ( null == nextPath ) {
					LOGGER.info( title + " not found in " + nextId + ", keep on truckin'" );
					continue;
				}

				Node currentPath = this.getXmlHelper().node( newLayer, "//path[./title = '" + title + "']" );
				if ( null == currentPath ) {
					LOGGER.info( title + " not found in the new layer, and it should have been! ur boned!" );
					continue;
				}

				List< Point2D > newPath = new ArrayList< Point2D >();
				int pointIndex = 0;

				Point2D previousPrior = null;
				Point2D nextPrior = null;
				Point2D currentPrior = null;

				for ( Point2D previousPoint : previousPath ) {
					Point2D nextPoint = null;
					try { 
						nextPoint = nextPath.get( pointIndex );
					} catch ( Exception e ) {
						if ( null == nextPoint ) {
							LOGGER.error( "missing point " + pointIndex + " in " + nextId + "," + title + " only " + nextPath.size() + " points" );
							continue;
						}
					}
					pointIndex++;

					// finally: make the inbetween points
					Point2D currentPoint = null;

					// for the first point, do a straight inbetween
					if ( null == previousPrior || null == nextPrior ) {

						Point2D pointDiff = new Point2D.Double(
							  nextPoint.getX() - previousPoint.getX() 
							, nextPoint.getY() - previousPoint.getY()
						);

						currentPoint = new Point2D.Double(
							  previousPoint.getX() + pointDiff.getX() * percent 
							, previousPoint.getY() + pointDiff.getY() * percent 
						);
					} else {
						// otherwise it's time to use angle and distance

						if ( LAME_ANGULAR ) {
							// this uses angular difference and distance... 
							double previousDistance = previousPoint.distance( previousPrior );
							double previousAngle = this.angle( previousPrior, previousPoint );

							double nextDistance = nextPoint.distance( nextPrior );
							double nextAngle = this.angle( nextPrior, nextPoint );

							double distanceDiff = nextDistance - previousDistance;
							double angleDiff = nextAngle - previousAngle;

							double distanceCurrent = previousDistance + distanceDiff * percent;
							double angleCurrent = previousAngle + angleDiff * percent;

							currentPoint = new Point2D.Double(
								  currentPrior.getX() + Math.cos( angleCurrent ) * distanceCurrent
								, currentPrior.getY() + Math.sin( angleCurrent ) * distanceCurrent
							);
						} else {
							// uses diff tweening... slightly better than angular

							Point2D previousDiff = new Point2D.Double(
								  previousPoint.getX() - previousPrior.getX()
								, previousPoint.getY() - previousPrior.getY()
							);
							Point2D nextDiff = new Point2D.Double(
								  nextPoint.getX() - nextPrior.getX()
								, nextPoint.getY() - nextPrior.getY()
							);

							Point2D diffDiff = new Point2D.Double(
								  nextDiff.getX() - previousDiff.getX()
								, nextDiff.getY() - previousDiff.getY()
							);

							Point2D currentDiff = new Point2D.Double(
								  previousDiff.getX() + diffDiff.getX() * percent
								, previousDiff.getY() + diffDiff.getY() * percent
							);

							currentPoint = new Point2D.Double(
								  currentPrior.getX() + currentDiff.getX()
								, currentPrior.getY() + currentDiff.getY()
							);
						}
					}

					newPath.add( currentPoint );
					//LOGGER.trace( "between:" + previousPoint + " to " + nextPoint + " = " + pointDiff + " -> " + currentPoint );

// trying to do relative to last point on each path proved problematic... for for angularr :-(
if ( !LAME_ANGULAR||null == previousPrior ) {

					previousPrior = previousPoint;
					nextPrior = nextPoint;
					currentPrior = currentPoint;
} // snit
				}

				this.getXmlHelper().set( currentPath, "d", this.toString( newPath, true /* FIXME:  need to track this */  ) );
			}
		}
	}

	public double angle( Point2D a, Point2D b ) {
		// toa = o/a = dy/dx
		double dy = b.getY() - a.getY();
		double dx = b.getX() - a.getX();

		return Math.atan2( dy, dx );
	}

	// http://xmlgraphics.apache.org/batik/using/parsers.html#examples
	public List< Point2D > parsePath( String path ) throws Exception {
		final List< Point2D > serat = new ArrayList< Point2D >();
		PathParser pp = new PathParser();

		// no curve support... 
		// does batik have a better hook to get the rendered points for curves too?
		PathHandler ph = new DefaultPathHandler() {
			float ax = 0;
			float ay = 0;
			public void movetoRel(float xo, float yo)  throws ParseException { this.movetoAbs( ax + xo, ay + yo ); }
			public void linetoRel(float xo, float yo)  throws ParseException { this.linetoAbs( ax + xo, ay + yo ); }
			public void linetoHorizontalRel(float xo)  throws ParseException { this.linetoHorizontalRel( ax + xo ); }
			public void linetoVerticalRel(float yo)    throws ParseException { this.linetoVerticalRel( ay + yo ); }
			public void linetoHorizontalAbs(float x)   throws ParseException { this.linetoAbs( x, ay ); }
			public void linetoVerticalAbs(float y)     throws ParseException { this.linetoAbs( ax, y ); }
			public void movetoAbs(float x, float y)    throws ParseException { ax = x; ay = y; serat.add( new Point2D.Double( ax, y ) ); }
			public void linetoAbs(float x, float y)    throws ParseException { movetoAbs( x, y ); }
		};

		LOGGER.debug( "parsing:" + path );

		pp.setPathHandler( ph );
		pp.parse( path );
		
		LOGGER.debug( "parsed: " + serat );

		return serat;
	}

	public String toString( List< Point2D > points, boolean hadZ ) {
		StringBuilder bob = new StringBuilder( "m" );
		Point2D previous = null;
		for( Point2D current : points ) {
			double x = current.getX();
			double y = current.getY();
			if ( null != previous ) {
				// make this relative
				x -= previous.getX();
				y -= previous.getY();
			}
			previous = current;
			bob
				.append( " " )
				.append( x )
				.append( "," )
				.append( y );
			;
		}

		if ( hadZ ) {
			bob.append( " z" );
		}
		
		return bob.toString();
	}

	public double pathLength( List< Point2D > points ) {
		double sum = 0;

		Point2D previous = points.get( points.size() - 1 );

		for( Point2D current : points ) {
			sum += current.distance( previous );
			previous = current;
		}

		return sum;
	}


	/**
	 *
	 * <p>
	 * Paths have to start at the same point and be made of straight
	 * segments (inkscape, see extensions, path tools, flatten bezier).
	 * </p>
	 *
	 * <p>
	 * Paths have to start at the same point and be made of straight
	 * This method makes sure they have the same number of points in
	 * a uniform distribution along the path:
	 * </p>
	 *
	 * <OL>
	 *     <LI>allocation of # of points per segment based on ratio of segment length * count</LI>
	 *     <LI>make sure the allocation exactly equals count, jiggle as needed</LI>
	 *     <LI>use the per segment counts to interpolate per segment</LI>
	 * <OL>
	 *
	 * <p>
	 * "When you do things right, people won't be sure you've done anything at all."
	 * </p>
	 *
	 * @param count number of points in the new path
	 * @param points list of Point2D in the original path
	 *
	 * @return new path with uniform distribution of {count} points along the original path
	 *
	 *
	 */
	public List< Point2D > normalizePath( int count, List< Point2D > points ) {
		List< Point2D > path = new ArrayList< Point2D >();

		double pathLength = this.pathLength( points );
		LOGGER.info( "path " + pathLength + " and has " + points.size() + " points" );

		Point2D previous = null;

		// first work out how many points per segment

		int sum = 0;

		List< AtomicInteger > pointsPerSegment = new ArrayList< AtomicInteger >();

		previous = points.get( points.size() - 1 );
		for ( Point2D current : points ) {
			double distance = current.distance( previous );
			double percent = distance / pathLength;
			int share = ( int ) ( count * percent ) + 1;
			sum += share;

			LOGGER.trace( distance + " / " + pathLength + " = " + percent + " so " + share + ", " + sum );
			pointsPerSegment.add( new AtomicInteger( share ) );

			previous = current;
		}
		
		LOGGER.info( "need to jiggle: " + sum + " versus " + count );

		// "commence the jigglin"

		if( sum > count ) {
			this.jiggleBelly( sum - count, -1, pointsPerSegment );
		} else {
			if ( sum < count ) {
				LOGGER.info( "huh... really?" );
				this.jiggleBelly( count - sum, 1, pointsPerSegment );
			}
		}

		// use the per segment counts to interpolate per segment

		sum = 0;

		int segmentIndex = 0;
		previous = points.get( points.size() - 1 );
		for ( Point2D current : points ) {
			int share = pointsPerSegment.get( segmentIndex++ ).intValue();
			if ( 0 == share ) {
				LOGGER.error( ": no points allocated for this segment!!!" );
				return null;
			}

			sum += share;

			double distance = current.distance( previous );
			Point2D pointDiff = new Point2D.Double(
				  ( current.getX() - previous.getX() )
				, ( current.getY() - previous.getY() )
			);
			Point2D mod = new Point2D.Double(
				  pointDiff.getX() / share
				, pointDiff.getY() / share
			);

			LOGGER.trace( "norman: from " + previous + " to " + current + " in " + share + " segments: " + mod );

			for ( int i = 0 ; i < share ; i++ ) {

				Point2D nu = new Point2D.Double(
					  previous.getX() + mod.getX() * i
					, previous.getY() + mod.getY() * i
				);

				LOGGER.trace( "norman>" + i + " > " + nu );

				path.add( nu );
			}

			previous = current;
		}

		if ( sum != count || path.size() != count ) {
			LOGGER.error( "path has wrong number of points:" + sum + " vs " + count + " and " + path.size() );
		} else {
			LOGGER.info( "new path length is good:" + sum + " vs " + count );
		}

		return path; 
	}

	public void jiggleBelly( int count, int increment, List< AtomicInteger > pointsPerSegment ) {
		Map< Integer, AtomicInteger > sizeToSegment = new TreeMap< Integer, AtomicInteger >();
		double sum = 0;
		for ( AtomicInteger atomic : pointsPerSegment ) {
			sum += atomic.intValue();
			sizeToSegment.put( -atomic.intValue(), atomic );
		}
		LOGGER.info( "by size: " + sizeToSegment + " need " + increment + " x " + count + " times" );

		int lost = 0;
		for ( AtomicInteger atomic : sizeToSegment.values() ) {
			int share = atomic.intValue();

			if ( increment < 0 && share == 2 ) {
				LOGGER.error( "ERROR: did not modify enuff and now it is too late!" );
				return;
			}

			int toLose = ( int ) ( count * share / sum ) + 2;
			while ( toLose + lost > count ) {
				toLose--; 
			}
			lost += toLose;

			int nu = ( share + increment * toLose );
			
			LOGGER.trace( "from " + share + ", modify " + toLose + " times to " + nu + ", so far " + lost + " versus " + count  );
			atomic.set( nu );

			if ( lost == count ) {
				return;
			}
			if ( lost > count ) {
				LOGGER.error( "jiggled to much!" + lost + " verus " + count );
				return;
			}
		}
	}

	public String toString( List< Point2D > normalized, String path ) {
		return this.toString( normalized, path.endsWith( "z" ) );
	}

	public String normalizePath( int count, String path ) throws Exception {
		return this.toString( this.normalizePath( count, this.parsePath( path ) ), path );
	}
	
	////

	public Document getInput() {
		return this.document_;
	}
	
	public void setInput( Document document ) {
		this.document_ = document;
		//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		//optional, but recommended
		this.document_.getDocumentElement().normalize();
	}

    public void setInput( InputStream in ) throws Exception {
		this.setInput( this.getXmlHelper().read( in ) );
	}

    public void setInput( InputStream in, String derp ) throws Exception {
		this.setInput( this.getXmlHelper().read( in, derp ) );
	}

    public void setInput( String in ) throws Exception {
		this.setInput( this.getXmlHelper().read( in ) );
	}

    public void setInput( File in ) throws Exception {
		this.setInput( this.getXmlHelper().read( in ) );
	}

	////

	public StreamResult getOutput() {
		return this.output_;
	}
	
	public void setOutput( StreamResult output ) {
		this.output_ = output;
	}

	public void setOutput( OutputStream out ) {
		this.setOutput( new StreamResult( out ) );
	}

	public void setOutput( Writer out ) {
		this.setOutput( new StreamResult( out ) );
	}

	public void setOutput( String out ) {
		this.setOutput( new StreamResult( out ) );
	}
	
	public void setOutput( File out ) {
		this.setOutput( new StreamResult( out ) );
	}

	////

	public XmlHelper getXmlHelper() {
		return this.xmlHelper_;
	}
	
	public void setXmlHelper( XmlHelper xmlHelper ) {
		this.xmlHelper_ = xmlHelper;
	}
};
