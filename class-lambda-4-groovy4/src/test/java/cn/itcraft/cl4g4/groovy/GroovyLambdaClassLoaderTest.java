package cn.itcraft.cl4g4.groovy;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Helly Guo
 * <p>
 * Created on 11/24/21 7:28 PM
 */
public class GroovyLambdaClassLoaderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroovyLambdaClassLoaderTest.class);

    private static final AtomicReference<GroovyClassLoader> CLASS_LOADER_REF = new AtomicReference<>();

    private static void invoke2() {
        GroovyClassLoader groovyClassLoader = CLASS_LOADER_REF.get();
        Class<?> clazz = groovyClassLoader.parseClass("class demo" + System.nanoTime()
                                                              + " implements " + DemoInterface.class.getName() + "{\n"
                                                              + "def boolean demo(String name, int age){name.length() > 5 && age > 12}"
                                                              + "}\n");
        try {
            DemoInterface demoInterface = (DemoInterface) clazz.newInstance();
            boolean ret = demoInterface.demo("abcdefgh", 31);
            LOGGER.info("invoke {} with param[\"abcdefgh\",12], returns {}", demoInterface, ret);
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    @Test
    public void test() {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setSourceEncoding("UTF-8");
        GroovyClassLoader groovyClassLoader =
                new GroovyLambdaClassLoader(Thread.currentThread().getContextClassLoader(), config, false);
        CLASS_LOADER_REF.set(groovyClassLoader);
        invoke2();
    }

    public interface DemoInterface {
        boolean demo(String name, int age);
    }


}
