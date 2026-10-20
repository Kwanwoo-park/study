package spring.study.regression;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
                    .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                    .filter(type -> !applicationTypes.contains(type))
                    .distinct().forEach(type -> registerDependency(context, type));
            context.refresh();
            for (Class<?> type : applicationTypes) {
                assertThat(mockingDetails(context.getBean(type)).isMock()).as(type.getName()).isFalse();
            }
        }
    }

    @Test
    void recordsAndEnumsAreDeclaredInSeparateTopLevelFiles() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        var readers = new CachingMetadataReaderFactory(resolver);
        int valueTypes = 0;
        for (var resource : resolver.getResources("classpath*:spring/study/**/*.class")) {
            String name = readers.getMetadataReader(resource).getClassMetadata().getClassName();
            Class<?> type = Class.forName(name, false, getClass().getClassLoader());
            if (!type.isRecord() && !type.isEnum()) continue;
            valueTypes++;
            assertThat(type.getEnclosingClass()).as(name).isNull();
        }
        assertThat(valueTypes).isPositive();
    }

    @Test
    void controllersDoNotBypassFacadesAndFacadesDoNotAccessRepositories() throws Exception {
        for (String layer : List.of("controller", "facade")) {
            for (Class<?> type : classes(layer)) {
                for (var field : type.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    String dependency = field.getType().getName();
                    assertThat(dependency).as(type.getName() + "." + field.getName()).doesNotContain(".repository.");
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
            for (var field : type.getDeclaredFields()) {
                assertThat(field.getType().getName()).as(type.getName() + "." + field.getName()).doesNotContain(".facade.");
            }
            for (var method : type.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) continue;
                assertThat(ResponseEntity.class.isAssignableFrom(method.getReturnType())).as(method.toGenericString()).isFalse();
                if (!HTTP_ADAPTERS.contains(type.getName())) {
                    assertThat(Arrays.stream(method.getParameterTypes()).map(Class::getName))
                            .as(method.toGenericString()).noneMatch(name -> name.startsWith("jakarta.servlet."));
                }
            }
        }
    }

    private List<Class<?>> classes(String layer) throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        var readers = new CachingMetadataReaderFactory(resolver);
        List<Class<?>> classes = new ArrayList<>();
        for (var resource : resolver.getResources("classpath*:spring/study/**/" + layer + "/*.class")) {
            String name = readers.getMetadataReader(resource).getClassMetadata().getClassName();
            if (!name.contains("$") && !name.endsWith("Test")) classes.add(Class.forName(name));
        }
        return classes.stream().distinct().toList();
    }

    private <T> void registerDependency(AnnotationConfigApplicationContext context, Class<T> type) {
        context.getBeanFactory().registerSingleton(type.getName(), mock(type));
    }
}
