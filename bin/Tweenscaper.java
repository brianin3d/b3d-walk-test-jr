/*NaN 2>/dev/null

if [ Tweenscaper.java -nt Tweenscaper.class ] ; then
	javac Tweenscaper.java 
	status=${?}
	if [ 0 != ${status} ] ; then
		exit ${status}
	fi
fi
	
java Tweenscaper ${*}
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Scanner;
import java.util.TreeMap;

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

/** 
 *
 * This will need some explanation...
 *
 * @author Brian Hammond
 *
 */
public class Tweenscaper {
	private XPath xpath_;

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
			double numeric_value = Double.valueOf( numeric_label );

			//this.log( g + ":" + label + " : " + numeric_label + " : " + numeric_value );
			sortedLayers.put( numeric_value, g );
		}
			
		return sortedLayers;
	}

	public Set< String > getTitles( Document document ) throws Exception {
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
				this.set( path, "d", this.normalizePath( this.value( path, "d" ) ) );
			}

			// put a clone of the original into newLayers 

			newLayers.put( layerEntry.getKey(), layerEntry.getValue().cloneNode( true ) );

			// make the inbetween layes

			this.inbetween( document, count, lastValue, lastNode, lastId, layerEntry.getKey(), layerEntry.getValue(), id, titles, newLayers );

			lastValue = layerEntry.getKey();
			lastNode = layerEntry.getValue();
			lastId = id;
		}

		// inbetween for first and last

		this.inbetween( document, count, lastValue, lastNode, lastId, lastValue + 1 /* sigh */, firstNode, firstId, titles, newLayers );

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

	public void inbetween( Document document, int count, double lastKey, Node lastNode, String lastId, double nextKey, Node nextNode, String nextId, Set< String > titles, Map< Double, Node > newLayers ) throws Exception {
		if ( null == lastNode || null == nextNode ) return;

		double diff = ( nextKey - lastKey );
		for ( int i = 0 ; i < count ; i++ ) {
			double frame_id = lastKey + diff * ( i + 1 ) / ( count + 1 );
			String newLabel = "inbetween_" + frame_id;

			Node newLayer = lastNode.cloneNode( true );

			newLayers.put( frame_id, newLayer );
			this.set( newLayer, "inkscape:label", newLabel );
			this.set( newLayer, "id", newLabel );

			this.log( "creating " + newLabel );

			for ( String title : titles ) {
				Node previousPath = this.layersPath( document, lastId, title );
				if ( null == previousPath ) {
					this.log( title + " not found in " + lastId + ", skipping it" );
					continue;
				}

				Node nextPath = this.layersPath( document, nextId, title );
				if ( null == nextPath ) {
					this.log( title + " not found in " + nextId + ", keep on truckin'" );
					continue;
				}

				Node currentPath  = this.node( newLayer, "//path[./title = '" + title + "']" );
				if ( null == currentPath ) {
					this.log( title + " not found in the new layer, and it should have been! ur boned!" );
					continue;
				}
				this.log( "hope this works... " + currentPath );
			}
		}
	}

	public List< Point2D.Double > parsePath( String path ) {
		List< Point2D.Double > serat = new ArrayList< Point2D.Double >();
		//this.log( "xx.1>" + path );

		Scanner words = new Scanner( path ).useDelimiter( "\\s+" );
		while( words.hasNext() ) {
			String word = words.next().toLowerCase();
			if ( "m".equals( word ) || "l".equals( word ) || "z".equals( word ) ) {
				continue; // ok... don't freak out
			}

			// super lazy regex
			if ( !word.matches( "^-*[0-9.]+,-*[0-9.]+$" ) ) {
				this.log( "what in the worlds is a " + word + ", probly something dirty..." );
				continue;
			}

			Scanner point = new Scanner( word ).useDelimiter( "," );
			Point2D.Double dot = new Point2D.Double( point.nextDouble(), point.nextDouble() );
			serat.add( dot );

			//this.log( "xx.2>" + word + " -> " + dot );
		}

		return serat;
	}

	public String makePath( List< Point2D.Double > points, boolean hadZ ) {
		StringBuilder bob = new StringBuilder( "m" );
		for( Point2D.Double current : points ) {
			bob
				.append( " " )
				.append( current.getX() )
				.append( "," )
				.append( current.getY() )
			;
		}

		if ( hadZ ) {
			bob.append( " z" );
		}
		
		return bob.toString();
	}

	public double pathLength( List< Point2D.Double > points ) {
		double sum = 0;

		Point2D.Double previous = null;
		Point2D.Double first = null;

		for( Point2D.Double current : points ) {
			if ( null == first ) {
				first = current;
			}

			if ( null != previous ) {
				sum += current.distance( previous );
			}
			previous = current;
		}

		if ( null != previous && null != first ) {
			sum += first.distance( previous );
		}

		return sum;
	}


	public List< Point2D.Double > normalizePath( int count, List< Point2D.Double > points ) {
		double pathLength = this.pathLength( points );
		this.log( "path " + pathLength + " and has " + points.size() + " points" );

		// TODO
		List< Point2D.Double > path = new ArrayList< Point2D.Double >();

		Point2D.Double previous = null;
		Point2D.Double first = null;

		// steps:
		// 1) allocation of # of points per segment based on ratio of segment length * count
		// 2) make sure the allocation exactly equals count, jiggle as needed
		// 3) use the per segment counts to interpolate per segment
		// 4) party
		
		for ( Point2D.Double current : points ) {
			if ( null == first ) {
				first = current;
			}

			// TODO: interpolate, previous to current
			if ( null != previous ) {
			}

			previous = current;
		}
			
		// TODO: interpolate, previous to first

		//return path;
		return points;
	}

	public String normalizePath( String path ) {
		return this.makePath( 
			this.normalizePath( 100, this.parsePath( path ) ) 
			, path.endsWith( "z" )
		);
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
