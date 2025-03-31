package de.learnlib.algorithm.register.radt.it;

import de.learnlib.algorithm.register.radt.RaDT;
import de.learnlib.data.Configuration;
import de.learnlib.testsupport.it.AbstractRALearnerIT;
import de.learnlib.testsupport.it.variant.LearnerVariantList.RALearnerVariantList;
import de.learnlib.theory.restriction.GenericSuffixRestrictionBuilderImpl;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.symbol.data.ParameterizedSymbol;

public class RaDTIT extends AbstractRALearnerIT {

    @Override
    protected <I extends ParameterizedSymbol> void addLearnerVariants(Alphabet<I> alphabet,
                                                                      Configuration<I> config,
                                                                      RALearnerVariantList<I> variants) {
        variants.addLearnerVariant("RaDT [default]", new RaDT<>(alphabet, config));
        variants.addLearnerVariant("RaDT [generic]",
                                   new RaDT<>(alphabet,
                                              config.getTreeOracle(),
                                              config::getHypothesisOracle,
                                              config.getLogicOracle(),
                                              new GenericSuffixRestrictionBuilderImpl(config.getConstants()),
                                              config.getConstants()));
    }
}
