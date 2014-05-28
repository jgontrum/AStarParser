/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.astar.pcfg;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * A recursively defined Trie of <V>
 * @author koller, modified by johannes to use primitive int as keys.
 * @param <V>
 */
public class Trie<V> {
    private final Int2ObjectOpenHashMap<Trie<V>> map;
    private final Set<V> values;

    public Trie() {
        map = new Int2ObjectOpenHashMap<Trie<V>>();
        values = new HashSet<V>();
    }
    
    
    public void remove(int[] keyList, V value) {
        remove(keyList, value, 0);
    }
    
    private void remove(int[] keyList, V value, int index) {
        if (index == keyList.length) {
            values.remove(value);
        } else {
            int keyHere = keyList[index];
            Trie<V> nextTrie = map.get(keyHere);

            if (nextTrie != null) {
                nextTrie.remove(keyList, value, index + 1);
            }
        }
    }
    
    /**
     * Stores a sequence of ints (the Array) in the Trie 
     * and maps the final state to the given value.
     * @param keyList
     * @param value 
     */
    public void put(int[] keyList, V value) {
        put(keyList, value, 0);
    }
    
    /**
     * Recursive version of put.
     * Go as deep as the length of the given array.
     * @param keyList
     * @param value
     * @param index 
     */
    private void put(int[] keyList, V value, int index) {
        if( index == keyList.length) {
            values.add(value);
        } else {
            int keyHere = keyList[index];
            Trie<V> nextTrie = map.get(keyHere);
            
            if( nextTrie == null ) {
                nextTrie = new Trie<V>();
                map.put(keyHere, nextTrie);
            }
            
            nextTrie.put(keyList, value, index+1);
        }
    }
    
    /**
     * Returns a set of values, that is mapped to the final state
     * we reach with the sequence of transitions in keyList.
     * @param keyList
     * @return 
     */
    public Set<V> get(int[] keyList) {
        return get(keyList, 0);
    }
    
    private Set<V> get(int[] keyList, int index) {
        if( index == keyList.length) {
            return values;
        } else {
            int keyHere = keyList[index];
            Trie<V> nextTrie = map.get(keyHere);
            
            if( nextTrie == null ) {
                return new HashSet<V>();
            } else {
                return nextTrie.get(keyList, index+1);
            }
        }
    }
    
    /**
     * Returns the subtrie that we reach with a transition with the given symbol.
     * @param id
     * @return 
     */
    public Trie<V> getSubtrie(int id) {
        return map.get(id);
    }
    
    /**
     * Returns an IntSet with the symbols for all outgoing transitions.
     * @return 
     */
    public IntSet getBranches() {
        return map.keySet();
    }
    
    /**
     * Returns the stored values that we get from the state we reach 
     * with the given symbol.
     * @param id
     * @return 
     */
    public Collection<V> getValuesForId(int id){
        if (map.containsKey(id)) {
            return map.get(id).values;
        } else {
            return new HashSet<V>();
        }
    }
    
//    /**
//     * Get all values that are stored in this trie including its subtries.
//     * @return 
//     */
//    public Collection<V> values() {
//        
//    	Collection<V> ret = new ArrayList<V>();
//    	collectValues(ret);    	
//    	return ret;
//    }
//    
//    private void collectValues(Collection<V> ret) {
//    	ret.addAll(values);
//    	for( Trie<V> child : map.values() ) {
//            child.collectValues(ret);
//    	}
//    }
}
