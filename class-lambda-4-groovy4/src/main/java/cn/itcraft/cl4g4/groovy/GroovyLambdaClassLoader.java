package cn.itcraft.cl4g4.groovy;

import cn.itcraft.cl4g4.common.ForbiddenClassInitializer;
import cn.itcraft.cl4g4.common.UnsafeUtil;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyRuntimeException;
import groovyjarjarasm.asm.ClassVisitor;
import groovyjarjarasm.asm.ClassWriter;
import groovyjarjarasm.asm.Opcodes;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.control.BytecodeProcessor;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.EncodingGroovyMethods;
import sun.misc.Unsafe;

import java.security.CodeSource;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * 使用 {@link Unsafe} 来加载 class
 */
public class GroovyLambdaClassLoader extends GroovyClassLoader {

    private static final Unsafe UNSAFE = UnsafeUtil.getUnSafe();

    private final CompilerConfiguration config;

    /**
     * creates a GroovyLambdaClassLoader.
     *
     * @param parent                    the parent class loader
     * @param config                    the compiler configuration
     * @param useConfigurationClasspath determines if the configurations classpath should be added
     */
    public GroovyLambdaClassLoader(ClassLoader parent, CompilerConfiguration config,
                                   boolean useConfigurationClasspath) {
        super(parent, config, useConfigurationClasspath);
        if (config == null) {
            this.config = CompilerConfiguration.DEFAULT;
        } else {
            this.config = config;
        }
    }

    /**
     * Parses the given text into a Java class capable of being run
     *
     * @param text     the text of the script/class to parse
     * @param fileName the file name to use as the name of the class
     * @return the main class defined in the given script
     */
    @Override
    public Class<?> parseClass(final String text, final String fileName) throws CompilationFailedException {
        GroovyCodeSource gcs =
                ForbiddenClassInitializer.newAction(() -> new GroovyCodeSource(text, fileName, "/groovy/script"));
        Objects.requireNonNull(gcs).setCachable(false);
        // it's better to cache class instances by the source code
        // GCL will load the unique class instance for the same source code
        // and avoid occupying Permanent Area/Metaspace repeatedly
        String cacheKey = genSourceCacheKey(gcs);

        return sourceCache.getAndPut(
                cacheKey,
                key -> doParseClass(gcs),
                false
                                    );
    }

    private String genSourceCacheKey(GroovyCodeSource codeSource) {
        StringBuilder strToDigest;

        String scriptText = codeSource.getScriptText();
        if (null == scriptText) {
            strToDigest = new StringBuilder(32);
            // if the script text is null, i.e. the script content is invalid
            // use the name as cache key for the time being to trigger the validation by `groovy.lang.GroovyLambdaClassLoader.validate`
            // note: the script will not be cached due to the invalid script content,
            //       so it does not matter even if cache key is not the md5 value of script content
            strToDigest.append("name:").append(codeSource.getName());
        } else {
            strToDigest = new StringBuilder((int) (scriptText.length() * 1.2));
            strToDigest.append("scriptText:").append(scriptText);

            CodeSource cs = codeSource.getCodeSource();
            if (null != cs) {
                strToDigest.append("/codeSource:").append(cs);
            }
        }

        try {
            return EncodingGroovyMethods.md5(strToDigest);
        } catch (NoSuchAlgorithmException e) {
            // should never reach here!
            throw new GroovyRuntimeException(e);
        }
    }

    private Class<?> doParseClass(GroovyCodeSource codeSource) {
        Class<?> answer;  // Was neither already loaded nor compiling, so compile and add to cache.
        CompilationUnit unit = createCompilationUnit(config, codeSource.getCodeSource());
        if (config.getRecompileGroovySource()) {
            unit.addFirstPhaseOperation(TimestampAdder.INSTANCE, CompilePhase.CLASS_GENERATION.getPhaseNumber());
        }
        SourceUnit su = unit.addSource(codeSource.getName(), codeSource.getScriptText());

        LambdaClassCollector collector = createLambdaCollector(unit, su);
        unit.setClassgenCallback(collector);
        int goalPhase = Phases.CLASS_GENERATION;
        if (config.getTargetDirectory() != null) {
            goalPhase = Phases.OUTPUT;
        }
        unit.compile(goalPhase);

        answer = collector.generatedClass;
        String mainClass = su.getAST().getMainClassName();
        for (Class<?> clazz : collector.getLoadedClasses()) {
            String clazzName = clazz.getName();
            definePackageInternal(clazzName);
            setClassCacheEntry(clazz);
            if (clazzName.equals(mainClass)) {
                answer = clazz;
                break;
            }
        }
        return answer;
    }

