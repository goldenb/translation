package edu.stanford.nlp.mt.base;

import edu.stanford.nlp.mt.Phrasal;

import java.util.*;
import java.util.zip.GZIPInputStream;
import java.io.*;

/**
 * Similar to MosesLexicalizedReorderingTable, but adds classes not supported in
 * Moses: left-discontinuous and right-discontinuous.
 * 
 * @author danielcer
 * @author Michel Galley
 * 
 */
public class ExtendedLexicalReorderingTable {

  /**
   * Reordering types
   * 
   * <pre>
   * Monotone with Previous      Monotone with Next
   * 
   *   e:  E_0 E_1 E_2            e:  E_0 E_1 E_2
   * f:                         f:         
   *  F_0  PPP                   F_0  
   *  F_1     PPP                F_1      PPP
   *  F_2                        F_2         PPP
   * 
   * 
   * Swap with Previous      Swap with Next
   * 
   *   e:  E_0 E_1 E_2            e:  E_0 E_1 E_2
   * f:                         f:         
   *  F_0      PPP               F_0  
   *  F_1  PPP                   F_1         PPP      
   *  F_2                        F_2      PPP   
   *  
   * Discontinuous with Prev  Discontinuous with Next
   * 
   *   e:  E_0 E_1 E_2            e:  E_0 E_1 E_2
   * f:                         f:         
   *  F_0      PPP               F_0          PPP
   *  F_1                        F_1               
   *  F_2  PPP                   F_2      PPP
   * 
   * </pre>
   * 
   * NonMonotone: Swap <em>or</em> Discontinuous.
   * 
   * @author danielcer
   * 
   */

  public enum ReorderingTypes {
    monotoneWithPrevious, swapWithPrevious, discontinuousWithPrevious, nonMonotoneWithPrevious, monotoneWithNext, swapWithNext, discontinuousWithNext, nonMonotoneWithNext, discontinuous2WithPrevious, discontinuous2WithNext, containmentWithPrevious, containmentWithNext
  }

  enum ConditionTypes {
    f, e, fe
  }

  static final ReorderingTypes[] msdPositionMapping = {
      ReorderingTypes.monotoneWithPrevious, ReorderingTypes.swapWithPrevious,
      ReorderingTypes.discontinuousWithPrevious };

  static final ReorderingTypes[] msdBidirectionalPositionMapping = {
      ReorderingTypes.monotoneWithPrevious, ReorderingTypes.swapWithPrevious,
      ReorderingTypes.discontinuousWithPrevious,
      ReorderingTypes.monotoneWithNext, ReorderingTypes.swapWithNext,
      ReorderingTypes.discontinuousWithNext };

  static final ReorderingTypes[] msd2BidirectionalPositionMapping = {
      ReorderingTypes.monotoneWithPrevious, ReorderingTypes.swapWithPrevious,
      ReorderingTypes.discontinuousWithPrevious,
      ReorderingTypes.discontinuous2WithPrevious,
      ReorderingTypes.monotoneWithNext, ReorderingTypes.swapWithNext,
      ReorderingTypes.discontinuousWithNext,
      ReorderingTypes.discontinuous2WithNext };

  static final ReorderingTypes[] msd2cBidirectionalPositionMapping = {
      ReorderingTypes.monotoneWithPrevious, ReorderingTypes.swapWithPrevious,
      ReorderingTypes.discontinuousWithPrevious,
      ReorderingTypes.discontinuous2WithPrevious,
      ReorderingTypes.containmentWithPrevious,
      ReorderingTypes.monotoneWithNext, ReorderingTypes.swapWithNext,
      ReorderingTypes.discontinuousWithNext,
      ReorderingTypes.discontinuous2WithNext,
      ReorderingTypes.containmentWithNext };

  static final ReorderingTypes[] monotonicityPositionalMapping = {
      ReorderingTypes.monotoneWithPrevious,
      ReorderingTypes.nonMonotoneWithPrevious };

  static final ReorderingTypes[] monotonicityBidirectionalMapping = {
      ReorderingTypes.monotoneWithPrevious,
      ReorderingTypes.nonMonotoneWithPrevious,
      ReorderingTypes.monotoneWithNext, ReorderingTypes.nonMonotoneWithNext };

  static final Map<String, Object> fileTypeToReorderingType = new HashMap<String, Object>();

  static {
    fileTypeToReorderingType.put("msd-fe", msdPositionMapping);
    fileTypeToReorderingType.put("msd-bidirectional-fe",
        msdBidirectionalPositionMapping);
    fileTypeToReorderingType.put("msd2-bidirectional-fe",
        msd2BidirectionalPositionMapping);
    fileTypeToReorderingType.put("msd2c-bidirectional-fe",
        msd2cBidirectionalPositionMapping);
    fileTypeToReorderingType.put("monotonicity-fe",
        monotonicityPositionalMapping);
    fileTypeToReorderingType.put("monotonicity-bidirectional-fe",
        monotonicityBidirectionalMapping);
    fileTypeToReorderingType.put("msd-f", msdPositionMapping);
    fileTypeToReorderingType.put("msd-bidirectional-f",
        msdBidirectionalPositionMapping);
    fileTypeToReorderingType.put("msd2-bidirectional-f",
        msd2BidirectionalPositionMapping);
    fileTypeToReorderingType.put("msd2c-bidirectional-f",
        msd2cBidirectionalPositionMapping);
    fileTypeToReorderingType.put("monotonicity-f",
        monotonicityPositionalMapping);
    fileTypeToReorderingType.put("monotonicity-bidirectional-f",
        monotonicityBidirectionalMapping);
  }

