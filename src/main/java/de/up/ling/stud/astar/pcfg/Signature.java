/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.astar.pcfg;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 *
 * @author johannes
 */
public class Signature <E> {
    private Object2IntOpenHashMap<E> objectToInt = new Object2IntOpenHashMap();
    private Int2ObjectOpenHashMap<E> intToObject = new Int2ObjectOpenHashMap();
    private int nextFreeCell = 1;
    
    /**
     * Adds a symbol of the type <E> to the signature. Returns the internal value.
     * @param symbol
     * @return 
     */
    public int addSymbol(E symbol) {
        int ret = objectToInt.getInt(symbol);

        if (ret == 0) {
            ret = nextFreeCell++;
            objectToInt.put(symbol, ret);
            intToObject.put(ret, symbol);
        }

        return ret;
    }
    
    /**
     * Receives the internal value for a given symbol. Returns 0, if the symbol is not in the signature yet.
     * Call addSymbol() in this case.
     * @param symbol
     * @return 
     */
    public int getIdforSymbol(E symbol) { 
        return objectToInt.getInt(symbol);
    }
    
    /**
     * Resolves a internal ID and returns the saved value of Type E.
     * @param id
     * @return 
     */
    public E getSymbolForId(int id) {
        return intToObject.get(id);
        //TODO What happens, if id is not in intToObject?
    }
    
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (int i = 1; i < nextFreeCell; ++i) {
            ret.append(i).append(" - ").append(getSymbolForId(i)).append("\n");
        }
        return ret.toString();
    }
}
