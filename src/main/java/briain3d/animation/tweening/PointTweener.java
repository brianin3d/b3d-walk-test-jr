package briain3d.animation.tweening;

import java.awt.geom.Point2D;

/**
 *
 * 
 *
 * @author Brian Hammond
 *
 */
public interface PointTweener {
	public Point2D tween( double percent, Point2D previousPoint, Point2D nextPoint, Point2D previousPrior, Point2D currentPrior, Point2D nextPrior );
};
