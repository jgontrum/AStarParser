/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.stud.astar;

import de.up.ling.stud.astar.pcfg.Pcfg;
import de.up.ling.stud.astar.pcfg.Rule;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.List;

/**
 *
 * @author Implementation of the SX Estimate, figure 10 in the paper
 */
public class SXEstimate extends Summarizer {
    private final Object2DoubleMap<SXTriple> outsideCache;
    private final Object2DoubleMap<SXTuple> insideCache;
    
    SXEstimate(Pcfg grammar) {
        super(grammar);
        this.outsideCache   = new Object2DoubleOpenHashMap<>();
        this.insideCache    = new Object2DoubleOpenHashMap<>();
    }
            
    private SXTriple summarize(Edge edge, List<String> sentence) {
        return new SXTriple(edge.getSymbol(), edge.getBegin(), sentence.size() - edge.getEnd());
    }
    
    @Override
    double evaluate(Edge edge, List<String> sentence) {
        return evaluate(summarize(edge, sentence));
    }

    private double evaluate(SXTriple span) {
//        System.err.println("Evaluating " + span);
        return outside(span);
    }
    
    
    ////////////////////////////////////////////////////////////////////////

    private double outside(SXTriple span) {
//        System.err.println("Calculating outside for " + span);
        if (outsideCache.containsKey(span)) {
//            System.err.println("Cached!");
            return outsideCache.get(span);
        } else {

            int state = span.getNonterminal();
            int lspan = span.getLeftWords();
            int rspan = span.getRightWords();

            if (lspan + rspan == 0) {
                return state == grammar.getStartSymbol()? 0 : Double.NEGATIVE_INFINITY;
            }

            double score = Double.NEGATIVE_INFINITY;
            
            // check left sibling
            if (!grammar.getRulesForSecondRhsSymbol(state).isEmpty()) {
                for (int sibsize = 1; sibsize < lspan; ++sibsize) {

                    for (Rule r : grammar.getRulesForSecondRhsSymbol(state)) {
                        double cost = inside(new SXTuple(r.getRhs()[0], sibsize))
                                + outside(new SXTriple(r.getLhs(), lspan - sibsize, rspan))
                                + Math.log(r.getProb());
                        score = Math.max(score, cost); // Update score, if it is better                        
                    }
                }
            }
            
            // check right sibling
            if (!grammar.getRulesForFirstRhsSymbol(state).isEmpty()) {
                for (int sibsize = 1; sibsize < rspan; ++sibsize) {
                    
                    for (Rule r : grammar.getRulesForFirstRhsSymbol(state)) {
                        double cost = inside(new SXTuple(r.getRhs()[1], sibsize))
                                + outside(new SXTriple(r.getLhs(), lspan, rspan - sibsize))
                                + Math.log(r.getProb());
                        score = Math.max(score, cost); // Update score, if it is better
                    }
                }
            }
            outsideCache.put(span, score);
            return score;
        }
    }

    private double inside(SXTuple tuple) {
        if (insideCache.containsKey(tuple)) {
            return insideCache.get(tuple);
        } else {
            int state = tuple.getNonterminal();
            int span = tuple.getWords();
            if (span == 0) {
                return grammar.isNonterminal(state) ? Double.NEGATIVE_INFINITY : 0;
            }
            double score = Double.NEGATIVE_INFINITY;

            // choose a split point
            for (int split = 1; split < span; split++) {
                for (Rule r : grammar.getRulesForLhs(state)) {
                    if (r.getRhs().length == 2) {
                        double cost = inside(new SXTuple(r.getRhs()[0], split))
                                + inside(new SXTuple(r.getRhs()[1], span - split))
                                + Math.log(r.getProb());
                        score = Math.max(score, cost); // Update score, if it is better
                    }
                }
            }
            insideCache.put(tuple, score);
            return score;
        }
    }
    
    private class SXTriple {
        private final int nonterminal;
        private final int leftWords;
        private final int rightWords;

        public SXTriple(int nonterminal, int leftWords, int rightWords) {
            this.nonterminal = nonterminal;
            this.leftWords = leftWords;
            this.rightWords = rightWords;
        }        

        public int getNonterminal() {
            return nonterminal;
        }

        public int getLeftWords() {
            return leftWords;
        }

        public int getRightWords() {
            return rightWords;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 17 * hash + this.nonterminal;
            hash = 17 * hash + this.leftWords;
            hash = 17 * hash + this.rightWords;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SXTriple other = (SXTriple) obj;
            if (this.nonterminal != other.nonterminal) {
                return false;
            }
            if (this.leftWords != other.leftWords) {
                return false;
            }
            if (this.rightWords != other.rightWords) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "SXTriple{" + "nonterminal=" + signature.getSymbolForId(nonterminal) + ", leftWords=" + leftWords + ", rightWords=" + rightWords + '}';
        }
        
        
    }
    
    private class SXTuple {
        private final int nonterminal;
        private final int words;

        public SXTuple(int nonterminal, int words) {
            this.nonterminal = nonterminal;
            this.words = words;
        }

        public int getNonterminal() {
            return nonterminal;
        }

        public int getWords() {
            return words;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 41 * hash + this.nonterminal;
            hash = 41 * hash + this.words;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SXTuple other = (SXTuple) obj;
            if (this.nonterminal != other.nonterminal) {
                return false;
            }
            if (this.words != other.words) {
                return false;
            }
            return true;
        }
        
        
    }
}
