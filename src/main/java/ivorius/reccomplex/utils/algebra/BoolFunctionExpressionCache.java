/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.utils.algebra;

import ivorius.reccomplex.RCConfig;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by lukas on 05.10.16.
 */
public class BoolFunctionExpressionCache<A, U> extends FunctionExpressionCache<Boolean, A, U> implements Predicate<A>
{
    public static final String GLOBAL_PREFIX = "global:";

    public BoolFunctionExpressionCache(Algebra<Boolean> algebra)
    {
        super(algebra);

        addBoolConstants();
    }

    public BoolFunctionExpressionCache(Algebra<Boolean> algebra, Boolean emptyResult, String emptyResultRepresentation)
    {
        super(algebra, emptyResult, emptyResultRepresentation);

        addBoolConstants();
    }

    protected void addBoolConstants()
    {
        addType(FunctionExpressionCaches.constant("true", true));
        addType(FunctionExpressionCaches.constant("false", false));
        addType(new VariableTypeGlobal(GLOBAL_PREFIX, ""));
    }

    @Override
    public boolean test(A a)
    {
        return evaluate(a);
    }

    public static class VariableTypeGlobal extends VariableType<Boolean, Object, Object>
    {
        public VariableTypeGlobal(String prefix, String suffix)
        {
            super(prefix, suffix);
        }

        @Override
        public Function<Object, Boolean> parse(String var)
        {
            boolean result = RCConfig.globalToggles.containsKey(var) && RCConfig.globalToggles.get(var);
            return o -> result;
        }

        @Override
        public Validity validity(String var, Object o)
        {
            return RCConfig.globalToggles.containsKey(var) ? Validity.KNOWN : Validity.UNKNOWN;
        }
    }
}
