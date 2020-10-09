package io.smallrye.config;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;

import io.smallrye.common.classloader.ClassDefiner;

public final class ConfigMappingLoader {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final ClassValue<ConfigMappingObjectHolder> CACHE = new ClassValue<ConfigMappingObjectHolder>() {
        @Override
        protected ConfigMappingObjectHolder computeValue(final Class<?> type) {
            return new ConfigMappingObjectHolder(getImplementationClass(type));
        }
    };

    public static List<ConfigMappingMetadata> getConfigMappingsMetadata(Class<?> type) {
        final List<ConfigMappingMetadata> mappings = new ArrayList<>();
        final ConfigMappingInterface configurationInterface = ConfigMappingInterface.getConfigurationInterface(type);
        if (configurationInterface != null) {
            mappings.add(configurationInterface);
            mappings.addAll(configurationInterface.getNested());
        }
        final ConfigMappingClass configMappingClass = ConfigMappingClass.getConfigurationClass(type);
        if (configMappingClass != null) {
            mappings.add(configMappingClass);
        }
        return mappings;
    }

    static ConfigMappingInterface getConfigMappingInterface(final Class<?> type) {
        return ConfigMappingInterface.getConfigurationInterface(getConfigMappingClass(type));
    }

    static Class<?> getConfigMappingClass(final Class<?> type) {
        final ConfigMappingClass configMappingClass = ConfigMappingClass.getConfigurationClass(type);
        if (configMappingClass == null) {
            return type;
        } else {
            return loadClass(type.getClassLoader(), configMappingClass.getClassName(), configMappingClass.getClassBytes());
        }
    }

    static <T> T configMappingObject(Class<T> interfaceType, ConfigMappingContext configMappingContext) {
        ConfigMappingObject instance;
        try {
            Constructor<? extends ConfigMappingObject> constructor = CACHE.get(interfaceType).getImplementationClass()
                    .getDeclaredConstructor(ConfigMappingContext.class);
            instance = constructor.newInstance(configMappingContext);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        } catch (InstantiationException e) {
            throw new InstantiationError(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            try {
                throw e.getCause();
            } catch (RuntimeException | Error e2) {
                throw e2;
            } catch (Throwable t) {
                throw new UndeclaredThrowableException(t);
            }
        }
        return interfaceType.cast(instance);
    }

    @SuppressWarnings("unchecked")
    static <T> Class<? extends ConfigMappingObject> getImplementationClass(Class<T> type) {
        final ConfigMappingMetadata mappingMetadata = ConfigMappingInterface.getConfigurationInterface(type);
        return (Class<? extends ConfigMappingObject>) loadClass(type.getClassLoader(),
                mappingMetadata.getClassName(),
                mappingMetadata.getClassBytes());
    }

    static Class<?> loadClass(final ClassLoader classLoader, final String className, final byte[] classBytes) {
        // Check if the interface implementation was already loaded. If not we will load it.
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            return loadClass(className, classBytes);
        }
    }

    private static Class<?> loadClass(final String className, final byte[] classBytes) {
        return ClassDefiner.defineClass(LOOKUP, ConfigMappingLoader.class, className, classBytes);
    }

    private static final class ConfigMappingObjectHolder {
        private final Class<? extends ConfigMappingObject> implementationClass;

        ConfigMappingObjectHolder(final Class<? extends ConfigMappingObject> implementationClass) {
            this.implementationClass = implementationClass;
        }

        public Class<? extends ConfigMappingObject> getImplementationClass() {
            return implementationClass;
        }
    }
}
