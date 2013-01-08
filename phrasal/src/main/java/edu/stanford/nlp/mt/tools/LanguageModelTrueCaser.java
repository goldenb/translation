package edu.stanford.nlp.mt.tools;

import edu.stanford.nlp.mt.base.AbstractPhraseGenerator;
import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.IStrings;
import edu.stanford.nlp.mt.base.LanguageModel;
import edu.stanford.nlp.mt.base.PhraseAlignment;
import edu.stanford.nlp.mt.base.RawSequence;
import edu.stanford.nlp.mt.base.RichTranslation;
import edu.stanford.nlp.mt.base.Sequence;
import edu.stanford.nlp.mt.base.SimpleSequence;
import edu.stanford.nlp.mt.base.TranslationOption;
import edu.stanford.nlp.mt.decoder.feat.CombinedFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.IncrementalFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.IsolatedPhraseFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.NGramLanguageModelFeaturizer;
import edu.stanford.nlp.mt.decoder.h.IsolatedPhraseForeignCoverageHeuristic;
import edu.stanford.nlp.mt.decoder.inferer.Inferer;
import edu.stanford.nlp.mt.decoder.inferer.InfererBuilderFactory;
import edu.stanford.nlp.mt.decoder.inferer.impl.MultiBeamDecoder;
import edu.stanford.nlp.mt.decoder.recomb.RecombinationFilter;
import edu.stanford.nlp.mt.decoder.recomb.TranslationNgramRecombinationFilter;
import edu.stanford.nlp.mt.decoder.util.Hypothesis;
import edu.stanford.nlp.mt.decoder.util.HypothesisBeamFactory;
import edu.stanford.nlp.mt.decoder.util.Scorer;
import edu.stanford.nlp.mt.decoder.util.UniformScorer;
import edu.stanford.nlp.util.StringUtils;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.LanguageModelFactory;
import eu.monnetproject.translation.TrueCaser;
import eu.monnetproject.translation.monitor.Messages;
import eu.monnetproject.translation.phrasal.lm.ARPALanguageModelFactory;
import eu.monnetproject.translation.phrasal.lm.WrappedLanguageModel;

import java.io.*;
import java.util.*;

/**
 * Language Model based TrueCasing
 *
 * This class implements n-gram language model based truecasing, an approach
 * similar to that seen Lita et al 2003's paper tRuEcasIng.
 *
 * @author danielcer
 *
 */
public class LanguageModelTrueCaser implements TrueCaser {

    private static final int BEAM_SIZE = 50;
    private Inferer<IString, String> inferer;

    public static void main(String args[]) throws Exception {
        //SRILanguageModel.addVocabToIStrings = true;
        if (args.length != 1) {
            System.err
                    .println("Usage:\n\tjava ... TrueCaser (language model) < uncased_input > cased_output");
            System.exit(-1);
        }

        LanguageModelTrueCaser tc = new LanguageModelTrueCaser();
        tc.init(Language.get(args[0]), new ARPALanguageModelFactory());

        // enter main truecasing loop
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(
                System.in, "UTF-8"));
        for (String line; (line = reader.readLine()) != null;) {
            String[] tokens = line.split("\\s+");
            int lineNumber = reader.getLineNumber();
            String[] trg = tc.trueCase(tokens, lineNumber);
            System.out.printf("%s \n", StringUtils.join(trg, " "));
        }

