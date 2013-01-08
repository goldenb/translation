package edu.stanford.nlp.mt.decoder.feat;

/**
 * IncrementalFeaturizer that should be cloned
 * 
 * @author Michel Galley
 */
public interface ClonedFeaturizer<TK, FV> extends
    IncrementalFeaturizer<TK, FV>, Cloneable {

  public Object clone() throws CloneNotSupportedException;

}