  static final Map<String, ConditionTypes> fileTypeToConditionType = new HashMap<String, ConditionTypes>();

  static {
    fileTypeToConditionType.put("msd-fe", ConditionTypes.fe);
    fileTypeToConditionType.put("msd-bidirectional-fe", ConditionTypes.fe);
    fileTypeToConditionType.put("msd2-bidirectional-fe", ConditionTypes.fe);
    fileTypeToConditionType.put("msd2c-bidirectional-fe", ConditionTypes.fe);
    fileTypeToConditionType.put("monotonicity-fe", ConditionTypes.fe);
    fileTypeToConditionType.put("monotonicity-bidirectional-fe",
        ConditionTypes.fe);
    fileTypeToConditionType.put("msd-f", ConditionTypes.f);
    fileTypeToConditionType.put("msd-bidirectional-f", ConditionTypes.f);
    fileTypeToConditionType.put("msd2-bidirectional-f", ConditionTypes.f);
    fileTypeToConditionType.put("msd2c-bidirectional-f", ConditionTypes.f);
    fileTypeToConditionType.put("monotonicity-f", ConditionTypes.f);
    fileTypeToConditionType.put("monotonicity-bidirectional-f",
        ConditionTypes.f);
  }

  final String filetype;
  final ArrayList<float[]> reorderingScores = new ArrayList<float[]>();

  public final ReorderingTypes[] positionalMapping;
  public final ConditionTypes conditionType;

  private static int[] mergeInts(int[] array1, int[] array2) {
    return new int[] { FlatPhraseTable.foreignIndex.indexOf(array1, true),
        FlatPhraseTable.translationIndex.indexOf(array2, true) };
  }

  public float[] getReorderingScores(int phraseId) {

    int reorderingId = -1;

    if (conditionType == ConditionTypes.f) {
      reorderingId = FlatPhraseTable.translationIndex.get(phraseId)[0];
    } else if (conditionType == ConditionTypes.e) {
      reorderingId = FlatPhraseTable.translationIndex.get(phraseId)[1];
    } else if (conditionType == ConditionTypes.fe) {
      reorderingId = phraseId;
    }

    if (reorderingId < 0)
      return null;

    return reorderingScores.get(reorderingId);
  }

  /**
   * 
   * @throws IOException
   */
  public ExtendedLexicalReorderingTable(String filename) throws IOException {
    String filetype = init(filename, null);
    this.filetype = filetype;
    this.positionalMapping = (ReorderingTypes[]) fileTypeToReorderingType
        .get(filetype);
    this.conditionType = fileTypeToConditionType.get(filetype);

  }

  public ExtendedLexicalReorderingTable(String filename, String desiredFileType)
      throws IOException {
    String filetype = init(filename, desiredFileType);
    if (!desiredFileType.equals(filetype)) {
      throw new RuntimeException(String.format(
          "Reordering file '%s' of type %s not %s\n", filename, filetype,
          desiredFileType));
    }
    this.filetype = filetype;
    this.positionalMapping = (ReorderingTypes[]) fileTypeToReorderingType
        .get(filetype);
    this.conditionType = fileTypeToConditionType.get(filetype);
  }

