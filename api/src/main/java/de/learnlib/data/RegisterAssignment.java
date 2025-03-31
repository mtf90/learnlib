package de.learnlib.data;

import java.util.Map;
import net.automatalib.data.DataValue;
import net.automatalib.data.Mapping;
import net.automatalib.data.RegisterValuation;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Register;

/**
 * A register assignment models which data values
 * of a prefix are stored in which registers.
 */
public class RegisterAssignment extends Mapping<DataValue<?>, Register<?>> {

    public RegisterValuation registerValuation() {
        RegisterValuation vars = new RegisterValuation();
        for (Map.Entry<DataValue<?>, SymbolicDataValue.Register<?>> e : this.entrySet()) {
            vars.put(e.getValue(), e.getKey());
        }
        return vars;
    }
}
