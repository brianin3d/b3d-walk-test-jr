package briain3d.animation.tweening;

import java.awt.geom.Point2D;

/**
 *
 * <P>
 * Right in the middle of previousPoint and nextPoint
 * </P>
 *
 * @author Brian Hammond
 *
 */
public class SimpleTweener implements PointTweener {
	public Point2D tween( double percent, Point2D previousPoint, Point2D nextPoint, Point2D previousPrior, Point2D currentPrior, Point2D nextPrior ) {
		Point2D pointDiff = new Point2D.Double(
			  nextPoint.getX() - previousPoint.getX() 
			, nextPoint.getY() - previousPoint.getY()
		);

		return new Point2D.Double(
			  previousPoint.getX() + pointDiff.getX() * percent 
			, previousPoint.getY() + pointDiff.getY() * percent 
		);
	}
};
