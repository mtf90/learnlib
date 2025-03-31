module de.learnlib.algorithm.register {

    requires de.learnlib.api;
    requires net.automatalib.api;
    requires net.automatalib.common.util;
    requires net.automatalib.core;

    requires jconstraints.core;
    requires org.slf4j;

    // annotations are 'provided'-scoped and do not need to be loaded at runtime
    requires static org.checkerframework.checker.qual;
    requires com.google.common;

    exports de.learnlib.algorithm.register;
    exports de.learnlib.algorithm.register.radt;
    exports de.learnlib.algorithm.register.rastar;
    exports de.learnlib.algorithm.register.ralambda;
}