        System.exit(0);
    }

    public void init(Language lang, LanguageModelFactory arpalmFactory) {

        MultiBeamDecoder.MultiBeamDecoderBuilder<IString, String> infererBuilder = (MultiBeamDecoder.MultiBeamDecoderBuilder<IString, String>) InfererBuilderFactory
                .factory(InfererBuilderFactory.MULTIBEAM_DECODER);

        // Read in LM & create LM featurizer

        final eu.monnetproject.translation.LanguageModel model = arpalmFactory.getModel(lang);
        final WrappedLanguageModel wrappedModel = new WrappedLanguageModel(model);
        NGramLanguageModelFeaturizer<IString> lmFeaturizer = new NGramLanguageModelFeaturizer<IString>(wrappedModel, NGramLanguageModelFeaturizer.FEATURE_NAME, false);
        List<IncrementalFeaturizer<IString, String>> listFeaturizers = new LinkedList<IncrementalFeaturizer<IString, String>>();
        listFeaturizers.add(lmFeaturizer);
        CombinedFeaturizer<IString, String> combinedFeaturizer = new CombinedFeaturizer<IString, String>(
                listFeaturizers);

        infererBuilder.setIncrementalFeaturizer(combinedFeaturizer);
        Scorer<String> scorer = new UniformScorer<String>(false);
        infererBuilder.setScorer(scorer);

        // Create truecasing phrase generator
        infererBuilder.setPhraseGenerator(new AllCasePhraseGenerator(
                combinedFeaturizer, scorer));
        infererBuilder
                .setSearchHeuristic(new IsolatedPhraseForeignCoverageHeuristic<IString, String>(
                combinedFeaturizer, scorer));
        List<LanguageModel<IString>> lgModels = new LinkedList<LanguageModel<IString>>();
        lgModels.add(lmFeaturizer.lm);

        // misc. decoder configuration
        RecombinationFilter<Hypothesis<IString, String>> recombinationFilter = new TranslationNgramRecombinationFilter<IString, String>(
                lgModels, Integer.MAX_VALUE);
        infererBuilder.setRecombinationFilter(recombinationFilter);
        infererBuilder.setMaxDistortion(0);
        infererBuilder.setBeamCapacity(BEAM_SIZE);
        infererBuilder.setBeamType(HypothesisBeamFactory.BeamType.sloppybeam);

        // builder decoder
        inferer = infererBuilder.build();
    }

    public String[] trueCase(String[] tokens, int id) {

        Sequence<IString> source = new SimpleSequence<IString>(true,
                IStrings.toIStringArray(tokens));
        RichTranslation<IString, String> translation = inferer.translate(source,
                id - 1, null, null, Inferer.DEFAULT_BEAM_SIZE);

        // manual fix up(s)
        // capitalize the first letter
        String[] trg = translation.translation.toString().split("\\s+");
//    if (trg.length > 0 && trg[0].length() > 0) {
//      String firstLetter = trg[0].substring(0, 1);
//      String rest = trg[0].substring(1, trg[0].length());
//      String capTrg = firstLetter.toUpperCase() + rest;
//      trg[0] = capTrg;
//    }

        return trg;
    }
}

class AllCasePhraseGenerator extends AbstractPhraseGenerator<IString, String> {

    static final String NAME = "AllCasePhrGen";
    Map<String, Set<String>> caseMap = new HashMap<String, Set<String>>();

    public AllCasePhraseGenerator(
            IsolatedPhraseFeaturizer<IString, String> phraseFeaturizer,
            Scorer<String> scorer) {
        super(phraseFeaturizer, scorer);

        // TODO : caseMap should actually examine the language model(s) directly
        // rather than using a dump from IStrings.keySet()
        Set<String> tokens = IString.keySet();

        int n = 0;
        final StringBuilder sb = new StringBuilder();
        // construct uncased to cased map
        for (String token : tokens) {
            sb.append(token.toLowerCase());
            final String tokenLC = sb.toString();

            if (!caseMap.containsKey(tokenLC)) {
                caseMap.put(tokenLC, new TreeSet<String>());
            }
            // add token as is
            caseMap.get(tokenLC).add(token);

            // add all lower case version of token
            caseMap.get(tokenLC).add(tokenLC);

            // add first letter capitalized version of token
            sb.replace(0, 1, tokenLC.substring(0,1).toUpperCase());
            final String capToken = sb.toString();
            //String firstLetter = token.substring(0, 1);
            //String rest = token.substring(1, token.length());
            //String capToken = firstLetter.toUpperCase() + rest;
            caseMap.get(token.toLowerCase()).add(capToken);
            sb.setLength(0);
        }
    }

    public String getName() {
        return NAME;
    }

    public List<TranslationOption<IString>> getTranslationOptions(
            Sequence<IString> sequence) {
        if (sequence.size() != 1) {
            throw new RuntimeException("Subsequence length != 1");
        }
        List<TranslationOption<IString>> list = new LinkedList<TranslationOption<IString>>();
        String token = sequence.get(0).toString().toLowerCase();
        Set<String> casings = caseMap.get(token);
        if (casings == null) {
            casings = new TreeSet<String>();
            casings.add(sequence.get(0).toString());
        }
        RawSequence<IString> rawSource = new RawSequence<IString>(sequence);

        for (String casing : casings) {
            IString[] trgArr = IStrings.toIStringArray(new String[]{casing});
            RawSequence<IString> trg = new RawSequence<IString>(trgArr);
            list.add(new TranslationOption<IString>(new float[0], new String[0], trg,
                    rawSource, PhraseAlignment.getPhraseAlignment("I-I")));
        }
        return list;
    }

    public int longestForeignPhrase() {
        return 1;
    }

    public void setCurrentSequence(Sequence<IString> foreign,
            List<Sequence<IString>> tranList) {
        // no op
    }
}
