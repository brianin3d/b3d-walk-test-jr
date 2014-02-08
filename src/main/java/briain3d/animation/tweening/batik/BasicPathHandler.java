package briain3d.animation.tweening.batik;

import java.awt.geom.Point2D;

import java.util.ArrayList;
import java.util.List;

import org.apache.batik.parser.DefaultPathHandler;
import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PathParser;

/**
 *
 * <P>
 * Uses a simplistic "turtle graphics" approach that 
 * only handles straight lines... :-(
 * </P>
 *
 * <P>
 * It's also not thread safe.
 * </P>
 *
 * <P>
 * http://xmlgraphics.apache.org/batik/using/parsers.html#examples
 * </P>
 *
 * @author Brian Hammond
 *
 */
public class BasicPathHandler extends DefaultPathHandler {
	private float ax = 0;
	private float ay = 0;
	private List< Point2D > points_;

	////

	public BasicPathHandler() {
		this.clear();
	}

	public void clear() {
		this.ax = this.ay = 0;
		this.setPoints( new ArrayList< Point2D >() );
	}

	////

	public List< Point2D > parse( String path ) throws Exception {
		this.clear();

		PathParser pathParser = new PathParser();
		pathParser.setPathHandler( this );
		pathParser.parse( path );

		return this.getPoints();
	}

	////

	public void movetoRel( float xo, float yo ) throws ParseException { 
		this.movetoAbs( this.ax + xo, this.ay + yo ); 
	}

	public void linetoRel( float xo, float yo ) throws ParseException { 
		this.linetoAbs( this.ax + xo, this.ay + yo ); 
	}

	public void linetoHorizontalRel( float xo ) throws ParseException {
		this.linetoHorizontalRel( this.ax + xo ); 
	}

	public void linetoVerticalRel( float yo ) throws ParseException { 
		this.linetoVerticalRel( this.ay + yo ); 
	}

	public void linetoHorizontalAbs( float x ) throws ParseException { 
		this.linetoAbs( x, this.ay ); 
	}

	public void linetoVerticalAbs( float y ) throws ParseException { 
		this.linetoAbs( this.ax, y ); 
	}

	public void linetoAbs( float x, float y ) throws ParseException { 
		this.movetoAbs( x, y ); 
	}

	public void movetoAbs( float x, float y ) throws ParseException { 
		this.ax = x; 
		this.ay = y; 
		this.getPoints().add( new Point2D.Double( this.ax, this.ay ) ); 
	}

	////

	public List< Point2D > getPoints() {
		return this.points_;
	}
	
	public void setPoints( List< Point2D > points ) {
		this.points_ = points;
	}
};
