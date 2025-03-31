package de.learnlib.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.SymbolicDataValueGenerator;

public class Memorables {

    public static Set<DataValue<?>> relabel(Set<DataValue<?>> values, SDTRelabeling relabeling) {
        Set<DataValue<?>> result = values.stream()
                .map(relabeling::get)
                .map( d -> (DataValue<?>) d)
                .collect(Collectors.toSet());

        assert result.size() == values.size();
        return result;
    }

    public static Map<DataType<?>, Integer> typedSize(Set<DataValue<?>> values) {
        Map<DataType<?>, Integer> ret = new LinkedHashMap<>();
        values.forEach(v -> {
            Integer i = ret.get(v.getDataType());
            i = (i == null) ? 1 : i+1;
            ret.put(v.getDataType(), i);
        });
        return ret;
    }

    public static Set<DataValue<?>> memorableValues(Collection<SDT> sdts) {
        return sdts.stream()
                .flatMap(sdt -> sdt.getDataValues().stream())
                .collect(Collectors.toSet());
    }

    public static RegisterAssignment getAssignment(SDT ... sdts) {
        return getAssignment(Arrays.asList(sdts));
    }

    public static RegisterAssignment getAssignment(Collection<SDT> sdts) {
        RegisterAssignment ra = new RegisterAssignment();
        SymbolicDataValueGenerator.RegisterGenerator regGen =
                new SymbolicDataValueGenerator.RegisterGenerator();

        memorableValues(sdts).forEach(
                dv -> ra.put(dv, regGen.next(dv.getDataType()))
        );

        return ra;
    }

}
