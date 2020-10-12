package io.smallrye.config.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WeldJunit5Extension.class)
public class InjectedConfigSerializationTest extends InjectionTest {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, Config.class)
            .build();

    @Inject
    private Config config;

    @Test
    public void injectableConfigIsSerializable() throws IOException, ClassNotFoundException {

        // Precondition: check that non-injected config is not serializable
        // Note: test config in InjectionTest has a non-serializable config source
        assertNotSerializable(ConfigProvider.getConfig());

        // Check that the injected config is serializable
        Config newConfig = (Config) assertSerializable(config);

        // Check that the config still works after serialization
        assertEquals("1234", config.getValue("my.prop", String.class));
        assertEquals("1234", newConfig.getValue("my.prop", String.class));
    }

    /**
     * Asserts that an object can be serialized and deserialized
     * 
     * @param o the object
     * @return the resulting object after serializing and deserializing
     */
    private static Object assertSerializable(Object o) {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        try {
            ObjectOutputStream out = new ObjectOutputStream(bytesOut);
            out.writeObject(o);
        } catch (IOException e) {
            fail("Could not serialize object: " + o, e);
        }

        Object result = null;
        try {
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesOut.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bytesIn);
            result = in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            fail("Could not deserialize object: " + o, e);
        }
        return result;
    }

    /**
     * Asserts that an object cannot be serialized
     * 
     * @param o the object
     */
    private static void assertNotSerializable(Object o) {
        try {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bytesOut);
            out.writeObject(o);
            fail("Object unexpectedly serialized successfully: " + o);
        } catch (IOException e) {
            // Expected
        }
    }
}
