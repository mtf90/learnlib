package de.learnlib.algorithm.register.rmmstar.it;

import de.learnlib.data.Configuration;
import de.learnlib.testsupport.it.AbstractRMMLearnerIT;
import de.learnlib.testsupport.it.variant.LearnerVariantList.RMMLearnerVariantList;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.symbol.data.ParameterizedSymbol;

public class RmmStarIT extends AbstractRMMLearnerIT {

    @Override
    protected <I extends ParameterizedSymbol, O extends ParameterizedSymbol> void addLearnerVariants(Alphabet<I> alphabet,
                                                                                                     Configuration<I> config,
                                                                                                     RMMLearnerVariantList<I, O> variants) {
//        variants.addLearnerVariant("RmmStar", new RaStar<>(alphabet, config));
    }
}
