package org.motechproject.mds.entityinfo;

import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.gemini.blueprint.util.OsgiBundleUtils;
import org.motechproject.mds.util.Constants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of the {@link EntityInfoReader} which reads entity information from json
 * files from the META-INF/entity-info directory inside the classpath. Used by mds-entities bundle,
 * which has all entity schema packed inside of it.
 */
public class EntityInfoReaderImpl implements EntityInfoReader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private BundleContext bundleContext;

    @Override
    public EntityInfo getEntityInfo(String entityClassName) {
        String file = "META-INF/entity-info/" + entityClassName + ".json";

        // the file is inside the entities bundle
        ClassLoader entitiesCl = getMdsEntitiesBundleClassLoader();

        try (InputStream in = entitiesCl.getResourceAsStream(file)) {
            return objectMapper.readValue(in, EntityInfo.class);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read entity info for " + entityClassName, e);
        }
    }

    // TODO: MOTECH-1466 - use util/helper here after redoing MDS package structure
    private ClassLoader getMdsEntitiesBundleClassLoader() {
        Bundle bundle =  OsgiBundleUtils.findBundleBySymbolicName(bundleContext, Constants.BundleNames.MDS_ENTITIES_SYMBOLIC_NAME);
        return bundle.adapt(BundleWiring.class).getClassLoader();
    }
}
