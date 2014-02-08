/*NaN 2>/dev/null

# ok, this is getting nuts...
classpath=""
for dep in http://repo1.maven.org/maven2/batik/batik-parser/1.6/batik-parser-1.6.jar http://repo1.maven.org/maven2/batik/batik-util/1.6/batik-util-1.6.jar ; do
	base=$( basename ${dep} )
	classpath="${classpath}${base}:"
	if [ -f ${base} ] ; then
		continue
	fi
	wget ${dep} || exit 1
done

classpath=${classpath}.

if [ Tweenscaper.java -nt Tweenscaper.class ] ; then
	javac -classpath ${classpath} Tweenscaper.java 
	status=${?}
	if [ 0 != ${status} ] ; then
		exit ${status}
	fi
fi
	
java -classpath ${classpath} Tweenscaper ${*}
exit ${?}

-----

thx to:

* http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
* http://www.mkyong.com/java/how-to-create-xml-file-in-java-dom/
* http://viralpatel.net/blogs/java-xml-xpath-tutorial-parse-xml/

*/

import java.awt.geom.Point2D;//.Double;

import java.io.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Scanner;
import java.util.TreeMap;

import java.util.concurrent.atomic.AtomicInteger; // thx Clive @ http://stackoverflow.com/questions/4520137/does-java-have-mutable-types-for-integer-float-double-long 

import javax.xml.namespace.NamespaceContext;
 
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
 
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;

import java.util.Iterator;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.batik.parser.DefaultPathHandler;
import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PathHandler;
import org.apache.batik.parser.PathParser;

/** 
 *
 * This will need some explanation...
 *
 * @author Brian Hammond
 *
 */
public class Tweenscaper {
	private XPath xpath_;

	int LAME_TWEEN_COUNT = 6;
	int LAME_POINT_COUNT = 400;
	boolean LAME_ANGULAR = false;

	public static void main( String[] args ) throws Exception {
		new Tweenscaper().run( args );
	}

	public void run( String[] args ) throws Exception {
		//File fXmlFile = new File("/Users/mkyong/staff.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse( System.in ); //fXmlFile);

		//optional, but recommended
		//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		doc.getDocumentElement().normalize();

		this.tween( doc );

		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult( System.out );// new File("C:\\file.xml"));
		transformer.transform(source, result);
	}

	public void log( Object lazyBastard ) {
		System.err.println( new Date() + " - DEBUG - LOL - " + lazyBastard );
	}

	public void tween( Document document ) throws Exception {
		this.tween( 
			document
			, this.getSortedLayers( document )
			, this.getTitles( document )
		);
	}

	public Map<Double, Node> getSortedLayers( Document document ) throws Exception {
		Map<Double, Node> sortedLayers = new TreeMap<Double, Node>();
		NodeList layers = this.nodes( document, "//g[@groupmode='layer']" );

		for ( int i = 0 ; i < layers.getLength() ; i++ ) {
			Node g = layers.item( i );
			String label = g.getAttributes().getNamedItem( "inkscape:label" ).getNodeValue();
			String numeric_label = label.replaceAll( "^[^0-9]*", "" ).replaceAll( "[^0-9.].*$", "" );
			try {
			double numeric_value = Double.valueOf( numeric_label );

			//this.log( g + ":" + label + " : " + numeric_label + " : " + numeric_value );
			sortedLayers.put( numeric_value, g );
			} catch ( Exception e ) {
				this.log( "skipping layer " + label );
			}
		}
			
		return sortedLayers;
	}

	public Set< String > getTitles( Document document ) throws Exception {
		// TODO: make sure this maintains the order in the dom
		NodeList titles = this.nodes( document, "//path/title/text()" );
		Set< String > uniqueTitles = new HashSet< String >();

		for ( int i = 0 ; i < titles.getLength() ; i++ ) {
			String title = titles.item( i ).getNodeValue().trim();
			if ( !title.isEmpty() ) {
				uniqueTitles.add( title );
			}
		}

		return uniqueTitles;
	}

