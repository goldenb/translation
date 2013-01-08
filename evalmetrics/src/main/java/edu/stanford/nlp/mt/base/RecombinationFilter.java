package edu.stanford.nlp.mt.base;

/**
 * 
 * @author danielcer
 */
public interface RecombinationFilter<S> extends Cloneable {
  /**
	 * 
	 */
  boolean combinable(S hypA, S hypB);

  /**
	 * 
	 */
  long recombinationHashCode(S hyp);

  public Object clone() throws CloneNotSupportedException;
}
