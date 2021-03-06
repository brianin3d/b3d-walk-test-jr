package briain3d.animation.tweening;

import java.io.File;

import javax.annotation.Resource;

import javax.xml.transform.stream.StreamResult;

//import org.codehaus.jackson.map.ObjectMapper;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.Assert;

//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

// not related:
// http://stackoverflow.com/questions/777947/creating-animated-gif-with-imageio
// http://elliot.kroo.net/software/java/GifSequenceWriter/

/**
 *
 * 
 *
 * @author Brian Hammond
 *
 */
public class TweenscaperTest {
	@Test 
	public void comingSoon() throws Exception {
		Tweenscaper tweenscaper = new Tweenscaper();
		tweenscaper.setInput( new File( "src/test/data/tween_this.svg" ) );
		tweenscaper.setOutput( new File( "target/test-classes/tweened_that.svg" ) );

		// not implemented yet
		// tweenscaper.setGifOutput( new File( "target/test-classes/tweened_that.gif" ) );
			
		tweenscaper.setInput( new File( "src/test/data/flat_tween_again.svg" ) );


		if ( !true ) {	
			tweenscaper.setPointTweener( new AngularTweener() );
			tweenscaper.setRooted( true ); // pretty much always use this with AngularTweener 
		}

		tweenscaper.setPointCount( 300 );
		tweenscaper.setTweenCount( 5 );

		if ( !true ) {
			tweenscaper.getXmlHelper().setLayerLabelPattern( ".*[23].*" );
			tweenscaper.getXmlHelper().setTitlePattern( "^bleg$" );
			tweenscaper.setPointCount( 300 );
			tweenscaper.setTweenCount( 1 );
		}

		tweenscaper.run();
	}

};
