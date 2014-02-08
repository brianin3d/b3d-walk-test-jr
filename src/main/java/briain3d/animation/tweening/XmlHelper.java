package briain3d.animation.tweening;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * <P>
 * This guy handles a lot of the nasty XML grunt
 * work.
 * </P>
 *
 * <P>
 * Thanks to http://www.mkyong.com/java/how-to-modify-xml-file-in-java-dom-parser/
 * </P>
 *
 * @author Brian Hammond
 *
 */
public class XmlHelper {
	private static Logger LOGGER = LogManager.getLogger( XmlHelper.class.getSimpleName() );

	private XPath xpath_;
	private DocumentBuilderFactory documentBuilderFactory_;
	private DocumentBuilder documentBuilder_;

	// TODO: make member variables and just uses these as defaults
	public static final String INKSCAPE_LAYER_ID = "inkscape:label";
	public static final String INKSCAPE_LAYER_EXPRESSION = "//g[@groupmode='layer']";
	public static final String INKSCAPE_PATH_TITLE_EXPRESSION = "//path/title/text()";
	public static final String INKSCAPE_PATH_IN_LAYER_EXPRESSION_TEMPLATE = "//g[@id='::ID::']/path[./title = '::TITLE::']";

	////
	// more inkscape specific

	public Node layersPath( Document document, String id, String title ) throws Exception {
		return this.node( document, this.template( INKSCAPE_PATH_IN_LAYER_EXPRESSION_TEMPLATE, "ID", id, "TITLE", title ) );
	}

	public Map<Double, Node> getSortedLayers( Document document ) throws Exception {
		Map<Double, Node> sortedLayers = new TreeMap<Double, Node>();
		NodeList layers = this.nodes( document, INKSCAPE_LAYER_EXPRESSION );

		for ( int i = 0 ; i < layers.getLength() ; i++ ) {
			Node g = layers.item( i );
			String label = g.getAttributes().getNamedItem( INKSCAPE_LAYER_ID ).getNodeValue();
			String numeric_label = label.replaceAll( "^[^0-9]*", "" ).replaceAll( "[^0-9.].*$", "" );
			try {
				double numeric_value = Double.valueOf( numeric_label );
				LOGGER.trace( g + ":" + label + " : " + numeric_label + " : " + numeric_value );
				sortedLayers.put( numeric_value, g );
			} catch ( Exception e ) {
				LOGGER.info( "skipping layer " + label );
			}
		}
			
		return sortedLayers;
	}

	public Set< String > getTitles( Document document ) throws Exception {
		NodeList titles = this.nodes( document, INKSCAPE_PATH_TITLE_EXPRESSION );
		Set< String > uniqueTitles = new LinkedHashSet< String >();

		for ( int i = 0 ; i < titles.getLength() ; i++ ) {
			String title = titles.item( i ).getNodeValue().trim();
			if ( !title.isEmpty() ) {
				uniqueTitles.add( title );
			}
		}

		return uniqueTitles;
	}

	////

	public void set( Node node, String name, String value ) {
		node.getAttributes().getNamedItem( name ).setTextContent( value );
	}

	////

	public Document read( InputStream in ) throws Exception {
		return this.getDocumentBuilder().parse( in );
	}

	public Document read( InputStream in, String derp ) throws Exception {
		return this.getDocumentBuilder().parse( in, derp );
	}

	public Document read( String in ) throws Exception {
		return this.getDocumentBuilder().parse( in );
	}

	public Document read( File in ) throws Exception {
		return this.getDocumentBuilder().parse( in );
	}

	////


	public void write( Document document, StreamResult out ) throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource( document );
		transformer.transform( source, out );
	}

	////

	public NodeList nodes( Document document, String expression ) throws Exception {
		return ( NodeList ) this.getXpath().compile( expression ).evaluate( document, XPathConstants.NODESET );
	}

	public Node node( Object document, String expression ) throws Exception {
		return ( Node ) this.getXpath().compile( expression ).evaluate( document, XPathConstants.NODE );
	}

	public String value( Node node, String item ) {
		return node.getAttributes().getNamedItem( item ).getNodeValue(); // seriously?
	}

	public String value( Document document, String expression ) throws Exception {
		return this.getXpath().compile( expression ).evaluate( document );
	}

	////
	
	public String template( String template, Object ... junk ) {
		if ( 0 != junk.length % 2 ) {
			throw new IllegalArgumentException( "should have even number of arguments" );
		}
		for ( int i = 0 ; i < junk.length ; i+= 2 ) {
			template = template.replaceAll( "::" + junk[ i ] + "::", junk[ i + 1 ].toString() );
		}
		return template;
	}

	////

	public XPath getXpath() {
		return (
			null == this.xpath_
			? this.xpath_ = XPathFactory.newInstance().newXPath()
			: this.xpath_
		);
	}
	
	public void setXPath( XPath xpath ) {
		this.xpath_ = xpath;
	}

	public DocumentBuilderFactory getDocumentBuilderFactory() {
		return (
			null == this.documentBuilderFactory_
			? this.documentBuilderFactory_ = DocumentBuilderFactory.newInstance()
			: this.documentBuilderFactory_
		);
	}
	
	public void setDocumentBuilderFactory( DocumentBuilderFactory documentBuilderFactory ) {
		this.documentBuilderFactory_ = documentBuilderFactory;
	}

	public DocumentBuilder getDocumentBuilder() throws Exception {
		return (
			null == this.documentBuilder_
			? this.documentBuilder_ = this.getDocumentBuilderFactory().newDocumentBuilder()
			: this.documentBuilder_
		);
	}
	
	public void setDocumentBuilder( DocumentBuilder documentBuilder ) {
		this.documentBuilder_ = documentBuilder;
	}
};