  private String init(String filename, String type) throws IOException {
    System.gc();
    boolean withGaps = Phrasal.withGaps;
    Runtime rt = Runtime.getRuntime();
    long preTableLoadMemUsed = rt.totalMemory() - rt.freeMemory();
    long startTimeMillis = System.currentTimeMillis();
    System.err.printf("Loading Moses Lexical Reordering Table: %s\n", filename);
    ReorderingTypes[] positionalMapping = null;
    ConditionTypes conditionType = null;
    String selectedFiletype = null;
    if (type == null) {
      for (String filetype : fileTypeToReorderingType.keySet()) {
        if (filename.contains(filetype)) {
          positionalMapping = (ReorderingTypes[]) fileTypeToReorderingType
              .get(filetype);
          conditionType = fileTypeToConditionType.get(filetype);
          selectedFiletype = filetype;
          break;
        }
      }
    } else {
      positionalMapping = (ReorderingTypes[]) fileTypeToReorderingType
          .get(type);
      conditionType = fileTypeToConditionType.get(type);
      selectedFiletype = type;
    }

    if (positionalMapping == null) {
      throw new RuntimeException(String.format(
          "Unable to determine lexical re-ordering file type for: %s\n",
          filename));
    }

    LineNumberReader reader;
    if (filename.endsWith(".gz")) {
      System.err.printf("(unzipping %s)\n", filename);
      reader = new LineNumberReader(new InputStreamReader(new GZIPInputStream(
          new FileInputStream(filename)), "UTF-8"));
    } else {
      reader = new LineNumberReader(new InputStreamReader(new FileInputStream(
          filename), "UTF-8"));
    }

    for (String line = reader.readLine(); line != null; line = reader
        .readLine()) {
      StringTokenizer toker = new StringTokenizer(line);
      List<String> phrase1TokenList = new LinkedList<String>();
      do {
        String token = toker.nextToken();
        if ("|||".equals(token)) {
          break;
        }
        phrase1TokenList.add(token);
      } while (toker.hasMoreTokens());

      if (!toker.hasMoreTokens()) {
        throw new RuntimeException(String.format(
            "Additional fields expected (line %d)", reader.getLineNumber()));
      }

      List<String> phrase2TokenList = new LinkedList<String>();

      if (conditionType == ConditionTypes.fe) {
        do {
          String token = toker.nextToken();
          if ("|||".equals(token)) {
            break;
          }
          phrase2TokenList.add(token);
        } while (toker.hasMoreTokens());

        if (!toker.hasMoreTokens()) {
          throw new RuntimeException(String.format(
              "Additional fields expected (line %d)", reader.getLineNumber()));
        }
      }

      List<String> scoreList = new LinkedList<String>();
      do {
        String token = toker.nextToken();
        if (token.startsWith("|||")) {
          scoreList.clear();
          continue;
        }
        scoreList.add(token);
      } while (toker.hasMoreTokens());

      if (scoreList.size() != positionalMapping.length) {
        throw new RuntimeException(
            String
                .format(
                    "File type '%s' requires that %d scores be provided for each entry, however only %d were found (line %d)",
                    filetype, positionalMapping.length, scoreList.size(),
                    reader.getLineNumber()));
      }

      final int[] indexInts;
      if (conditionType == ConditionTypes.e
          || conditionType == ConditionTypes.f) {
        IString[] tokens = IStrings.toIStringArray(phrase1TokenList);
        indexInts = withGaps ? DTUTable.toWordIndexArray(tokens) : IStrings.toIntArray(tokens);
      } else {
        IString[] fTokens = IStrings.toIStringArray(phrase1TokenList);
        int[] fIndexInts = withGaps ? DTUTable.toWordIndexArray(fTokens) : IStrings.toIntArray(fTokens);
        IString[] eTokens = IStrings.toIStringArray(phrase2TokenList);
        int[] eIndexInts = withGaps ? DTUTable.toWordIndexArray(eTokens) : IStrings.toIntArray(eTokens);
        indexInts = mergeInts(fIndexInts, eIndexInts);
      }

      float[] scores = new float[scoreList.size()];
      int scoreId = 0;
      for (String score : scoreList) {
        try {
          scores[scoreId++] = (float) Math.log(Double.parseDouble(score));
        } catch (NumberFormatException e) {
          throw new RuntimeException(String.format(
              "Can't parse %s as a number (line %d)\n", score,
              reader.getLineNumber()));
        }
      }

      int idx = FlatPhraseTable.translationIndex.indexOf(indexInts, true);
      while (idx >= reorderingScores.size())
        reorderingScores.add(null);
      assert (reorderingScores.get(idx) == null);
      reorderingScores.set(idx, scores);
    }
    System.gc();
    long postTableLoadMemUsed = rt.totalMemory() - rt.freeMemory();
    long loadTimeMillis = System.currentTimeMillis() - startTimeMillis;
    System.err.printf(
        "Done loading reordering table: %s (mem used: %d MiB time: %.3f s)\n",
        filename, (postTableLoadMemUsed - preTableLoadMemUsed) / (1024 * 1024),
        loadTimeMillis / 1000.0);

    return selectedFiletype;
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err
          .printf("Usage:\n\tjava ExtendedLexicalReorderingTable (lexical reordering filename)\n");
      System.exit(-1);
    }

    ExtendedLexicalReorderingTable mlrt = new ExtendedLexicalReorderingTable(
        args[0]);

    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    System.out.print("\n>");
    for (String query = reader.readLine(); query != null; query = reader
        .readLine()) {
      String[] fields = query.split("\\s*\\|\\|\\|\\s*");
      int[] foreign = IStrings.toIntArray(IStrings.toIStringArray(fields[0]
          .split("\\s+")));
      int[] translation = IStrings.toIntArray(IStrings.toIStringArray(fields[1]
          .split("\\s+")));
      int[] merged = mergeInts(foreign, translation);
      int id = FlatPhraseTable.translationIndex.indexOf(merged);
      float[] scores = mlrt.getReorderingScores(id);
      for (int i = 0; i < scores.length; i++) {
        System.out.printf("%s: %e\n", mlrt.positionalMapping[i], scores[i]);
      }
      System.out.print("\n>");
    }
  }

}
