/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.stud.astar;

import de.up.ling.stud.astar.pcfg.Pcfg;
import de.up.ling.stud.astar.pcfg.Rule;
import de.up.ling.stud.astar.pcfg.Signature;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 *
 * @author johannes
 */
public class InsideOutsideTools {
    private final Pcfg grammar; 
    private final IntList words;
    private final Signature sig;
    private final int n;
    
    /**
     * See Manning and Schütze: Foundations of Statistical Language Processing
     * pp 392.
     * @param grammar
     * @param words 
     */
    public InsideOutsideTools(Pcfg grammar, IntList words) {
        this.grammar = grammar;
        this.words = words;
        this.n = words.size() - 1;
        this.sig = grammar.getSignature();
    }
    
    public double b(int symbol, int start, int end) { 
        return 0.0;
    }
    
    public double inside(int symbol, int start, int end) {
       
        if (start == end) { // base case
            
            for (Rule rule : grammar.getRulesForLhs(symbol)) {
                
                if (rule.getRhs().length == 1 && rule.getRhs()[0] == words.get(start)) {
                 
                    return rule.getProb();
                }
            }
            
        } else { // induction
            
//            System.err.println("Calculationg for: <" + symbol + ", " + start + ", " + end + ">");
            
            double ret = 0.0;

            for (Rule rule : grammar.getRulesForLhs(symbol)) { // Iteration over all N^r and N^s 
                
                if (rule.getRhs().length == 2) {
                    
                    for (int d = start; d < end; d++) { // Guessing where to split

                        int r = rule.getRhs()[0];
                        int s = rule.getRhs()[1];

                        double inside_r = inside(r, start, d);
                        double inside_s = inside(s, d + 1, end);
                        
                        
                        ret += rule.getProb() * inside_r * inside_s;

                    }
                }
            }
            
            return ret;

        }
        
        return 0.0;
    }
    
    // a_j(p,q) symbol = j, start = p, end = q
    public double outside(int symbol, int start, int end) {
        // base case
        if (start == 0 && end == n) {
            System.err.println("Here.");
            return (symbol == grammar.getStartSymbol()) ? 1 : 0;
        } else {
            // induction
            
            double part1 = 0.0;
            System.err.println("Calculationg for: <" + sig.getSymbolForId(symbol) + ", " + start + ", " + end + ">");

            System.err.println(grammar.getRulesForFirstRhsSymbol(symbol));
            
            for (Rule rule : grammar.getRulesForFirstRhsSymbol(symbol)) {
                System.err.println(rule.toString(sig));
                if (rule.getRhs().length == 2) {
                    for (int e = end + 1; e < n; e++) { // n?  OBO?
                        double outside = outside(rule.getLhs(), start, e);
                        double prob = rule.getProb();
                        double inside = inside(rule.getRhs()[1], end + 1, e);
                        System.err.println("Old Part1 " + part1);
                        part1 += outside * inside * prob;
                        System.err.println("New Part1 " + part1);

                    }
                }
            }
            
            double part2 = 0.0;
            
            for (Rule rule : grammar.getRulesForSecondRhsSymbol(symbol)) {
                System.err.println(rule.toString(sig));
                if (rule.getRhs().length == 2) {
                    for (int e = 0; e < start; e++) {
                        System.err.println("e:" + e);
                        double outside = outside(rule.getLhs(), e, end);
                        double prob = rule.getProb();
                        double inside = inside(rule.getRhs()[0], e, start - 1);
                        System.err.println("Old Part2 " + part2);
                        part2 += outside * inside * prob;
                        System.err.println("New Part2 " + part2);
                    }
                }
            }
            
            return  part1  + part2;
        }
    }
    
    
    
    
    
}
