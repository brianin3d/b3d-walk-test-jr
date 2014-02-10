package briain3d.animation.tweening;

import briain3d.animation.tweening.model.Layer;
import briain3d.animation.tweening.model.Path;

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

	private transient XPath xpath_;
	private transient DocumentBuilderFactory documentBuilderFactory_;
	private transient DocumentBuilder documentBuilder_;

	private PathNormalizer pathNormalizer_;

	public static final String DEFAULT_LAYER_LABEL_PATTERN = ".*[0-9]+.*";
	private String layerLabelPattern_ = DEFAULT_LAYER_LABEL_PATTERN;

	public static final String DEFAULT_TITLE_PATTERN = ".*";
	private String titlePattern_ = DEFAULT_TITLE_PATTERN;

	// TODO: make member variables and just uses these as defaults
	public static final String INKSCAPE_LAYER_EXPRESSION = "//g[@groupmode='layer']";
	public static final String INKSCAPE_LAYER_ID = "inkscape:label";
	public static final String INKSCAPE_LAYER_PATHS_EXPRESSION = "path[./title != '']";
	public static final String INKSCAPE_PATH_IN_LAYER_EXPRESSION_TEMPLATE = "//g[@id='::ID::']/path[./title = '::TITLE::']";
	public static final String INKSCAPE_PATH_TITLE_EXPRESSION = "//path/title/text()";

	////
	// more inkscape specific

	public Node layersPath( Document document, String id, String title ) throws Exception {
		return this.node( document, this.template( INKSCAPE_PATH_IN_LAYER_EXPRESSION_TEMPLATE, "ID", id, "TITLE", title ) );
	}

	////

	public double idToDouble( String id ) {
		return Double.valueOf( id.replaceAll( "^[^0-9]*", "" ).replaceAll( "[^0-9.].*$", "" ) );
	}

	public boolean layerMatches( String label ) {
		return label.matches( this.getLayerLabelPattern() );
	}

	public boolean titleMatches( String value ) {
		return value.matches( this.getTitlePattern() );
	}

	////

	public Map< Double, Layer > getLayers( int pointCount, Document document ) throws Exception {
		Map< Double, Layer > sortedLayers = new TreeMap<Double, Layer>();

		NodeList layers = this.nodes( document, INKSCAPE_LAYER_EXPRESSION );
		for ( int i = 0 ; i < layers.getLength() ; i++ ) {
			Node g = layers.item( i );
			String label = this.get( g, INKSCAPE_LAYER_ID );

			if ( !this.layerMatches( label ) ) {
				LOGGER.info( "label does not match pattern, skipping layer " + label );
				continue;
			}

			try {
				double value = this.idToDouble( label );

				Layer layer = this.makeLayer( pointCount, g, label );
				if ( null == layer ) {
					LOGGER.info( "skipping layer " + label );
				} else {
					LOGGER.trace( "layer: " + label + " @" + value + "TODO" );
					sortedLayers.put( value, layer );
				}
			} catch ( Exception e ) {
				LOGGER.info( "skipping layer " + label );
				continue;
			}
		}
			
		return sortedLayers;
	}

	public Layer makeLayer( int pointCount, Node g, String label ) throws Exception {
		NodeList paths = this.nodes( g, INKSCAPE_LAYER_PATHS_EXPRESSION );
		if ( 0 == paths.getLength() ) {
			LOGGER.info( "no titled paths found in" + label + ", ignoring it" );
			return null;
		}
		LOGGER.info( "found " + paths.getLength() + " in " + label );

		Layer layer = new Layer( g );
		for ( int j = 0 ; j < paths.getLength() ; j++ ) {
			Node path = paths.item( j );
			String title = this.value( path, "title" ).trim();

			if ( title.isEmpty() || !this.titleMatches( title ) ) {
				LOGGER.info( "skipping path " + title );
				continue; 
			}
				
			Path nuPath = new Path(
				this.getPathNormalizer().normalizePath(
					pointCount
					,  this.get( path, "d" )
				)
			);

			layer.getPaths().put( title, nuPath );
			LOGGER.info( "hi>" + path + " x " + title );
		}

		return layer.getPaths().isEmpty() ? null : layer;
	}

	////

	public String get( Node node, String name ) { 
		return node.getAttributes().getNamedItem( name ).getNodeValue();
	}

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

	// document should be a Document or Node
	public NodeList nodes( Object document, String expression ) throws Exception {
		return ( NodeList ) this.getXpath().compile( expression ).evaluate( document, XPathConstants.NODESET );
	}

	// document should be a Document or Node
	public Node node( Object document, String expression ) throws Exception {
		return ( Node ) this.getXpath().compile( expression ).evaluate( document, XPathConstants.NODE );
	}

	// document should be a Document or Node
	public String value( Object document, String expression ) throws Exception {
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

	public PathNormalizer getPathNormalizer() {
		return (
			null == this.pathNormalizer_
			? this.pathNormalizer_ = new PathNormalizer()
			: this.pathNormalizer_
		);
	}
	
	public void setPathNormalizer( PathNormalizer pathNormalizer ) {
		this.pathNormalizer_ = pathNormalizer;
	}

	public String getLayerLabelPattern() {
		return this.layerLabelPattern_;
	}
	
	public void setLayerLabelPattern( String layerLabelPattern ) {
		this.layerLabelPattern_ = layerLabelPattern;
	}
	
	public String getTitlePattern() {
		return this.titlePattern_;
	}
	
	public void setTitlePattern( String titlePattern ) {
		this.titlePattern_ = titlePattern;
	}
};
