package ch.css.jobrunr.control.deployment.scanner;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDetailPage;
import ch.css.jobrunr.control.infrastructure.details.RecapValueExtractor;
import ch.css.jobrunr.control.infrastructure.details.RecapValueExtractorSupport;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.jandex.*;
import org.jboss.logging.Logger;

import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Generates runtime RecapValueExtractor implementations based on build-time Jandex metadata.
 */
public class RecapValueExtractorGenerator {

    private static final Logger LOG = Logger.getLogger(RecapValueExtractorGenerator.class);
    private static final DotName JOB_RECAP_PARAMETER = DotName.createSimple(ch.css.jobrunr.control.annotations.JobRecapParameter.class.getName());

    private final IndexView index;
    private final BuildProducer<GeneratedBeanBuildItem> generatedBeanProducer;

    public RecapValueExtractorGenerator(IndexView index,
                                        BuildProducer<GeneratedBeanBuildItem> generatedBeanProducer) {
        this.index = index;
        this.generatedBeanProducer = generatedBeanProducer;
    }

    public void generate(Set<JobDefinition> jobDefinitions) {
        List<String> recapClassNames = jobDefinitions.stream()
                .map(JobDefinition::jobDetailPage)
                .filter(Objects::nonNull)
                .map(JobDetailPage::recapParameterClass)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();

        for (String recapClassName : recapClassNames) {
            generateExtractor(recapClassName);
        }
    }

    private void generateExtractor(String recapClassName) {
        ClassInfo recapClassInfo = index.getClassByName(DotName.createSimple(recapClassName));
        if (recapClassInfo == null) {
            LOG.warnf("Cannot generate RecapValueExtractor because recap class is not in Jandex index: %s", recapClassName);
            return;
        }
        if (!recapClassInfo.isRecord()) {
            LOG.warnf("Cannot generate RecapValueExtractor because recap class is not a record: %s", recapClassName);
            return;
        }

        List<RecordComponentInfo> supportedComponents = recapClassInfo.recordComponents().stream()
                .filter(component -> component.annotation(JOB_RECAP_PARAMETER) != null)
                .filter(component -> isSupportedRecapType(component.type()))
                .toList();

        String generatedClassName = generatedExtractorClassName(recapClassName);
        LOG.debugf("Generating RecapValueExtractor %s for recap class %s with %d supported components",
                generatedClassName, recapClassName, supportedComponents.size());

        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanProducer))
                .className(generatedClassName)
                .interfaces(RecapValueExtractor.class)
                .build()) {
            classCreator.addAnnotation(ApplicationScoped.class.getName(), RetentionPolicy.RUNTIME);
            createNoArgConstructor(classCreator, generatedClassName);
            createRecapClassNameMethod(classCreator, recapClassName);
            createExtractMethod(classCreator, recapClassName, supportedComponents);
        }
    }

    private void createNoArgConstructor(ClassCreator classCreator, String className) {
        MethodCreator constructor = classCreator.getMethodCreator(MethodDescriptor.ofConstructor(className));
        constructor.setModifiers(Modifier.PUBLIC);
        constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), constructor.getThis());
        constructor.returnValue(null);
    }

    private void createRecapClassNameMethod(ClassCreator classCreator, String recapClassName) {
        MethodCreator method = classCreator.getMethodCreator("recapClassName", String.class);
        method.setModifiers(Modifier.PUBLIC);
        method.returnValue(method.load(recapClassName));
    }

    private void createExtractMethod(ClassCreator classCreator, String recapClassName, List<RecordComponentInfo> components) {
        MethodCreator method = classCreator.getMethodCreator("extract", Map.class, Object.class);
        method.setModifiers(Modifier.PUBLIC);

        ResultHandle recapObject = method.getMethodParam(0);
        ResultHandle typedRecap = method.checkCast(recapObject, recapClassName);
        ResultHandle result = method.newInstance(MethodDescriptor.ofConstructor(HashMap.class));

        for (RecordComponentInfo component : components) {
            MethodDescriptor accessorDescriptor = createAccessorDescriptor(recapClassName, component);
            ResultHandle value = method.invokeVirtualMethod(accessorDescriptor, typedRecap);
            ResultHandle longValue = convertToLong(method, value, component.type());
            ResultHandle boxedLong = method.invokeStaticMethod(
                    MethodDescriptor.ofMethod(Long.class, "valueOf", Long.class, long.class),
                    longValue
            );
            method.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class, Object.class),
                    result,
                    method.load(component.name()),
                    boxedLong
            );
        }

        method.returnValue(result);
    }

    private MethodDescriptor createAccessorDescriptor(String recapClassName, RecordComponentInfo component) {
        Type type = component.type();
        if (type.kind() == Type.Kind.PRIMITIVE) {
            PrimitiveType.Primitive primitive = type.asPrimitiveType().primitive();
            if (primitive == PrimitiveType.Primitive.INT) {
                return MethodDescriptor.ofMethod(recapClassName, component.name(), int.class);
            }
            if (primitive == PrimitiveType.Primitive.LONG) {
                return MethodDescriptor.ofMethod(recapClassName, component.name(), long.class);
            }
        }
        if ("java.lang.Integer".equals(type.name().toString())) {
            return MethodDescriptor.ofMethod(recapClassName, component.name(), Integer.class);
        }
        return MethodDescriptor.ofMethod(recapClassName, component.name(), Long.class);
    }

    private ResultHandle convertToLong(MethodCreator method, ResultHandle value, Type type) {
        if (type.kind() == Type.Kind.PRIMITIVE) {
            PrimitiveType.Primitive primitive = type.asPrimitiveType().primitive();
            if (primitive == PrimitiveType.Primitive.INT) {
                return method.invokeStaticMethod(
                        MethodDescriptor.ofMethod(RecapValueExtractorSupport.class, "toLong", long.class, int.class),
                        value
                );
            }
            if (primitive == PrimitiveType.Primitive.LONG) {
                return method.invokeStaticMethod(
                        MethodDescriptor.ofMethod(RecapValueExtractorSupport.class, "toLong", long.class, long.class),
                        value
                );
            }
        }
        if ("java.lang.Integer".equals(type.name().toString())) {
            return method.invokeStaticMethod(
                    MethodDescriptor.ofMethod(RecapValueExtractorSupport.class, "toLong", long.class, Integer.class),
                    value
            );
        }
        return method.invokeStaticMethod(
                MethodDescriptor.ofMethod(RecapValueExtractorSupport.class, "toLong", long.class, Long.class),
                value
        );
    }

    private String generatedExtractorClassName(String recapClassName) {
        long hash = Integer.toUnsignedLong(recapClassName.hashCode());
        return "ch.css.jobrunr.control.generated.recap." + sanitizeClassName(recapClassName) + "_" + hash + "_RecapValueExtractor";
    }

    private String sanitizeClassName(String className) {
        return className.replace('.', '_').replace('$', '_');
    }

    private boolean isSupportedRecapType(Type type) {
        if (type == null || type.name() == null) {
            return false;
        }
        if (type.kind() == Type.Kind.PRIMITIVE) {
            PrimitiveType.Primitive primitive = type.asPrimitiveType().primitive();
            return primitive == PrimitiveType.Primitive.INT || primitive == PrimitiveType.Primitive.LONG;
        }
        String typeName = type.name().toString();
        return "java.lang.Integer".equals(typeName) || "java.lang.Long".equals(typeName);
    }
}
