package edu.stanford.nlp.mt.decoder.recomb;

import edu.stanford.nlp.mt.decoder.util.Hypothesis;

/**
 * 
 * @author danielcer
 * 
 * @param <TK>
 * @param <FV>
 */
public class TranslationIdentityRecombinationFilter<TK, FV> implements
    RecombinationFilter<Hypothesis<TK, FV>> {
  boolean noisy = false;

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  @Override
  public boolean combinable(Hypothesis<TK, FV> hypA, Hypothesis<TK, FV> hypB) {
    if (hypA.featurizable == null && hypB.featurizable == null)
      return true;
    if (hypA.featurizable == null)
      return false;
    if (hypB.featurizable == null)
      return false;

    if (noisy) {
      System.err.printf("%s vs. %s\n", hypA.featurizable.partialTranslation,
          hypB.featurizable.partialTranslation);
      System.err.printf("tirf equal: %s\n",
          hypA.featurizable.partialTranslation
              .equals(hypB.featurizable.partialTranslation));
    }
    return hypA.featurizable.partialTranslation
        .equals(hypB.featurizable.partialTranslation);
  }

  @Override
  public long recombinationHashCode(Hypothesis<TK, FV> hyp) {
    if (hyp.featurizable == null) {
      return 0;
    }
    return hyp.featurizable.partialTranslation.longHashCode();
  }

}
