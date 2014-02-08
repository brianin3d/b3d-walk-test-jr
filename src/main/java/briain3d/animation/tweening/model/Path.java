package briain3d.animation.tweening.model;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * <p>
 * This represents an SVG path as a list of Point2D
 * objects
 * </p>
 *
 * @author Brian Hammond
 *
 */
public class Path {
	private List< Point2D > points_;

	public Path() {
	}

	public Path( List< Point2D > points ) {
		this.setPoints( points );
	}

	public List< Point2D > getPoints() {
		return (
				null == this.points_
				? this.points_ = this.newPoints()
				: this.points_
			   );
	}

	public List< Point2D > newPoints() {
		return new ArrayList< Point2D >();
	}

	public void setPoints( List< Point2D > points ) {
		this.points_ = points;
	}

	public String toString() {
		return this.toStringBuilder().toString();
	}

	public StringBuilder toStringBuilder() {
		return this.toStringBuilder( new StringBuilder() );
	}

	public StringBuilder toStringBuilder( StringBuilder stringBuilder ) {
		return stringBuilder
			.append( "{\"classname\":\"Path\"" )
			.append( ", \"points\":\"" )
			.append( this.getPoints() )
			.append( "\"" )
			.append( "}" )
		;
	}

	// you may need deep copies... sorry....
	public Path copy( Path that ) {
		this.setPoints( that.getPoints() );
		return this;
	}
};
