package mujava.test;

import java.io.IOException;

public interface InstrumentedClassLoader {

    public byte[] getInstrumentedClass(String name);
}
