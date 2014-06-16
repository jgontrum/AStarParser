/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.stud.astar;

import de.up.ling.stud.astar.pcfg.Pcfg;
import de.up.ling.stud.astar.pcfg.Rule;
import de.up.ling.stud.astar.pcfg.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import edu.stanford.nlp.util.BinaryHeapPriorityQueue;
import java.awt.GraphicsConfigTemplate;
import java.util.Arrays;

/**
 *
 * @author gontrum
 */
public class astarParser {
    private BinaryHeapPriorityQueue<Edge> agenda;
    private Signature<String> signature;
    
    private Int2ObjectMap<Set<Edge>> seenItemsByStartPosition;
    private Int2ObjectMap<Set<Edge>> seenItemsByEndPosition;
    
    private Set<Edge> competedEdges; //< Do not process the same edge more than one time
    
    private Set<Edge> workingSet;
    private Object2DoubleMap<Edge> insideMap; //< Log inside score for spans
    
    private int n;
    private Pcfg pcfg;
    
    private Summarizer summarizer;
    
    private void enqueue(Edge item, double weight) {
        agenda.add(item, weight);
        workingSet.add(item);
        competedEdges.add(item);
    }
    
    private Edge poll() {
        return agenda.removeFirst();
    }
    
    // Save items from a temporary set to the maps
    private boolean flush() { 
        for (Edge item : workingSet) {
            // Save by end
            if (seenItemsByEndPosition.containsKey(item.getEnd())) {
                seenItemsByEndPosition.get(item.getEnd()).add(item);
            } else {
                Set<Edge> insert = new HashSet<>();
                insert.add(item);
                seenItemsByEndPosition.put(item.getEnd(), insert);
            }
            // Save by start
            if (seenItemsByStartPosition.containsKey(item.getBegin())) {
                seenItemsByStartPosition.get(item.getBegin()).add(item);
            } else {
                Set<Edge> insert = new HashSet<>();
                insert.add(item);
                seenItemsByStartPosition.put(item.getBegin(), insert);
            }
            
            if (isStartItem(item)) return true;
        }
        return false;
    }
    
    private void updateInside(Edge span, double value) {
        if (insideMap.containsKey(span)) {
            if (value > insideMap.get(span)) {
                insideMap.put(span, value);
            }
        } else {
            insideMap.put(span, value);
        }

    }
    
    private double getInside(Edge span) {
        return insideMap.containsKey(span)? insideMap.get(span) : Double.NEGATIVE_INFINITY;
    }
    
    public Tree parse(List<String> words, Pcfg pcfg) {
        /*
            Set up variables
         */
        agenda = new BinaryHeapPriorityQueue<>();
        
        seenItemsByEndPosition = new Int2ObjectOpenHashMap<>();
        seenItemsByEndPosition.defaultReturnValue(new HashSet<>());
        seenItemsByStartPosition = new Int2ObjectOpenHashMap<>();
        seenItemsByStartPosition.defaultReturnValue(new HashSet<>());
        
        competedEdges = new HashSet<>();
        workingSet = new HashSet<>();
        
        insideMap = new Object2DoubleOpenHashMap<>();
        insideMap.defaultReturnValue(Double.NEGATIVE_INFINITY);
        
        n = words.size();
        
        this.pcfg = pcfg;
        signature = pcfg.getSignature();
        
        summarizer = new SXEstimate(pcfg);
        
        /*
            Create startitems
        */
        
        for (int i = 0; i < n; i++) {
            int[] rhs = new int[1];
            rhs[0] = signature.getIdforSymbol(words.get(i));
            
//            System.err.println(words.get(i));
            
            for (Rule r : pcfg.getRules(rhs)) {
//                System.err.println("-> " +r.toString(signature));
                Edge edge = new Edge(r.getLhs(), i, i+1, null, null);
                
                double inside = Math.log(r.getProb());
                double outsideEstimate = summarizer.evaluate(edge, words);
                
                updateInside(edge, inside);
                
                enqueue(edge, inside + outsideEstimate);
            }
    
        }
        
        /*
            Main loop
         */
        int edgeCounter = 0;
        while (!agenda.isEmpty()) { 
            ++edgeCounter;
            if (flush()) break;
            Edge item = poll();
//            System.err.println("Current Item: " + item.toStringReadable(signature));
            
            seenItemsByEndPosition.get(item.getBegin()).stream().forEach((candidate) -> {
                int[] rhs = new int[2];
                rhs[0] = candidate.getSymbol();
                rhs[1] = item.getSymbol();
                
                pcfg.getRules(rhs).stream().forEach((r) -> {
                    Edge edge = new Edge(r.getLhs(), candidate.getBegin(), item.getEnd(), candidate, item);

                    if (!competedEdges.contains(edge)) {
                        double insideLeft = getInside(candidate);
                        double insideRight = getInside(item);
                        double newInside = insideLeft + insideRight + Math.log(r.getProb());

                        double outsideEstimate = summarizer.evaluate(edge, words); 
                        
                        updateInside(edge, newInside);
                        enqueue(edge, newInside + outsideEstimate);
                    }

                });
            });
            
            seenItemsByStartPosition.get(item.getEnd()).stream().forEach((candidate) -> {
                int[] rhs = new int[2];
                rhs[0] = item.getSymbol();
                rhs[1] = candidate.getSymbol();

                pcfg.getRules(rhs).stream().forEach((r) -> {
                    Edge edge = new Edge(r.getLhs(), item.getBegin(), candidate.getEnd(), item, candidate);
                    
                    if (!competedEdges.contains(edge)) {
                        double insideLeft = getInside(item);
                        double insideRight = getInside(candidate);
                        double newInside = insideLeft + insideRight + Math.log(r.getProb());
                        
                        double outsideEstimate = summarizer.evaluate(edge, words);

                        updateInside(edge, newInside);
                        enqueue(edge, newInside + outsideEstimate);                        
                    }

                });
            });
            
        }
        
        System.err.println("Edges: " + edgeCounter);
        
        
        // Find final item and create a parse tree
        for (Edge finalItem : seenItemsByEndPosition.get(n)) {
            if (isStartItem(finalItem)) {
                return createParseTree(finalItem);
            }
        }
        
        return null;
    }
    
    private boolean isStartItem(Edge item) {
        return item.getEnd() == n && item.getBegin() == 0 && item.getSymbol() == pcfg.getStartSymbol();
    }

    
    private Tree createParseTree(Edge item) {
        List<Tree<String>> children = new ArrayList<>();
        
        Edge child1 = item.getFirstChild();
        Edge child2 = item.getSecondChild();
        
        if (child1 != null && child2 != null) {
            children.add(createParseTree(child1));
            children.add(createParseTree(child2));
        }
        return Tree.create(signature.getSymbolForId(item.getSymbol()), children);
    }
    
}
