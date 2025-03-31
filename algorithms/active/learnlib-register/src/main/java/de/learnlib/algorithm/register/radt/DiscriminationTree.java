package de.learnlib.algorithm.register.radt;

import de.learnlib.algorithm.register.LocationComponent;
import de.learnlib.data.SymbolicSuffix;
import java.util.Map;

import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

/**
 * This interface describes the methods needed in a discrimination tree during learning.
 *
 * @author fredrik
 */
public interface DiscriminationTree<I extends ParameterizedSymbol> {

    /**
     * Sift a prefix into the DT to find the corresponding leaf. If add is true, also adds the prefix to the set of non-short prefixes of the corresponding leaf.
     *
     * @param prefix
     * @param add
     * @return the leaf corresponding to prefix
     */
    DTLeaf sift(Word<SymbolInstance<I>> prefix, boolean add);

    /**
     * Split a prefix from a leaf node into a new leaf. Adds a new inner node using the suffix as a discriminator.
     *
     * @param prefix
     * @param suffix
     * @param leaf
     */
    void split(Word<SymbolInstance<I>> prefix, SymbolicSuffix<I> suffix, DTLeaf leaf);

    Map<Word<SymbolInstance<I>>, LocationComponent> getComponents();
}
