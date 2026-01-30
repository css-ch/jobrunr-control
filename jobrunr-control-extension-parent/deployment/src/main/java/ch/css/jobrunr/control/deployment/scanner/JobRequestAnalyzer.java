package ch.css.jobrunr.control.deployment.scanner;

import org.jboss.jandex.*;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

/**
 * Analyzes JobRequest types and their relationships with JobRequestHandler implementations.
 * Handles type parameter resolution and interface hierarchy traversal.
 */
public class JobRequestAnalyzer {

    private static final Logger log = Logger.getLogger(JobRequestAnalyzer.class);

    private static final DotName JOB_REQUEST_HANDLER = DotName.createSimple(JobRequestHandler.class.getName());
    private static final DotName JOB_REQUEST = DotName.createSimple(JobRequest.class.getName());

    private final IndexView index;

    public JobRequestAnalyzer(IndexView index) {
        this.index = index;
    }

    /**
     * Finds the JobRequest type parameter from a JobRequestHandler implementation.
     *
     * @param classInfo the class implementing JobRequestHandler
     * @return the JobRequest type, or null if not found
     */
    public Type findJobRequestType(ClassInfo classInfo) {
        Type jobRequestType = findParameterizedInterfaceArgument(classInfo, JOB_REQUEST_HANDLER);

        if (jobRequestType == null || jobRequestType.kind() != Type.Kind.CLASS) {
            return null;
        }

        ClassInfo requestClassInfo = index.getClassByName(jobRequestType.name());
        if (isValidJobRequest(requestClassInfo)) {
            log.debugf("JobRequest type resolved: %s", jobRequestType.name());
            return jobRequestType;
        }

        return null;
    }

    private boolean isValidJobRequest(ClassInfo classInfo) {
        return classInfo != null
                && isRecord(classInfo)
                && implementsInterfaceRecursive(classInfo, JOB_REQUEST);
    }

    private boolean isRecord(ClassInfo classInfo) {
        if (classInfo.superName() != null && "java.lang.Record".equals(classInfo.superName().toString())) {
            return true;
        }
        return (classInfo.flags() & 0x10) != 0;
    }

    private Type findParameterizedInterfaceArgument(ClassInfo classInfo, DotName targetInterface) {
        for (Type interfaceType : classInfo.interfaceTypes()) {
            Type result = checkInterface(interfaceType, targetInterface);
            if (result != null) {
                return result;
            }

            result = recurseIntoInterface(interfaceType, targetInterface);
            if (result != null) {
                return result;
            }
        }

        return checkSuperclass(classInfo, targetInterface);
    }

    private Type checkInterface(Type interfaceType, DotName targetInterface) {
        if (interfaceType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            return null;
        }

        ParameterizedType pt = interfaceType.asParameterizedType();
        if (pt.arguments().isEmpty()) {
            return null;
        }

        if (pt.name().equals(targetInterface)) {
            return pt.arguments().getFirst();
        }

        ClassInfo ifaceInfo = index.getClassByName(pt.name());
        if (ifaceInfo != null && implementsInterfaceRecursive(ifaceInfo, targetInterface)) {
            return pt.arguments().getFirst();
        }

        return null;
    }

    private Type recurseIntoInterface(Type interfaceType, DotName targetInterface) {
        ClassInfo ifaceInfo = index.getClassByName(interfaceType.name());
        if (ifaceInfo != null) {
            return findParameterizedInterfaceArgument(ifaceInfo, targetInterface);
        }
        return null;
    }

    private Type checkSuperclass(ClassInfo classInfo, DotName targetInterface) {
        if (classInfo.superName() == null) {
            return null;
        }

        ClassInfo superClass = index.getClassByName(classInfo.superName());
        if (superClass != null) {
            return findParameterizedInterfaceArgument(superClass, targetInterface);
        }

        return null;
    }

    /**
     * Checks whether the class or any of its superclasses/interfaces implement targetInterface.
     */
    private boolean implementsInterfaceRecursive(ClassInfo classInfo, DotName targetInterface) {
        for (Type interfaceType : classInfo.interfaceTypes()) {
            if (interfaceType.name().equals(targetInterface)) {
                return true;
            }
            ClassInfo ifaceInfo = index.getClassByName(interfaceType.name());
            if (ifaceInfo != null && implementsInterfaceRecursive(ifaceInfo, targetInterface)) {
                return true;
            }
        }

        if (classInfo.superName() != null) {
            ClassInfo superClass = index.getClassByName(classInfo.superName());
            return superClass != null && implementsInterfaceRecursive(superClass, targetInterface);
        }

        return false;
    }
}
