package briain3d.animation.tweening.model;

import java.util.LinkedHashMap; // maintains arrival sequence
import java.util.Map;

import org.w3c.dom.Node;

/**
 *
 * <P>
 * Hold the dom node and a map from title to path.
 * </P>
 *
 * @author Brian Hammond
 *
 */
public class Layer {
	private Node node_;
	private Map< String, Path > paths_;

	public Layer() {
	}

	public Layer( Node node ) {
		this.setNode( node );
	}

	public Layer( Node node, Map< String, Path > paths ) {
		this.setNode( node );
		this.setPaths( paths );
	}

	public Node getNode() {
		return this.node_;
	}

	public void setNode( Node node ) {
		this.node_ = node;
	}

	public Map< String, Path > getPaths() {
		return (
				null == this.paths_
				? this.paths_ = this.newPaths()
				: this.paths_
			   );
	}

	public Map< String, Path > newPaths() {
		return new LinkedHashMap< String, Path >();
	}

	public void setPaths( Map< String, Path > paths ) {
		this.paths_ = paths;
	}

	public String toString() {
		return this.toStringBuilder().toString();
	}

	public StringBuilder toStringBuilder() {
		return this.toStringBuilder( new StringBuilder() );
	}

	public StringBuilder toStringBuilder( StringBuilder stringBuilder ) {
		return stringBuilder
			.append( "{\"classname\":\"Layer\"" )
			.append( ", \"node\":\"" )
			.append( this.getNode() )
			.append( "\"" )
			.append( ", \"paths\":\"" )
			.append( this.getPaths() )
			.append( "\"" )
			.append( "}" )
		;
	}

	// you may need deep copies... sorry....
	public Layer copy( Layer that ) {
		this.setNode( that.getNode() );
		this.setPaths( that.getPaths() );
		return this;
	}

};
