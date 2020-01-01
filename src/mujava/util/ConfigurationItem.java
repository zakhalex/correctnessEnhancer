package mujava.util;

import mujava.MutationControl;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationItem {
    private Map<String,String> parameters=new HashMap<>();

    public ConfigurationItem(Map<String,String> params)
    {
        this.parameters.putAll(params);
    }

    public String getFileName() {
        return parameters.get(MutationControl.Inputs.FILES.getLabel());
    }

    //File to be mutated
    public String getClassName() {
        return parameters.get(MutationControl.Inputs.MUTANTS.getLabel());
    }

    public String getMethodName() {
        return parameters.get(MutationControl.Inputs.METHODS.getLabel());
    }

    public String getTestName() {
        return parameters.get(MutationControl.Inputs.TESTS.getLabel());
    }
}
