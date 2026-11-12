package spring.study.regression;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import java.io.InputStream;

import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

class DomainLayerArchitectureTest {
    private static final Set<String> HTTP_ADAPTERS = Set.of(
            "spring.study.common.service.JwtManager",
            "spring.study.common.service.ClientIpResolver",
            "spring.study.jwt.service.JwtCookieService");

    @Test
    void springConnectsEveryRealControllerAndFacadeWithoutMissingBeansOrCycles() throws Exception {
        List<Class<?>> applicationTypes = new ArrayList<>(classes("controller"));
        applicationTypes.removeIf(type -> !type.isAnnotationPresent(Controller.class)
                && !type.isAnnotationPresent(RestController.class));
        applicationTypes.addAll(classes("facade"));
        assertThat(applicationTypes.size()).isGreaterThan(60);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(applicationTypes.toArray(Class<?>[]::new));
            // Only lower services/infrastructure are mocked, never a controller or facade.
            applicationTypes.stream()
                    .flatMap(type -> Arrays.stream(type.getDeclaredConstructors()))
                    .flatMap(constructor -> Arrays.stream(constructor.getParameters()))
                    .filter(parameter -> !applicationTypes.contains(parameter.getType()))
                    .forEach(parameter -> registerDependency(context, parameter));
            context.refresh();
            for (Class<?> type : applicationTypes) {
                assertThat(mockingDetails(context.getBean(type)).isMock()).as(type.getName()).isFalse();
            }
        }
    }

    @Test
    void controllersDoNotBypassFacadesAndFacadesDoNotAccessRepositories() throws Exception {
        for (String layer : List.of("controller", "facade")) {
            for (Class<?> type : classes(layer)) {
                for (Field field : type.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    String dependency = field.getType().getName();
                    assertThat(dependency).as(type.getName() + "." + field.getName()).doesNotContain(".repository.");
                    if (layer.equals("facade")) {
                        assertThat(dependency).as(type.getName() + "." + field.getName()).doesNotContain(".facade.");
                    }
                    if (layer.equals("controller") && !HTTP_ADAPTERS.contains(dependency)) {
                        assertThat(dependency).as(type.getName() + "." + field.getName()).doesNotContain(".service.");
                    }
                }
            }
        }
    }

    @Test
    void servicesNeverDependOnFacadesOrReturnHttpResponses() throws Exception {
        for (Class<?> type : classes("service")) {
            if (!type.isAnnotationPresent(Service.class)) continue;
            for (Field field : type.getDeclaredFields()) {
                assertThat(field.getType().getName()).as(type.getName() + "." + field.getName()).doesNotContain(".facade.");
                assertThat(field.getType().getName()).as(type.getName() + "." + field.getName()).doesNotContain(".service.");
            }
            long repositories = Arrays.stream(type.getDeclaredFields())
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .filter(field -> field.getType().getName().contains(".repository."))
                    .count();
            assertThat(repositories).as(type.getName() + " repository dependencies").isLessThanOrEqualTo(1L);
            for (Method method : type.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) continue;
                assertThat(ResponseEntity.class.isAssignableFrom(method.getReturnType())).as(method.toGenericString()).isFalse();
                if (!HTTP_ADAPTERS.contains(type.getName())) {
                    assertThat(Arrays.stream(method.getParameterTypes()).map(Class::getName))
                            .as(method.toGenericString()).noneMatch(name -> name.startsWith("jakarta.servlet."));
                }
            }
        }
    }

    @Test
    void bytecodeDoesNotCallForbiddenLayersEvenWithoutAnInjectedField() throws Exception {
        for (String layer : List.of("facade", "service")) {
            for (Class<?> type : classes(layer)) {
                String source = type.getName().replace('.', '/');
                try (InputStream bytecode = type.getResourceAsStream(type.getSimpleName() + ".class")) {
                    assertThat(bytecode).as(type.getName()).isNotNull();
                    new ClassReader(bytecode).accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            return new MethodVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitMethodInsn(int opcode, String owner, String method, String methodDescriptor, boolean isInterface) {
                                    if (!owner.startsWith("spring/study/") || owner.equals(source) || owner.startsWith(source + "$")) return;
                                    assertThat(owner).as(type.getName() + "." + name + " -> " + owner + "." + method)
                                            .doesNotContain("/facade/", layer.equals("facade") ? "/repository/" : "/service/");
                                }
                            };
                        }
                    }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }
    }

    private List<Class<?>> classes(String layer) throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        CachingMetadataReaderFactory readers = new CachingMetadataReaderFactory(resolver);
        List<Class<?>> classes = new ArrayList<>();
        for (Resource resource : resolver.getResources("classpath*:spring/study/**/" + layer + "/*.class")) {
            String name = readers.getMetadataReader(resource).getClassMetadata().getClassName();
            if (!name.contains("$") && !name.endsWith("Test")) classes.add(Class.forName(name));
        }
        return classes.stream().distinct().toList();
    }

    private void registerDependency(AnnotationConfigApplicationContext context, Parameter parameter) {
        Class<?> type = parameter.getType();
        Qualifier qualifier = parameter.getAnnotation(Qualifier.class);
        String beanName = qualifier == null || qualifier.value().isBlank() ? type.getName() : qualifier.value();
        if (!context.getBeanFactory().containsSingleton(beanName)) {
            context.getBeanFactory().registerSingleton(beanName, mock(type));
        }
    }
}
