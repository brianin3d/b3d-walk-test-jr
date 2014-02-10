package briain3d.animation.tweening;

import java.awt.geom.Point2D;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * this uses angular difference and distance... has problems
 *
 * @author Brian Hammond
 *
 */
public class AngularTweener implements PointTweener {
	private static Logger LOGGER = LogManager.getLogger( Tweenscaper.class.getSimpleName() );


	public Point2D tween( double percent, Point2D previousPoint, Point2D nextPoint, Point2D previousPrior, Point2D currentPrior, Point2D nextPrior ) {
		double previousDistance = previousPoint.distance( previousPrior );
		double previousAngle = this.angle( previousPrior, previousPoint );

		double nextDistance = nextPoint.distance( nextPrior );
		double nextAngle      = this.angle( nextPrior, nextPoint );

		double diffDistance = nextDistance - previousDistance;
		double diffAngle    = nextAngle    - previousAngle;

		double currentDistance = previousDistance + diffDistance * percent;
		double currentAngle =    previousAngle    + diffAngle    * percent;

		LOGGER.trace( "{percent:" + percent + ", previousPoint:" + previousPoint + ", nextPoint:" + nextPoint + ", previousPrior:" + previousPrior + ", currentPrior:" + currentPrior + ", nextPrior:" + nextPrior + "}" );
		LOGGER.trace( "{previousAngle:" + previousAngle + ", nextAngle:" + nextAngle + ", diffAngle:" + diffAngle + ", currentAngle:" + currentAngle + "}" );
		LOGGER.trace( "{previousDistance:" + previousDistance + ", nextDistance:" + nextDistance + ", diffDistance:" + diffDistance + ", currentDistance:" + currentDistance + "}" );

		return this.triggy( currentPrior, currentAngle, currentDistance );
	}

	public Point2D triggy( Point2D a, double angle, double distance ) {
		return new Point2D.Double(
			  a.getX() + Math.cos( angle ) * distance
			, a.getY() + Math.sin( angle ) * distance
		);
	}

	public double angle( Point2D a, Point2D b ) {
		double dx = b.getX() - a.getX();
		double dy = b.getY() - a.getY();

		
	    double angle = Math.atan2( dy,dx );

		//return angle > 0 ? angle : Math.PI * 2 + angle; // not sure abou this...
			//http://stackoverflow.com/questions/1311049/how-to-map-atan2-to-degrees-0-360
//http://stackoverflow.com/questions/1707151/finding-angles-0-360
		//if x < 0 add 180 to the angle
		//else if y < 0 add 360 to the angle.
/*
		if ( dx < 0 ) {
			angle += Math.PI;
		} else {
			if ( dy < 0 ) {
				angle += Math.PI *2;
			}
		}
*/
		//For those not comfortable with this notation, and without the conversion to degrees built in: 
			
		//	if(x>0) {radians = x;} else {radians = 2*PI + x;} 
	//		if(angle>0) {angle = angle;} else {angle = 2*Math.PI + angle;} 

		//if ( angle < 0 ) angle += 2*Math.PI;
		
		//so we are just adding 2PI to the result if it is less than 0. –  David Doria Sep 25 '12 at 19:05


		return angle;
/*
   float PointPairToBearingDegrees(CGPoint startingPoint, CGPoint endingPoint) {
		// get origin point to origin by subtracting end from start
	   CGPoint originPoint = CGPointMake(endingPoint.x - startingPoint.x, endingPoint.y - startingPoint.y); 

	   // get bearing in radians
	   float bearingRadians = atan2f(originPoint.y, originPoint.x); 

	   float bearingDegrees = bearingRadians * (180.0 / M_PI); // convert to degrees
	   bearingDegrees = (bearingDegrees > 0.0 ? bearingDegrees : (360.0 + bearingDegrees)); // correct discontinuity
	   return bearingDegrees;
   }
					   */

	}
};