	public void tween( Document document, Map< Double, Node > sortedLayers, Set< String > titles ) throws Exception {
		this.log( "sorted layers:" + sortedLayers );
		this.log( "paths with titles:" + titles );

		int count = 1;
		Map< Double, Node > newLayers = new TreeMap< Double, Node >();

		double lastValue = 0;
		Node lastNode = null;
		String lastId = null;

		double firstValue = 0;
		Node firstNode = null;
		String firstId = null;

		Map< String, List< Point2D.Double > > normalized_path_map = new HashMap< String, List< Point2D.Double > >();

		for ( Map.Entry< Double, Node > layerEntry : sortedLayers.entrySet() ) {
			String id = this.value( layerEntry.getValue(), "id" );

			if ( null == firstNode ) {
				firstValue = layerEntry.getKey();
				firstNode = layerEntry.getValue();
				firstId = id;
			}

			for ( String title : titles ) {
				Node path = this.layersPath( document, id, title );
				if ( null == path ) {
					this.log( "there is no path titled " + title + " in layer.id:" + id + ", moving on" );
					continue;
				}

				this.log( id + "." + title + " -> " + path );

				String key = id + "\t" + title;
				List< Point2D.Double > normalizedPath = this.normalizePath(
					LAME_POINT_COUNT // TODO: externalize this
					, this.parsePath( this.value( path, "d" ) ) // d means "path", makes perfect sense :-P
				);
				normalized_path_map.put( key, normalizedPath );
			}

			// make the inbetween layes

			this.inbetween( document, count, lastValue, lastNode, lastId, layerEntry.getKey(), layerEntry.getValue(), id, titles, newLayers, normalized_path_map );

			// put a clone of the original into newLayers 

			newLayers.put( layerEntry.getKey(), layerEntry.getValue().cloneNode( true ) ); 
			this.log( "add layer " + layerEntry.getKey() );

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

	public void inbetween( Document document, int count, double lastKey, Node lastNode, String lastId, double nextKey, Node nextNode, String nextId, Set< String > titles, Map< Double, Node > newLayers, Map< String, List< Point2D.Double > > normalized_path_map ) throws Exception {
		if ( null == lastNode || null == nextNode ) return;

		count = LAME_TWEEN_COUNT;

		double diff = ( nextKey - lastKey );
		for ( int i = 0 ; i < count ; i++ ) {
			double percent = ( i + 1 ) / ( double ) ( count + 1 );

			double frame_id = lastKey + diff * percent;
			String newLabel = "inbetween_" + frame_id;

			Node newLayer = lastNode.cloneNode( true );

			newLayers.put( frame_id, newLayer );
			this.log( "add layer' " + frame_id + ", percent:" + percent );

			this.set( newLayer, "inkscape:label", newLabel );
			this.set( newLayer, "id", newLabel );

			this.log( "creating " + newLabel );

			for ( String title : titles ) {
				List< Point2D.Double > previousPath = normalized_path_map.get( lastId + "\t" + title );
				if ( null == previousPath ) {
					this.log( title + " not found in " + lastId + ", skipping it" );
					continue;
				}

				List< Point2D.Double > nextPath = normalized_path_map.get( nextId + "\t" + title );
				if ( null == nextPath ) {
					this.log( title + " not found in " + nextId + ", keep on truckin'" );
					continue;
				}

				Node currentPath  = this.node( newLayer, "//path[./title = '" + title + "']" );
				if ( null == currentPath ) {
					this.log( title + " not found in the new layer, and it should have been! ur boned!" );
					continue;
				}

				List< Point2D.Double > newPath = new ArrayList< Point2D.Double >();
				int pointIndex = 0;

				Point2D.Double previousPrior = null;
				Point2D.Double nextPrior = null;
				Point2D.Double currentPrior = null;

				for ( Point2D.Double previousPoint : previousPath ) {
					Point2D.Double nextPoint = null;
					try { 
						nextPoint = nextPath.get( pointIndex );
					} catch ( Exception e ) {
						if ( null == nextPoint ) {
							// should not happen
							this.log( "ERROR: missing point " + pointIndex + " in " + nextId + "," + title + " only " + nextPath.size() + " points" );
							continue;
						}
					}
					pointIndex++;

					// finally: make the inbetween points
					Point2D.Double currentPoint = null;

					// for the first point, do a straight inbetween
					if ( null == previousPrior || null == nextPrior ) {

						Point2D.Double pointDiff = new Point2D.Double(
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

							Point2D.Double previousDiff = new Point2D.Double(
								  previousPoint.getX() - previousPrior.getX()
								, previousPoint.getY() - previousPrior.getY()
							);
							Point2D.Double nextDiff = new Point2D.Double(
								  nextPoint.getX() - nextPrior.getX()
								, nextPoint.getY() - nextPrior.getY()
							);

							Point2D.Double diffDiff = new Point2D.Double(
								  nextDiff.getX() - previousDiff.getX()
								, nextDiff.getY() - previousDiff.getY()
							);

							Point2D.Double currentDiff = new Point2D.Double(
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
					//this.log( "between:" + previousPoint + " to " + nextPoint + " = " + pointDiff + " -> " + currentPoint );

// trying to do relative to last point on each path proved problematic... for for angularr :-(
if ( !LAME_ANGULAR||null == previousPrior ) {

					previousPrior = previousPoint;
					nextPrior = nextPoint;
					currentPrior = currentPoint;
} // snit
				}

				this.set( currentPath, "d", this.toString( newPath, true /* FIXME:  need to track this */  ) );
			}
		}
	}

	public double angle( Point2D.Double a, Point2D.Double b ) {
		// toa = o/a = dy/dx
		double dy = b.getY() - a.getY();
		double dx = b.getX() - a.getX();

		return Math.atan2( dy, dx );
	}

	// http://xmlgraphics.apache.org/batik/using/parsers.html#examples
	public List< Point2D.Double > parsePath( String path ) throws Exception {
		final List< Point2D.Double > serat = new ArrayList< Point2D.Double >();
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

		this.log( "parsing:" + path );

		pp.setPathHandler( ph );
		pp.parse( path );
		
		this.log( "parsed: " + serat );

		return serat;
	}

	public String toString( List< Point2D.Double > points, boolean hadZ ) {
		StringBuilder bob = new StringBuilder( "m" );
		Point2D.Double previous = null;
		for( Point2D.Double current : points ) {
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

	public double pathLength( List< Point2D.Double > points ) {
		double sum = 0;

		Point2D.Double previous = points.get( points.size() - 1 );

		for( Point2D.Double current : points ) {
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
	 * @param points list of Point2D.Double in the original path
	 *
	 * @return new path with uniform distribution of {count} points along the original path
	 *
	 *
	 */
	public List< Point2D.Double > normalizePath( int count, List< Point2D.Double > points ) {
		List< Point2D.Double > path = new ArrayList< Point2D.Double >();

		double pathLength = this.pathLength( points );
		this.log( "path " + pathLength + " and has " + points.size() + " points" );

		Point2D.Double previous = null;

		// first work out how many points per segment

		int sum = 0;

		List< AtomicInteger > pointsPerSegment = new ArrayList< AtomicInteger >();

		previous = points.get( points.size() - 1 );
		for ( Point2D.Double current : points ) {
			double distance = current.distance( previous );
			double percent = distance / pathLength;
			int share = ( int ) ( count * percent ) + 1;
			sum += share;

			//this.trace( distance + " / " + pathLength + " = " + percent + " so " + share + ", " + sum );
			pointsPerSegment.add( new AtomicInteger( share ) );

			previous = current;
		}
		
		this.log( "need to jiggle: " + sum + " versus " + count );

		// "commence the jigglin"

		if( sum > count ) {
			this.jiggleBelly( sum - count, -1, pointsPerSegment );
		} else {
			if ( sum < count ) {
				this.log( "huh... really?" );
				this.jiggleBelly( count - sum, 1, pointsPerSegment );
			}
		}

		// use the per segment counts to interpolate per segment

		sum = 0;

		int segmentIndex = 0;
		previous = points.get( points.size() - 1 );
		for ( Point2D.Double current : points ) {
			int share = pointsPerSegment.get( segmentIndex++ ).intValue();
			if ( 0 == share ) {
				this.log( "ERROR: no points allocated for this segment!!!" );
				return null;
			}

			sum += share;

			double distance = current.distance( previous );
			Point2D.Double pointDiff = new Point2D.Double(
				  ( current.getX() - previous.getX() )
				, ( current.getY() - previous.getY() )
			);
			Point2D.Double mod = new Point2D.Double(
				  pointDiff.getX() / share
				, pointDiff.getY() / share
			);

			//this.log( "norman: from " + previous + " to " + current + " in " + share + " segments: " + mod );

			for ( int i = 0 ; i < share ; i++ ) {

				Point2D.Double nu = new Point2D.Double(
					  previous.getX() + mod.getX() * i
					, previous.getY() + mod.getY() * i
				);

				//this.log( "norman>" + i + " > " + nu );

				path.add( nu );
			}

			previous = current;
		}

		if ( sum != count || path.size() != count ) {
			this.log( "ERROR! path has wrong number of points:" + sum + " vs " + count + " and " + path.size() );
		} else {
			this.log( "new path length is good:" + sum + " vs " + count );
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
		this.log( "by size: " + sizeToSegment + " need " + increment + " x " + count + " times" );

		int lost = 0;
		for ( AtomicInteger atomic : sizeToSegment.values() ) {
			int share = atomic.intValue();

			if ( increment < 0 && share == 2 ) {
				this.log( "ERROR: did not modify enuff and now it is too late!" );
				return;
			}

			int toLose = ( int ) ( count * share / sum ) + 2;
			while ( toLose + lost > count ) {
				toLose--; 
			}
			lost += toLose;

			int nu = ( share + increment * toLose );
			
			//this.trace( "from " + share + ", modify " + toLose + " times to " + nu + ", so far " + lost + " versus " + count  );
			atomic.set( nu );

			if ( lost == count ) {
				return;
			}
			if ( lost > count ) {
				this.log( "ERROR: jiggled to much!" + lost + " verus " + count );
				return;
			}
		}
	}

	public String toString( List< Point2D.Double > normalized, String path ) {
		return this.toString( normalized, path.endsWith( "z" ) );
	}

	public String normalizePath( int count, String path ) throws Exception {
		return this.toString( this.normalizePath( count, this.parsePath( path ) ), path );
	}

	public Node layersPath( Document document, String id, String title ) throws Exception {
		return this.node( document, "//g[@id='" + id + "']/path[./title = '" + title + "']" );
	}

	public void set( Node node, String name, String value ) {
		// thx http://www.mkyong.com/java/how-to-modify-xml-file-in-java-dom-parser/
		node.getAttributes().getNamedItem( name ).setTextContent( value ); // this is awful
	}

	////

	public NodeList nodes( Document document, String expression ) throws Exception {
		//NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
		return ( NodeList ) this.getXpath().compile( expression ).evaluate( document, XPathConstants.NODESET );
	}
	
	public Node node( Object document, String expression ) throws Exception {
		//Node node = (Node) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODE);
		return ( Node ) this.getXpath().compile( expression ).evaluate( document, XPathConstants.NODE );
	}

	public String value( Node node, String item ) {
		return node.getAttributes().getNamedItem( item ).getNodeValue(); // seriously?
	}

	public String value( Document document, String expression ) throws Exception {
		return this.getXpath().compile( expression ).evaluate( document );
	}
	
	/////

	public XPath getXpath() {
		if ( null == this.xpath_ ) {
			this.xpath_ = XPathFactory.newInstance().newXPath();
		}
		return this.xpath_;
	}
	
	public void setXPath( XPath xpath ) {
		this.xpath_ = xpath;
	}
};
