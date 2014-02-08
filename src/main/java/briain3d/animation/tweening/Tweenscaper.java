package briain3d.animation.tweening;

import briain3d.animation.tweening.batik.BasicPathHandler;

import briain3d.animation.tweening.model.Layer;
import briain3d.animation.tweening.model.Path;

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

	private Map< Double, Layer > layers_;
	private Map< Double, Layer > newLayers_;

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
		this.tween();
		this.writeOutput();
	}

	public void writeOutput() throws Exception {
		this.getXmlHelper().write( this.getInput(), this.getOutput() );
	}

	public void tween() throws Exception {
		int count = 1;

		double lastKey = 0;
		Layer lastLayer = null;

		double firstKey = 0;
		Layer firstLayer = null;

		for ( Map.Entry< Double, Layer > layerEntry : this.getLayers().entrySet() ) {
			double key = layerEntry.getKey();
			Layer layer = layerEntry.getValue();

			if ( null == firstLayer ) {
				firstKey = key;
				firstLayer = layer;
			}
			
			// make the inbetween layes

			this.inbetween( lastKey, lastLayer, key, layer );

			// keep the old layer around 

			this.getNewLayers().put( key, layer ); 
			LOGGER.info( "add layer: " + key );

			// keep old values

			lastKey = key;
			lastLayer = layer;
		}

		// inbetween for first and last
			
		this.inbetween( lastKey, lastLayer, lastKey + 1 /* weak */, firstLayer );
				
		// out with the old

		Node svg = null;
		for ( Layer old : this.getLayers().values() ) {
			if ( null == svg ) {
				svg = old.getNode().getParentNode();
			}
			svg.removeChild( old.getNode() ); 
		}

		// in with the new

		for ( Layer newLayer : this.getNewLayers().values() ) {
			svg.appendChild( newLayer.getNode() );
		}
	}

	public void inbetween( double previousKey, Layer previousLayer, double nextKey, Layer nextLayer ) throws Exception {
		if ( null == previousLayer || null == nextLayer ) return;

		int count = LAME_TWEEN_COUNT;

		double diff = ( nextKey - previousKey );
		for ( int i = 0 ; i < count ; i++ ) {
			double percent = ( i + 1 ) / ( double ) ( count + 1 );

			double frame_id = previousKey + diff * percent;
			String newLabel = "inbetween_" + frame_id;

			Node newNode = previousLayer.getNode().cloneNode( true );
			this.getXmlHelper().set( newNode, "inkscape:label", newLabel );
			this.getXmlHelper().set( newNode, "id", newLabel );

			LOGGER.info( "creating " + newLabel );

			Layer newLayer = new Layer( newNode );
			this.getNewLayers().put( frame_id, newLayer );
			LOGGER.info( "add layer> " + frame_id + ", percent:" + percent );

			for ( String title : previousLayer.getPaths().keySet() ) {
				Path previousPath = previousLayer.getPaths().get( title );
				if ( null == previousPath ) {
					LOGGER.info( title + " not found in " + previousKey + ", skipping it!!!" );
					continue;
				}

				Path nextPath = nextLayer.getPaths().get( title );
				if ( null == nextPath ) {
					LOGGER.info( title + " not found in " + nextKey + ", keep on truckin'" );
					continue;
				}

				Node pathNode = this.getXmlHelper().node( newNode, "//path[./title = '" + title + "']" );
				if ( null == pathNode ) {
					LOGGER.info( title + " not found in the new layer, and it should have been! ur boned!" );
					continue;
				}

				Path newPath = this.tween( percent, previousPath, nextPath );
				newLayer.getPaths().put( title, newPath );

				this.getXmlHelper().set( 
					pathNode
					, "d"
					, ( new PathNormalizer() ).toString( newPath, true /* FIXME:  need to track this */  ) 
				);
			}  // each path
		} // each tween
	}

	public Path tween( double percent, Path previousPath, Path nextPath ) {
		Path newPath = new Path();

		int pointIndex = 0;

		Point2D previousPrior = null;
		Point2D nextPrior = null;
		Point2D currentPrior = null;

		for ( Point2D previousPoint : previousPath.getPoints() ) {
			Point2D nextPoint = null;
			try { 
				nextPoint = nextPath.getPoints().get( pointIndex );
			} catch ( Exception e ) {
				if ( null == nextPoint ) {
					LOGGER.error( "missing point " + pointIndex );//+ " in " + nextId + "," + title + " only " + nextPath.size() + " points" );
					continue;
				}
			}
			pointIndex++;

			// finally: make the inbetween points
			Point2D currentPoint = null;

			// for the first point, do a straight inbetween
			if ( null == previousPrior || null == nextPrior ) {
				currentPoint = this.midpoint( percent, previousPoint, nextPoint );
			} else {
				if ( LAME_ANGULAR ) {
					// use angle and distance
					currentPoint = this.angularTween( percent, previousPoint, nextPoint, previousPrior, currentPrior, nextPrior );
				} else {
					currentPoint = this.diffTween( percent, previousPoint, nextPoint, previousPrior, currentPrior, nextPrior );
				}
			}

			newPath.getPoints().add( currentPoint );

			previousPrior = previousPoint;
			nextPrior = nextPoint;
			currentPrior = currentPoint;
		}

		return newPath;
	}

	public Point2D midpoint( double percent, Point2D previousPoint, Point2D nextPoint ) {
		Point2D pointDiff = new Point2D.Double(
			  nextPoint.getX() - previousPoint.getX() 
			, nextPoint.getY() - previousPoint.getY()
		);

		return new Point2D.Double(
			  previousPoint.getX() + pointDiff.getX() * percent 
			, previousPoint.getY() + pointDiff.getY() * percent 
		);
	}

	// this uses angular difference and distance... has problems
	public Point2D angularTween( double percent, Point2D previousPoint, Point2D nextPoint, Point2D previousPrior, Point2D currentPrior, Point2D nextPrior ) {
		double previousDistance = previousPoint.distance( previousPrior );
		double previousAngle = this.angle( previousPrior, previousPoint );

		double nextDistance = nextPoint.distance( nextPrior );
		double nextAngle = this.angle( nextPrior, nextPoint );

		double distanceDiff = nextDistance - previousDistance;
		double angleDiff = nextAngle - previousAngle;

		double distanceCurrent = previousDistance + distanceDiff * percent;
		double angleCurrent = previousAngle + angleDiff * percent;

		return new Point2D.Double(
			currentPrior.getX() + Math.cos( angleCurrent ) * distanceCurrent
			, currentPrior.getY() + Math.sin( angleCurrent ) * distanceCurrent
		);
	}

	public double angle( Point2D a, Point2D b ) {
		// toa = o/a = dy/dx
		double dy = b.getY() - a.getY();
		double dx = b.getX() - a.getX();

		return Math.atan2( dy, dx );
	}
	
	// uses diff tweening... slightly better than angular
	public Point2D diffTween( double percent, Point2D previousPoint, Point2D nextPoint, Point2D previousPrior, Point2D currentPrior, Point2D nextPrior ) {
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

		return new Point2D.Double(
				currentPrior.getX() + currentDiff.getX()
				, currentPrior.getY() + currentDiff.getY()
				);
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

	public Map< Double, Layer > getLayers() throws Exception {
		return (
			null == this.layers_
			? this.layers_ = this.getXmlHelper().getLayers( LAME_POINT_COUNT, this.getInput() )
			: this.layers_
		);
	}
	
	public void setLayers( Map< Double, Layer > layers ) {
		this.layers_ = layers;
	}

	public Map< Double, Layer > getNewLayers() {
		return (
			null == this.newLayers_
			? this.newLayers_ = new TreeMap< Double, Layer >()
			: this.newLayers_
		);
	}
	
	public void setNewLayers( Map< Double, Layer > newLayers ) {
		this.newLayers_ = newLayers;
	}
};
