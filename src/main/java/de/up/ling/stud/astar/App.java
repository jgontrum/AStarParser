package de.up.ling.stud.astar;

import de.up.ling.stud.astar.pcfg.ParseException;
import de.up.ling.stud.astar.pcfg.Pcfg;
import de.up.ling.stud.astar.pcfg.PcfgParser;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws ParseException, FileNotFoundException
    {
        String grammar = "S\n"
                + "Det -> den [1]\n"
                + "N -> Baum [1]\n"
                + "NP -> Det N [0.5]\n"
                + "NP -> Hans [0.5]\n"
                + "S -> NP VP [1]\n"
                + "V -> mag [1]\n"
                + "VP -> V NP [1]";
        
//        grammar = "S\n" +
//                "S -> A B [1]\n" +
//                "A -> C D [1]\n" +
//                "C -> a [1]\n" +
//                "D -> b [1]\n" +
//                "B -> c [1]";

        Pcfg pcfg;
        pcfg = PcfgParser.parse(new StringReader(grammar));
//        pcfg = PcfgParser.parse(new FileReader(new File("grammar.txt")));
        
        String sentence;
        sentence = "Hans mag den Baum";
//        sentence = "a b c";
//        sentence = "The market crumbled . DOL";
//        sentence = "No , it , was n't Black Monday . DOL";
        
        StringTokenizer tokenizer = new StringTokenizer(sentence);
        List<String> inputWord = new ArrayList();
        IntList wordsDecoded = new IntArrayList();
        while (tokenizer.hasMoreTokens()) {
            String currentWord = tokenizer.nextToken();
            inputWord.add(currentWord);
            wordsDecoded.add(pcfg.getSignature().addSymbol(currentWord));
        }
        System.err.println(inputWord);

        InsideOutsideTools iot = new InsideOutsideTools(pcfg, wordsDecoded);
        
        System.err.println(iot.inside(pcfg.getStartSymbol(), 0, wordsDecoded.size() -1 ));
        wordsDecoded.rem(0);
                wordsDecoded.rem(0);

        System.err.println(iot.outside(pcfg.getSignature().getIdforSymbol("NP"), 2, wordsDecoded.size() - 1));

//        astarParser parser = new astarParser();    
//        System.err.println(parser.parse(inputWord, pcfg));
    }
}
