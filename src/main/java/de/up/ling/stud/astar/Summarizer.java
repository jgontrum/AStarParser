/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.stud.astar;

import de.up.ling.stud.astar.pcfg.Pcfg;
import de.up.ling.stud.astar.pcfg.Signature;

/**
 *
 * @author johannes
 */
public abstract class Summarizer {
    protected final Signature signature;
    protected final Pcfg grammar;
    
    public Summarizer(Pcfg grammar) {
        this.grammar = grammar;
        this.signature = grammar.getSignature();
    }
    
    abstract double outside(int state, int lspan, int rspan);
    
    abstract double inside(int state, int span);

}
