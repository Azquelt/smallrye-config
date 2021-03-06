package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static io.smallrye.config.ProfileConfigSourceInterceptor.SMALLRYE_PROFILE;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import io.smallrye.config.common.MapBackedConfigSource;

public class ProfileConfigSourceInterceptorTest {
    @Test
    public void profile() {
        final SmallRyeConfig config = buildConfig("my.prop", "1", "%prof.my.prop", "2", SMALLRYE_PROFILE, "prof");

        assertEquals("2", config.getValue("my.prop", String.class));

        assertEquals("my.prop", config.getConfigValue("my.prop").getName());
        assertEquals("my.prop", config.getConfigValue("%prof.my.prop").getName());
        assertEquals("2", config.getConfigValue("%prof.my.prop").getValue());
    }

    @Test
    public void profileOnly() {
        final Config config = buildConfig("%prof.my.prop", "2", SMALLRYE_PROFILE, "prof");

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    public void fallback() {
        final Config config = buildConfig("my.prop", "1", SMALLRYE_PROFILE, "prof");

        assertEquals("1", config.getValue("my.prop", String.class));
    }

    @Test
    public void expressions() {
        final Config config = buildConfig("my.prop", "1", "%prof.my.prop", "${my.prop}", SMALLRYE_PROFILE, "prof");

        assertThrows(IllegalArgumentException.class, () -> config.getValue("my.prop", String.class));
    }

    @Test
    public void profileExpressions() {
        final Config config = buildConfig("my.prop", "1",
                "%prof.my.prop", "${%prof.my.prop.profile}",
                "%prof.my.prop.profile", "2",
                SMALLRYE_PROFILE, "prof");

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    void cannotExpand() {
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config("my.prop", "${another.prop}", SMALLRYE_PROFILE, "prof", "config_ordinal", "1000"))
                .withSources(config("my.prop", "${another.prop}", "%prof.my.prop", "2", SMALLRYE_PROFILE, "prof"))
                .build();

        assertThrows(NoSuchElementException.class, () -> config.getValue("my.prop", String.class));
    }

    @Test
    public void customConfigProfile() {
        final String[] configs = { "my.prop", "1", "%prof.my.prop", "2", "config.profile", "prof" };
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDiscoveredInterceptors()
                .withSources(config(configs))
                .build();

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    public void noConfigProfile() {
        final String[] configs = { "my.prop", "1", "%prof.my.prop", "2" };
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config(configs))
                .build();

        assertEquals("1", config.getValue("my.prop", String.class));
    }

    @Test
    public void priorityProfile() {
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(
                        new MapBackedConfigSource("higher", new HashMap<String, String>() {
                            {
                                put("%prof.my.prop", "higher-profile");
                            }
                        }, 200) {
                        },
                        new MapBackedConfigSource("lower", new HashMap<String, String>() {
                            {
                                put("my.prop", "lower");
                                put("%prof.my.prop", "lower-profile");
                            }
                        }, 100) {
                        })
                .withInterceptors(
                        new ProfileConfigSourceInterceptor("prof"),
                        new ExpressionConfigSourceInterceptor())
                .build();

        assertEquals("higher-profile", config.getValue("my.prop", String.class));
    }

    @Test
    public void priorityOverrideProfile() {
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(
                        new MapBackedConfigSource("higher", new HashMap<String, String>() {
                            {
                                put("my.prop", "higher");
                            }
                        }, 200) {
                        },
                        new MapBackedConfigSource("lower", new HashMap<String, String>() {
                            {
                                put("my.prop", "lower");
                                put("%prof.my.prop", "lower-profile");
                            }
                        }, 100) {
                        })
                .withInterceptors(
                        new ProfileConfigSourceInterceptor("prof"),
                        new ExpressionConfigSourceInterceptor())
                .build();

        assertEquals("higher", config.getValue("my.prop", String.class));
    }

    @Test
    public void priorityProfileOverOriginal() {
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(
                        new MapBackedConfigSource("higher", new HashMap<String, String>() {
                            {
                                put("my.prop", "higher");
                                put("%prof.my.prop", "higher-profile");
                            }
                        }, 200) {
                        },
                        new MapBackedConfigSource("lower", new HashMap<String, String>() {
                            {
                                put("my.prop", "lower");
                                put("%prof.my.prop", "lower-profile");
                            }
                        }, 100) {
                        })
                .withInterceptors(
                        new ProfileConfigSourceInterceptor("prof"),
                        new ExpressionConfigSourceInterceptor())
                .build();

        assertEquals("higher-profile", config.getValue("my.prop", String.class));
    }

    @Test
    public void propertyNames() {
        final SmallRyeConfig config = buildConfig("my.prop", "1", "%prof.my.prop", "2", "%prof.prof.only", "1",
                SMALLRYE_PROFILE, "prof");

        assertEquals("2", config.getConfigValue("my.prop").getValue());
        assertEquals("1", config.getConfigValue("prof.only").getValue());

        final List<String> properties = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertFalse(properties.contains("%prof.my.prop"));
        assertTrue(properties.contains("my.prop"));
        assertTrue(properties.contains("prof.only"));
    }

    @Test
    void excludePropertiesFromInactiveProfiles() {
        final SmallRyeConfig config = buildConfig("%prof.my.prop", "1", "%foo.another", "2");

        final List<String> properties = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertTrue(properties.contains("my.prop"));
        assertFalse(properties.contains("another"));
    }

    @Test
    public void profileName() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("my.prop", "1", "%prof.my.prop", "2"))
                .withProfile("prof")
                .build();

        assertEquals("2", config.getConfigValue("my.prop").getValue());
    }

    @Test
    void multipleProfiles() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_PROFILE, "common,prof", "config_ordinal", "1000"))
                .withSources(config("%common.common.prop", "1234", "%prof.my.prop", "5678"))
                .addDefaultInterceptors()
                .build();

        assertEquals("1234", config.getRawValue("common.prop"));
        assertEquals("5678", config.getRawValue("my.prop"));
    }

    @Test
    void multipleProfilesSamePriority() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_PROFILE, "foo,bar", "config_ordinal", "1000"))
                .withSources(config("%foo.common.prop", "1234", "%bar.common.prop", "5678"))
                .addDefaultInterceptors()
                .build();

        assertEquals("5678", config.getRawValue("common.prop"));
    }

    @Test
    void multipleProfilesDifferentPriorities() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_PROFILE, "common,prof", "config_ordinal", "1000"))
                .withSources(config("%prof.common.prop", "5678", "config_ordinal", "300"))
                .withSources(config("%common.common.prop", "1234", "config_ordinal", "500"))
                .addDefaultInterceptors()
                .build();

        assertEquals("5678", config.getRawValue("common.prop"));
    }

    @Test
    void multipleProfilesDifferentPrioritiesMain() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_PROFILE, "common,prof", "config_ordinal", "1000"))
                .withSources(config("common.prop", "9", "config_ordinal", "900"))
                .withSources(config("%prof.common.prop", "5678", "config_ordinal", "500"))
                .withSources(config("%common.common.prop", "1234", "config_ordinal", "300"))
                .addDefaultInterceptors()
                .build();

        assertEquals("9", config.getRawValue("common.prop"));
    }

    private static SmallRyeConfig buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .withSources(config(keyValues))
                .withInterceptors(
                        new ProfileConfigSourceInterceptor("prof"),
                        new ExpressionConfigSourceInterceptor())
                .build();
    }
}
