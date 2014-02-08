package briain3d.animation.tweening;

import briain3d.animation.tweening.batik.BasicPathHandler;
import briain3d.animation.tweening.model.Path;

import java.awt.geom.Point2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * 
 *
 * @author Brian Hammond
 *
 */
public class PathNormalizer {
	private static Logger LOGGER = LogManager.getLogger( XmlHelper.class.getSimpleName() );
	
	public List< Point2D > parsePath( String path ) throws Exception {
		return new BasicPathHandler().parse( path ); // lifecyle 
	}

	public double pathLength( Path path ) {
		return pathLength( path.getPoints() );
	}

	public double pathLength( List< Point2D > points ) {
		double sum = 0;

		Point2D previous = points.get( points.size() - 1 );

		for( Point2D current : points ) {
			sum += current.distance( previous );
			previous = current;
		}

		return sum;
	}

	public List< Point2D > normalizePath( int count, String path ) throws Exception {
		return this.normalizePath( count, this.parsePath( path ) );
	}

	/**
	 *
	 * <p>
	 * Paths have to start at the same point and be made of straight
	 * segments (inkscape, see extensions, path tools, flatten bezier).
	 * </p>
	 *
	 * <p>
	 * Paths have to start at the same point and be made of straight
	 * This method makes sure they have the same number of points in
	 * a uniform distribution along the path:
	 * </p>
	 *
	 * <OL>
	 *     <LI>allocation of # of points per segment based on ratio of segment length * count</LI>
	 *     <LI>make sure the allocation exactly equals count, jiggle as needed</LI>
	 *     <LI>use the per segment counts to interpolate per segment</LI>
	 * <OL>
	 *
	 * <p>
	 * "When you do things right, people won't be sure you've done anything at all."
	 * </p>
	 *
	 * @param count number of points in the new path
	 * @param points list of Point2D in the original path
	 *
	 * @return new path with uniform distribution of {count} points along the original path
	 *
	 *
	 */
	public List< Point2D > normalizePath( int count, List< Point2D > points ) {
		List< Point2D > path = new ArrayList< Point2D >();

		double pathLength = this.pathLength( points );
		LOGGER.info( "path " + pathLength + " and has " + points.size() + " points" );

		Point2D previous = null;

		// first work out how many points per segment

		int sum = 0;

		List< AtomicInteger > pointsPerSegment = new ArrayList< AtomicInteger >();

		previous = points.get( points.size() - 1 );
		for ( Point2D current : points ) {
			double distance = current.distance( previous );
			double percent = distance / pathLength;
			int share = ( int ) ( count * percent ) + 1;
			sum += share;

			LOGGER.trace( distance + " / " + pathLength + " = " + percent + " so " + share + ", " + sum );
			pointsPerSegment.add( new AtomicInteger( share ) );

			previous = current;
		}
		
		LOGGER.info( "need to jiggle: " + sum + " versus " + count );

		// "commence the jigglin"

		if( sum > count ) {
			this.jiggleBelly( sum - count, -1, pointsPerSegment );
		} else {
			if ( sum < count ) {
				LOGGER.info( "huh... really?" );
				this.jiggleBelly( count - sum, 1, pointsPerSegment );
			}
		}

		// use the per segment counts to interpolate per segment

		sum = 0;

		int segmentIndex = 0;
		previous = points.get( points.size() - 1 );
		for ( Point2D current : points ) {
			int share = pointsPerSegment.get( segmentIndex++ ).intValue();
			if ( 0 == share ) {
				LOGGER.error( ": no points allocated for this segment!!!" );
				return null;
			}

			sum += share;

			double distance = current.distance( previous );
			Point2D pointDiff = new Point2D.Double(
				  ( current.getX() - previous.getX() )
				, ( current.getY() - previous.getY() )
			);
			Point2D mod = new Point2D.Double(
				  pointDiff.getX() / share
				, pointDiff.getY() / share
			);

			LOGGER.trace( "norman: from " + previous + " to " + current + " in " + share + " segments: " + mod );

			for ( int i = 0 ; i < share ; i++ ) {

				Point2D nu = new Point2D.Double(
					  previous.getX() + mod.getX() * i
					, previous.getY() + mod.getY() * i
				);

				LOGGER.trace( "norman>" + i + " > " + nu );

				path.add( nu );
			}

			previous = current;
		}

		if ( sum != count || path.size() != count ) {
			LOGGER.error( "path has wrong number of points:" + sum + " vs " + count + " and " + path.size() );
		} else {
			LOGGER.info( "new path length is good:" + sum + " vs " + count );
		}

		return path; 
	}

	public void jiggleBelly( int count, int increment, List< AtomicInteger > pointsPerSegment ) {
		Map< Integer, AtomicInteger > sizeToSegment = new TreeMap< Integer, AtomicInteger >();
		double sum = 0;
		for ( AtomicInteger atomic : pointsPerSegment ) {
			sum += atomic.intValue();
			sizeToSegment.put( -atomic.intValue(), atomic );
		}
		LOGGER.info( "by size: " + sizeToSegment + " need " + increment + " x " + count + " times" );

		int lost = 0;
		for ( AtomicInteger atomic : sizeToSegment.values() ) {
			int share = atomic.intValue();

			if ( increment < 0 && share == 2 ) {
				LOGGER.error( "ERROR: did not modify enuff and now it is too late!" );
				return;
			}

			int toLose = ( int ) ( count * share / sum ) + 2;
			while ( toLose + lost > count ) {
				toLose--; 
			}
			lost += toLose;

			int nu = ( share + increment * toLose );
			
			LOGGER.trace( "from " + share + ", modify " + toLose + " times to " + nu + ", so far " + lost + " versus " + count  );
			atomic.set( nu );

			if ( lost == count ) {
				return;
			}
			if ( lost > count ) {
				LOGGER.error( "jiggled to much!" + lost + " verus " + count );
				return;
			}
		}
	}

	public String toString( Path path ) {
		return this.toString( path, true );
	}

	public String toString( Path path, boolean hadZ ) {
		return this.toString( path.getPoints(), hadZ );
	}

	public String toString( List< Point2D > points, boolean hadZ ) {
		StringBuilder bob = new StringBuilder( "m" );
		Point2D previous = null;
		for( Point2D current : points ) {
			double x = current.getX();
			double y = current.getY();
			if ( null != previous ) {
				// make this relative
				x -= previous.getX();
				y -= previous.getY();
			}
			previous = current;
			bob
				.append( " " )
				.append( x )
				.append( "," )
				.append( y );
			;
		}

		if ( hadZ ) {
			bob.append( " z" );
		}
		
		return bob.toString();
	}
};
