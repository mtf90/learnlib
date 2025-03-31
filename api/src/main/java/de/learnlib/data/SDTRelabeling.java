package de.learnlib.data;

import net.automatalib.data.DataValue;
import net.automatalib.data.Mapping;
import net.automatalib.data.GuardElement;
import net.automatalib.data.TypedValue;
import org.checkerframework.checker.units.qual.K;

public class SDTRelabeling extends Mapping<GuardElement, GuardElement> {

    public static SDTRelabeling fromBijection(Bijection<DataValue<?>> in) {
        SDTRelabeling ret = new SDTRelabeling();
        ret.putAll(in);
        return ret;
    }

    public static SDTRelabeling fromMapping(Mapping<GuardElement, GuardElement> mapping) {
    	SDTRelabeling ret = new SDTRelabeling();
    	ret.putAll(mapping);
    	return ret;
    }

//    @Override
//    public GuardElement put(GuardElement key, GuardElement value) {
//        if (!key.getClass().equals(value.getClass())) {
//            throw new IllegalArgumentException("Types of key and value do not match");
//        }
//        return super.put(key, value);
//    }

    public <T extends GuardElement> T getIfAvailable(T oldValue) {
        T newValue = (T) get(oldValue);
        return newValue != null ? newValue : oldValue;
    }
}
