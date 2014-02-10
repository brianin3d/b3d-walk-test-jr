package briain3d.animation.tweening;

import java.awt.geom.Point2D;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * 
 *
 * @author Brian Hammond
 *
 */
public class PointTweenerTest {

	@Test
	public void testPointTweeners() {
		PointTweener[] tweeners = new PointTweener[] {
			  new DiffTweener()
			, new AngularTweener()
			, new SimpleTweener()
		};

		for ( PointTweener tweener : tweeners ) {
			Point2D result = this.tween( 
				tweener
				, 0.5
				, 1, 0
				, -1, 0
			);

			result = this.triflin( result );
			System.out.println( ">" + tweener + " -> " + result );
		}
	};

	public Point2D triflin( Point2D pt ) {
		return this.triflin( pt, 0.000001 );
	}
	
	public Point2D triflin( Point2D pt, double triflin ) {
		return new Point2D.Double(
			  this.triflin( pt.getX(), triflin ) 
			, this.triflin( pt.getY(), triflin ) 
		);
	}

	public double triflin( double v, double triflin ) {
		return (
			( ( v > 0 && v < triflin ) || ( v < 0 && v < -triflin ) )
			? 0
			: v
		);
	}
	
	public Point2D tween( PointTweener tweener, double percent, double previousPoint_x, double previousPoint_y, double nextPoint_x, double nextPoint_y, double previousPrior_x, double previousPrior_y, double currentPrior_x, double currentPrior_y, double nextPrior_x, double nextPrior_y ) {
		return this.tween(
			  tweener
			, percent
			, this.pt( previousPoint_x, previousPoint_y )
			, this.pt( nextPoint_x,     nextPoint_y )
			, this.pt( previousPrior_x, previousPrior_y )
			, this.pt( currentPrior_x,  currentPrior_y )
			, this.pt( nextPrior_x,     nextPrior_y )
		);
	};

	public Point2D tween( PointTweener tweener, double percent, double previousPoint_x, double previousPoint_y, double nextPoint_x, double nextPoint_y ) {
		return this.tween(
			  tweener
			, percent
			, this.pt( previousPoint_x, previousPoint_y )
			, this.pt( nextPoint_x,     nextPoint_y )
		);
	}

	public Point2D tween( PointTweener tweener, double percent, Point2D previousPoint , Point2D nextPoint ) {
		return this.tween( tweener, percent, previousPoint, nextPoint, null, null, null );
	}
	
	public Point2D tween( PointTweener tweener, double percent, Point2D previousPoint , Point2D nextPoint , Point2D previousPrior , Point2D currentPrior , Point2D nextPrior ) {
		return tweener.tween(
			  percent
			, this.orOrigin( previousPoint )
			, this.orOrigin( nextPoint )
			, this.orOrigin( previousPrior )
			, this.orOrigin( currentPrior )
			, this.orOrigin( nextPrior )
		);
	};

	public final static Point2D ORIGIN = new Point2D.Double( 0, 0 );

	public Point2D orOrigin( Point2D pt ) {
		return null == pt ? ORIGIN : pt;
	};

	public Point2D pt( double x, double y ) {
		return new Point2D.Double( x, y );
	}
};
