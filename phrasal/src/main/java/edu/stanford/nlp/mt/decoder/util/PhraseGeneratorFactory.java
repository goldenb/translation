package edu.stanford.nlp.mt.decoder.util;

import java.util.*;
import java.io.*;

import edu.stanford.nlp.mt.base.CombinedPhraseGenerator;
import edu.stanford.nlp.mt.base.IdentityPhraseGenerator;
import edu.stanford.nlp.mt.base.FlatPhraseTable;
import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.SymbolFilter;
import edu.stanford.nlp.mt.base.DTUTable;
import edu.stanford.nlp.mt.decoder.feat.IsolatedPhraseFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.UnknownWordFeaturizer;
import edu.stanford.nlp.mt.tools.NumericFilter;
//import edu.stanford.nlp.mt.base.DynamicPhraseTable;
//import edu.stanford.nlp.mt.base.NewDynamicPhraseTable;

/**
 * 
 * @author Daniel Cer
 */
public class PhraseGeneratorFactory {

  public static final String CONCATENATIVE_LIST_GENERATOR = "tablelist";
  public static final String BASIC_AUGMENTED_CONCATENATIVE_LIST_GENERATOR = "augmentedtablelist";
  public static final String PSEUDO_PHARAOH_GENERATOR = "pseudopharaoh";
  public static final String DTU_GENERATOR = "dtu";
  public static final String DYNAMIC_GENERATOR = "dpt";
  public static final String PHAROAH_PHRASE_TABLE = "pharaohphrasetable";
  public static final String PHAROAH_PHRASE_TABLE_ALT = "ppt";
  public static final String NEW_DYNAMIC_GENERATOR = "newdg";

  static public <FV> PhraseGenerator<IString> factory(
      IsolatedPhraseFeaturizer<IString, FV> phraseFeaturizer,
      Scorer<FV> scorer, String... pgSpecs) throws IOException {

    if (pgSpecs.length == 0) {
      throw new RuntimeException(
          "PhraseGenerator specifier is empty. PhraseGenerators "
              + "must be explicitly identified and parameterized.");
    }

    String pgName = pgSpecs[0].toLowerCase();

    if (pgName.equals(CONCATENATIVE_LIST_GENERATOR)
        || pgName.equals(BASIC_AUGMENTED_CONCATENATIVE_LIST_GENERATOR)) {
      List<PhraseGenerator<IString>> phraseTables = new LinkedList<PhraseGenerator<IString>>();

      for (int i = 1; i < pgSpecs.length; i++) {
        String[] fields = pgSpecs[i].split(":");
        if (fields.length != 2) {
          throw new RuntimeException(String.format(
              "Expected the pair (phrasetable type):(filename), got '%s'",
              pgSpecs[i]));
        }
        String type = fields[0].toLowerCase();
        String filename = fields[1];
        if (type.equals(PHAROAH_PHRASE_TABLE)
            || type.equals(PHAROAH_PHRASE_TABLE_ALT)) {
          phraseTables.add((new FlatPhraseTable<FV>(phraseFeaturizer, scorer,
              filename)));
        } else if (type.equals(DTU_GENERATOR)) {
          phraseTables
              .add((new DTUTable<FV>(phraseFeaturizer, scorer, filename)));
        } else {
          throw new RuntimeException(String.format(
              "Unknown phrase table type: '%s'\n", type));
        }
      }
      if (pgName.equals(CONCATENATIVE_LIST_GENERATOR)) {
        return new CombinedPhraseGenerator<IString>(phraseTables,
            CombinedPhraseGenerator.Type.CONCATENATIVE);
      } else if (pgName.equals(BASIC_AUGMENTED_CONCATENATIVE_LIST_GENERATOR)) {
        List<PhraseGenerator<IString>> augmentedList = new LinkedList<PhraseGenerator<IString>>();

        // special purpose numeric identity phrase translator
        augmentedList.add(new IdentityPhraseGenerator<IString, FV>(
            phraseFeaturizer, scorer, new NumericFilter<IString>()));

        // user specified translation tables and equal in ranking special
        // purpose phrase generators
        List<PhraseGenerator<IString>> userEquivList = new LinkedList<PhraseGenerator<IString>>(
            phraseTables); // user phrase tables

        userEquivList.add(new IdentityPhraseGenerator<IString, FV>(
            phraseFeaturizer, scorer, new SymbolFilter<IString>())); // symbol
                                                                     // identity
                                                                     // phrase
                                                                     // generator

        CombinedPhraseGenerator<IString> equivUserRanking = new CombinedPhraseGenerator<IString>(
            userEquivList);
        augmentedList.add(equivUserRanking);

        // catch all foreign phrase identity generator
        augmentedList.add(new IdentityPhraseGenerator<IString, FV>(
            phraseFeaturizer, scorer));

        return new CombinedPhraseGenerator<IString>(augmentedList,
            CombinedPhraseGenerator.Type.STRICT_DOMINANCE);
      }
    } else if (pgName.equals(PSEUDO_PHARAOH_GENERATOR)
        || pgName.equals(DTU_GENERATOR)) {

      boolean withGaps = pgName.equals(DTU_GENERATOR);

      List<PhraseGenerator<IString>> pharoahList = new LinkedList<PhraseGenerator<IString>>();
      List<PhraseGenerator<IString>> finalList = new LinkedList<PhraseGenerator<IString>>();
      if (pgSpecs.length < 2) {
        throw new RuntimeException("A phrase table filename must be specified.");
      }
      if (pgSpecs.length > 3) {
        throw new RuntimeException("Unrecognized additional material.");
      }
      int phraseLimit = -1;
      if (pgSpecs.length == 3) {
        String phraseLimitStr = pgSpecs[2];
        try {
          phraseLimit = Integer.parseInt(phraseLimitStr);
        } catch (NumberFormatException e) {
          throw new RuntimeException(
              String
                  .format(
                      "Specified phrase limit, %s, can not be parsed as an integer value\n",
                      phraseLimitStr));
        }
      }

      String[] filenames = pgSpecs[1].split(System
          .getProperty("path.separator"));
      for (String filename : filenames) {
        // System.err.printf("loading pt: %s\n", filename);
        if (withGaps)
          pharoahList.add(new DTUTable<FV>(phraseFeaturizer, scorer, filename));
        else
          pharoahList.add(new FlatPhraseTable<FV>(phraseFeaturizer, scorer,
              filename));
      }

      finalList.add(new CombinedPhraseGenerator<IString>(pharoahList,
          CombinedPhraseGenerator.Type.CONCATENATIVE));

      finalList.add(new IdentityPhraseGenerator<IString, FV>(phraseFeaturizer,
          scorer, UnknownWordFeaturizer.UNKNOWN_PHRASE_TAG));

      CombinedPhraseGenerator.Type combinationType = withGaps ? CombinedPhraseGenerator.Type.CONCATENATIVE
          : CombinedPhraseGenerator.Type.STRICT_DOMINANCE;
      if (phraseLimit == -1) {
        return new CombinedPhraseGenerator<IString>(finalList, combinationType);
      } else {
        return new CombinedPhraseGenerator<IString>(finalList, combinationType,
            phraseLimit);
      }
    }

    throw new RuntimeException(String.format("Unknown phrase generator '%s'\n",
        pgName));
  }
}
