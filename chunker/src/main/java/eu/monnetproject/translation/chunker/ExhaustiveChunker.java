/**
 * ********************************************************************************
 * Copyright (c) 2011, Monnet Project All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. * Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. * Neither the name of the Monnet Project nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************
 */
package eu.monnetproject.translation.chunker;

import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.ChunkList;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.LanguageModel;
import eu.monnetproject.translation.TranslationPhraseChunker;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author John McCrae
 */
public class ExhaustiveChunker implements TranslationPhraseChunker {

    private final LanguageModel lm;

    public ExhaustiveChunker(LanguageModel lm) {
        this.lm = lm;
    }

    @Override
    public List<String> preCase(List<String> label) {  
        final ListIterator<String> iter = label.listIterator();
        while(iter.hasNext()) {
            final String tokeni = iter.next();
            final String lowerCaseTk = tokeni.toLowerCase();
            if (!tokeni.equals(lowerCaseTk)) {
                final double origCaseScore = lm.score(Arrays.asList(tokeni));
                final double lowerCaseScore = lm.score(Arrays.asList(lowerCaseTk));
                if (!Double.isNaN(origCaseScore) && !Double.isNaN(lowerCaseScore)
                        && !Double.isInfinite(origCaseScore) && !Double.isInfinite(lowerCaseScore)
                        && lowerCaseScore - origCaseScore > 1.5) {
                    iter.set(lowerCaseTk);
                }
            }
        }
        return label;
    }

    
    
    @Override
    public ChunkList chunk(List<String> tokens2) {
        ChunkListImpl rval = new ChunkListImpl();
        final String[] tokens = tokens2.toArray(new String[tokens2.size()]);
        for (int i = 0; i < tokens.length; i++) {
            for (int j = i + 1; j <= tokens.length; j++) {
                final Chunk chunk = new ChunkImpl(build(tokens, i, j));
                rval.add(chunk);
            }
        }
        return rval;
    }

    private String build(String[] str, int begin, int end) {
        final StringBuilder builder = new StringBuilder();
        for (int i = begin; i < end; i++) {
            builder.append(str[i]).append(" ");
        }
        return builder.toString().trim();
    }
}
