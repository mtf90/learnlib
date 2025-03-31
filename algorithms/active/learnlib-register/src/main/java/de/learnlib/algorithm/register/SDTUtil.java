package de.learnlib.algorithm.register;

import de.learnlib.data.Bijection;
import de.learnlib.data.SDT;
import de.learnlib.data.SDTRelabeling;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import net.automatalib.data.DataValue;

public class SDTUtil {


    /**
     * Returns a bijection b such that b and bi agree on the registers in the intersection
     * of their domains and such that sdt1 and sdt2 are equivalent under b.
     * Returns null if no such bijection can be found.
     *
     * @param sdt1
     * @param sdt2
     * @param bi
     * @return
     */
    public static Bijection<DataValue<?>> equivalentUnderBijection(SDT sdt1, SDT sdt2, Bijection<DataValue<?>> bi) {
        sdt1 = sdt1.relabel(SDTRelabeling.fromBijection(bi));
        Set<DataValue<?>> regs1 = sdt1.getDataValues();
        Set<DataValue<?>> regs2 = sdt2.getDataValues();

        if (regs1.size() != regs2.size()) {
            return null;
        }

        if (new HashSet<>(regs1).containsAll(regs2)) {
            return sdt1.isEquivalentUnderCondition(sdt2, ExpressionUtil.TRUE) ? bi : null;
        }

        Set<DataValue<?>> replace = new LinkedHashSet<>(regs1);
        replace.removeAll(bi.values());
        Set<DataValue<?>> by = new LinkedHashSet<>(regs2);
        by.removeAll(bi.values());

        RemappingIterator<DataValue<?>> it = new RemappingIterator<>(replace, by);
        while (it.hasNext()) {
            Bijection<DataValue<?>> vars = it.next();
            if (sdt2.isEquivalentUnderCondition(sdt1.relabel(SDTRelabeling.fromBijection(vars)), ExpressionUtil.TRUE)) {
                Bijection<DataValue<?>> b = new Bijection<>();
                b.putAll(bi);
                b.putAll(vars);
                return b;
            }
        }

        return null;
    }

    /**
     * Returns a bijection b such that sdt1 and sdt2 are semantically equivalent under b
     *
     * @param sdt1
     * @param sdt2
     * @return
     */
    public static Bijection<DataValue<?>> equivalentUnderBijection(SDT sdt1, SDT sdt2) {
        return equivalentUnderBijection(sdt1, sdt2, new Bijection<>());
    }
}
