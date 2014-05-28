/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.astar.pcfg;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author koller, modified by johannes to work with interal int values.
 */
public final class Rule {
    private final int lhs;
    private final int[] rhs;
    private final double prob;
    private final boolean unary;
    private final List<Rule> unaryChain;
    private final boolean posRule;

    public Rule(int lhs, int[] rhs, double prob) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.prob = prob;
        this.unary = false;
        this.unaryChain = null;
        this.posRule = false;
    }
    
    /**
     * Creates a bridge-rule to handle unary rules.
     * The lhs is the lhs of the unary rule, while the rhs is the rhs of the rule.
     * that is the next binary rule, that can be derivated from the unary rule.
     * This rule is only used internaly and will be dissolved while creating the parse tree.
     * Chain is a list of rules, that describe the derivation, where the first element is the 
     * first unary rule and the last element is the rule before the binary rule.
     * @param lhs
     * @param rhs
     * @param prob
     * @param chain 
     */
    public Rule(int lhs, int[] rhs, double prob, List<Rule> chain, boolean posRule) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.prob = prob;
        this.unary = true;
        this.unaryChain = chain;
        this.posRule = posRule;
    }
    
    
    /**
     * Creates a POS-Rule
     * @param lhs
     * @param rhs
     * @param prob
     * @param posRule 
     */
    public Rule(int lhs, int[] rhs, double prob, boolean posRule) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.prob = prob;
        this.unary = false;
        this.unaryChain = null;
        this.posRule = posRule;
    }
    
    // Return the lhs
    public int getLhs() {
        return lhs;
    }

    // Return the rhs
    public int [] getRhs() {
        return rhs;
    }
    
    // Return the prob
    public double getProb() {
        return prob;
    }
    
    public List<Rule> getChainList() {
        return unaryChain;
    }
    
    /**
     * Returns an IntSet for the rhs.
     * Use this function not in time critically sections! In that case, try getRhs()
     * @return 
     */
    public IntSet getRhsList() {
        return new IntArraySet(rhs);
    }
    
    public boolean isUnary() {
        return unary;
    }
            
   public boolean isPOS() {
       return posRule;
   }

    @Override
    // Prints this rule with ints
    public String toString() {
        String rhsString = "";
        for (int i = 0; i < rhs.length; ++i) {
            rhsString = rhsString.concat(rhs[i] + " ");
        }
        return getLhs() + " -> " + rhsString + " [" + prob + "]";
    }
    
    /**
     * Like toString(), but resolves the ints to Strings with a given Signature
     * @param signature
     * @return 
     */
    public String toString(Signature signature) {
        String rhsString = "";
        for (int i = 0; i < rhs.length; ++i) {
            rhsString = rhsString.concat(signature.getSymbolForId(rhs[i]) + " ");
        }
        return signature.getSymbolForId(getLhs()) + " -> " + rhsString + " [" + prob + "]";
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + this.lhs;
        hash = 59 * hash + Arrays.hashCode(this.rhs);
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.prob) ^ (Double.doubleToLongBits(this.prob) >>> 32));
        hash = 59 * hash + (this.unary ? 1 : 0);
        hash = 59 * hash + (this.unaryChain != null ? this.unaryChain.hashCode() : 0);
        hash = 59 * hash + (this.posRule ? 1 : 0);
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
        final Rule other = (Rule) obj;
        if (this.lhs != other.lhs) {
            return false;
        }
        if (!Arrays.equals(this.rhs, other.rhs)) {
            return false;
        }
        if (Double.doubleToLongBits(this.prob) != Double.doubleToLongBits(other.prob)) {
            return false;
        }
        if (this.unary != other.unary) {
            return false;
        }
        if (this.unaryChain != other.unaryChain && (this.unaryChain == null || !this.unaryChain.equals(other.unaryChain))) {
            return false;
        }
        if (this.posRule != other.posRule) {
            return false;
        }
        return true;
    }
  
    
    
}
