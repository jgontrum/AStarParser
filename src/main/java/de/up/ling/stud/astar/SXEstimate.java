/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.stud.astar;

import de.up.ling.stud.astar.pcfg.Pcfg;
import de.up.ling.stud.astar.pcfg.Rule;

/**
 *
 * @author Implementation of the SX Estimate, figure 10 in the paper
 */
public class SXEstimate extends Summarizer {

    public SXEstimate(Pcfg grammar) {
        super(grammar);
    }
    
    @Override
    public double outside(int state, int lspan, int rspan) {
        if (lspan + rspan == 0) {
            return state == grammar.getStartSymbol()? 0 : Double.NEGATIVE_INFINITY;
        }
        
        double score = Double.NEGATIVE_INFINITY;
        
        for (int sibsize = 0; sibsize < lspan; sibsize++) {
            // check left sibling
            for (Rule r : grammar.getRulesForSecondRhsSymbol(state)) {
                double cost = inside(r.getRhs()[0], sibsize)
                        + outside(r.getLhs(), lspan - sibsize, rspan)
                        + Math.log(r.getProb());
                score = Math.max(score, cost); // Update score, if it is better                        
            }
            
            // check right sibling
            for (Rule r: grammar.getRulesForFirstRhsSymbol(state)) {
                double cost = inside(r.getRhs()[1], sibsize)
                        + outside(r.getLhs(), lspan, rspan - sibsize) 
                        + Math.log(r.getProb());
                score = Math.max(score, cost); // Update score, if it is better
            }
        }
        
        for (int sibsize = 0; sibsize < lspan; sibsize++) {
            for (Rule r : grammar.getRulesForSecondRhsSymbol(state)) {
                double cost = inside(r.getRhs()[0], sibsize)
                        + outside(r.getLhs(), lspan - sibsize, rspan)
                        + Math.log(r.getProb());
                score = Math.max(score, cost); // Update score, if it is better                        
            }
        }
        return 0;
    }

    @Override
    public double inside(int state, int span) {
        if (span == 0) {
            return grammar.isNonterminal(state)? Double.NEGATIVE_INFINITY : 0;
        } 
        double score = Double.NEGATIVE_INFINITY;
        
        // choose a split point
        for (int split = 1; split < span; split++) {
            for (Rule r : grammar.getRulesForLhs(state)) {
                if (r.getRhs().length == 2) {
                    double cost = inside(r.getRhs()[0], split) 
                            + inside(r.getRhs()[1], span-split) 
                            + Math.log(r.getProb());
                    score = Math.max(score, cost); // Update score, if it is better
                }
            }
        }
        
        return score;
    }
    
}