    private void definePackageInternal(String className) {
        int idx = className.lastIndexOf('.');
        if (idx != -1) {
            String pkgName = className.substring(0, idx);
            Package pkg = getPackage(pkgName);
            if (pkg == null) {
                definePackage(pkgName, null, null, null, null, null, null, null);
            }
        }
    }

    /**
     * creates a new CompilationUnit. If you want to add additional
     * phase operations to the CompilationUnit (for example to inject
     * additional methods, variables, fields), then you should overwrite
     * this method.
     *
     * @param config the compiler configuration, usually the same as for this class loader
     * @param source the source containing the initial file to compile, more files may follow during compilation
     * @return the CompilationUnit
     */
    @Override
    protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource source) {
        return new CompilationUnit(config, source, this);
    }

    /**
     * creates a LambdaClassCollector for a new compilation.
     *
     * @param unit the compilationUnit
     * @param su   the SourceUnit
     * @return the LambdaClassCollector
     */
    private LambdaClassCollector createLambdaCollector(CompilationUnit unit, SourceUnit su) {
        return new LambdaClassCollector(unit, su);
    }

    private static class LambdaClassCollector implements CompilationUnit.ClassgenCallback {
        private final SourceUnit su;
        private final CompilationUnit unit;
        private final Collection<Class<?>> loadedClasses;
        private Class<?> generatedClass;

        protected LambdaClassCollector(CompilationUnit unit, SourceUnit su) {
            this.unit = unit;
            this.loadedClasses = new ArrayList<>();
            this.su = su;
        }

        protected void createClass(byte[] code, ClassNode classNode) {
            BytecodeProcessor bytecodePostprocessor = unit.getConfiguration().getBytecodePostprocessor();
            byte[] fcode = code;
            if (bytecodePostprocessor != null) {
                fcode = bytecodePostprocessor.processBytecode(classNode.getName(), fcode);
            }
            Class<?> theClass = UNSAFE.defineAnonymousClass(AnonymousGroovyClassHolder.class, fcode, null);
            this.loadedClasses.add(theClass);

            if (generatedClass == null) {
                fillGeneratedClass(classNode, theClass);
            }
        }

        private void fillGeneratedClass(ClassNode classNode, Class<?> theClass) {
            ModuleNode mn = classNode.getModule();
            SourceUnit msu = null;
            boolean mnPresent = mn != null;
            if (mnPresent) {
                msu = mn.getContext();
            }
            ClassNode main = null;
            if (mnPresent) {
                main = mn.getClasses().get(0);
            }
            if (msu == su && main == classNode) {
                generatedClass = theClass;
            }
        }

        protected void onClassNode(ClassWriter classWriter, ClassNode classNode) {
            byte[] code = classWriter.toByteArray();
            createClass(code, classNode);
        }

        @Override
        public void call(ClassVisitor classWriter, ClassNode classNode) {
            onClassNode((ClassWriter) classWriter, classNode);
        }

        public Collection<Class<?>> getLoadedClasses() {
            return this.loadedClasses;
        }
    }

    private static class TimestampAdder implements CompilationUnit.IPrimaryClassNodeOperation, Opcodes {
        private static final TimestampAdder INSTANCE = new TimestampAdder();

        private TimestampAdder() {
        }

        protected void addTimeStamp(ClassNode node) {
            if (node.getDeclaredField(Verifier.__TIMESTAMP) == null) { // in case if verifier visited the call already
                FieldNode timeTagField = new FieldNode(
                        Verifier.__TIMESTAMP,
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                        ClassHelper.long_TYPE,
                        //"",
                        node,
                        new ConstantExpression(System.currentTimeMillis()));
                // alternatively, FieldNode timeTagField = SourceUnit.createFieldNode("public static final long __timeStamp = " + System.currentTimeMillis() + "L");
                timeTagField.setSynthetic(true);
                node.addField(timeTagField);

                timeTagField = new FieldNode(
                        Verifier.__TIMESTAMP__ + System.currentTimeMillis(),
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                        ClassHelper.long_TYPE,
                        //"",
                        node,
                        new ConstantExpression((long) 0));
                // alternatively, FieldNode timeTagField = SourceUnit.createFieldNode("public static final long __timeStamp = " + System.currentTimeMillis() + "L");
                timeTagField.setSynthetic(true);
                node.addField(timeTagField);
            }
        }

        @Override
        public void call(final SourceUnit source, final GeneratorContext context, final ClassNode classNode) throws
                                                                                                             CompilationFailedException {
            if ((classNode.getModifiers() & Opcodes.ACC_INTERFACE) != 0) {
                // does not apply on interfaces
                return;
            }
            if (!(classNode instanceof InnerClassNode)) {
                addTimeStamp(classNode);
            }
        }
    }
}
