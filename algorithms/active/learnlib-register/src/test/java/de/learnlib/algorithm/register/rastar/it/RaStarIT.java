package de.learnlib.algorithm.register.rastar.it;

import de.learnlib.algorithm.register.rastar.RaStar;
import de.learnlib.data.Configuration;
import de.learnlib.testsupport.it.AbstractRALearnerIT;
import de.learnlib.testsupport.it.variant.LearnerVariantList.RALearnerVariantList;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.symbol.data.ParameterizedSymbol;

public class RaStarIT extends AbstractRALearnerIT {

    @Override
    protected <I extends ParameterizedSymbol> void addLearnerVariants(Alphabet<I> alphabet,
                                                                      Configuration<I> config,
                                                                      RALearnerVariantList<I> variants) {
        variants.addLearnerVariant("RaStar", new RaStar<>(alphabet, config));
    }
}
