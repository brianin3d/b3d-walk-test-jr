package briain3d.animation.tweening;

import java.awt.geom.Point2D;

/**
 * 
 * uses diff tweening... slightly better than angular
 *
 * @author Brian Hammond
 *
 */
public class DiffTweener implements PointTweener {
	public Point2D tween( double percent, Point2D previousPoint, Point2D nextPoint, Point2D previousPrior, Point2D currentPrior, Point2D nextPrior ) {

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
};
