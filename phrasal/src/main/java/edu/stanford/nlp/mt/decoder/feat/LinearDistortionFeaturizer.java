package edu.stanford.nlp.mt.decoder.feat;

import java.util.List;

import edu.stanford.nlp.mt.base.ConcreteTranslationOption;
import edu.stanford.nlp.mt.base.FeatureValue;
import edu.stanford.nlp.mt.base.Featurizable;
import edu.stanford.nlp.mt.base.Sequence;

/**
 * 
 * @author danielcer
 * 
 * @param <TK>
 */
public class LinearDistortionFeaturizer<TK> implements
    IncrementalFeaturizer<TK, String> {

  public static final String DEBUG_PROPERTY = "DebugLinearDistortionFeaturizer";
  public static final boolean DEBUG = Boolean.parseBoolean(System.getProperty(
      DEBUG_PROPERTY, "false"));
  public static final String FEATURE_NAME = "LinearDistortion";

  @Override
  public FeatureValue<String> featurize(Featurizable<TK, String> f) {
    return new FeatureValue<String>(FEATURE_NAME, -1.0 * f.linearDistortion);
  }

  @Override
  public List<FeatureValue<String>> listFeaturize(Featurizable<TK, String> f) {
    return null;
  }

  @Override
  public void initialize(List<ConcreteTranslationOption<TK>> options,
      Sequence<TK> foreign) {
  }

  public void reset() {
  }
}
