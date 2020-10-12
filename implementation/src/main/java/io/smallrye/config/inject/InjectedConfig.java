package io.smallrye.config.inject;

import static io.smallrye.config.inject.SecuritySupport.getContextClassLoader;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * The implementation of the injected {@link Config} bean.
 * <p>
 * This layer of indirection allows the injected {@code Config} to be serializable, without requiring all config sources
 * referenced by the {@code Config} to be so.
 */
public class InjectedConfig implements Config, Serializable {

    private static final long serialVersionUID = 1L;

    private transient Config instance;

    private Config getInstance() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = ConfigProvider.getConfig(getContextClassLoader());
                }
            }
        }
        return instance;
    }

    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        return getInstance().getValue(propertyName, propertyType);
    }

    @Override
    public ConfigValue getConfigValue(String propertyName) {
        return getInstance().getConfigValue(propertyName);
    }

    @Override
    public <T> List<T> getValues(String propertyName, Class<T> propertyType) {
        return getInstance().getValues(propertyName, propertyType);
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        return getInstance().getOptionalValue(propertyName, propertyType);
    }

    @Override
    public <T> Optional<List<T>> getOptionalValues(String propertyName, Class<T> propertyType) {
        return getInstance().getOptionalValues(propertyName, propertyType);
    }

    @Override
    public Iterable<String> getPropertyNames() {
        return getInstance().getPropertyNames();
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return getInstance().getConfigSources();
    }

    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
        return getInstance().getConverter(forType);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return getInstance().unwrap(type);
    }

}
