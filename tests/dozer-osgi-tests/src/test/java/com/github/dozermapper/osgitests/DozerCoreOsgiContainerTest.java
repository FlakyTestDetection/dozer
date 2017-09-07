/*
 * Copyright 2005-2017 Dozer Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dozermapper.osgitests;

import javax.inject.Inject;

import com.github.dozermapper.osgitests.support.OsgiTestSupport;
import com.github.dozermapper.osgitestsmodel.Person;

import org.dozer.DozerBeanMapperBuilder;
import org.dozer.DozerModule;
import org.dozer.Mapper;
import org.dozer.el.ELExpressionFactory;
import org.dozer.osgi.Activator;
import org.dozer.osgi.OSGiClassLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.url;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class DozerCoreOsgiContainerTest extends OsgiTestSupport {

    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {

        return options(
                // Commons
                url("link:classpath:org.apache.commons.beanutils.link"),
                url("link:classpath:org.apache.commons.collections.link"),
                url("link:classpath:org.apache.commons.lang3.link"),
                url("link:classpath:org.apache.commons.io.link"),
                // JAXB
                url("link:classpath:org.apache.geronimo.specs.geronimo-stax-api_1.0_spec.link"),
                url("link:classpath:org.apache.geronimo.specs.geronimo-activation_1.1_spec.link"),
                url("link:classpath:org.apache.servicemix.specs.jaxb-api-2.2.link"),
                url("link:classpath:org.apache.servicemix.bundles.jaxb-impl.link"),
                // Optional; Jackson
                url("link:classpath:com.fasterxml.jackson.core.jackson-annotations.link"),
                url("link:classpath:com.fasterxml.jackson.core.jackson-core.link"),
                url("link:classpath:com.fasterxml.jackson.core.jackson-databind.link"),
                url("link:classpath:com.fasterxml.jackson.dataformat.jackson-dataformat-xml.link"),
                url("link:classpath:com.fasterxml.jackson.dataformat.jackson-dataformat-yaml.link"),
                url("link:classpath:com.fasterxml.jackson.module.jackson-module-jaxb-annotations.link"),
                url("link:classpath:com.fasterxml.woodstox.woodstox-core.link"),
                url("link:classpath:stax2-api.link"),
                url("link:classpath:org.yaml.snakeyaml.link"),
                // Optional; Javassist
                url("link:classpath:javassist.link"),
                // Optional; EL
                url("link:classpath:javax.el-api.link"),
                url("link:classpath:com.sun.el.javax.el.link"),
                // Optional; Hibernate
                url("link:classpath:org.hibernate.javax.persistence.hibernate-jpa-2.1-api.link"),
                url("link:classpath:org.apache.servicemix.bundles.dom4j.link"),
                url("link:classpath:org.apache.servicemix.bundles.antlr.link"),
                url("link:classpath:org.jboss.jandex.link"),
                url("link:classpath:org.jboss.logging.jboss-logging.link"),
                url("link:classpath:com.fasterxml.classmate.link"),
                url("link:classpath:org.hibernate.common.hibernate-commons-annotations.link"),
                url("link:classpath:org.hibernate.core.link"),
                url("link:classpath:org.hibernate.osgi.link"),
                // Core
                url("link:classpath:com.github.dozermapper.dozer-core.link"),
                url("link:classpath:com.github.dozermapper.dozer-schema.link"),
                url("link:classpath:com.github.dozermapper.tests.dozer-osgi-tests-model.link"),
                junitBundles(),
                // Required by: Hibernate
                systemPackage("javax.transaction;version=1.1.0"),
                systemPackage("javax.transaction.xa;version=1.1.0")
        );
    }

    @Test
    public void canGetBundleFromDozerCore() {
        assertNotNull(bundleContext);
        assertNotNull(Activator.getContext());
        assertNotNull(Activator.getBundle());

        Bundle core = getBundle(bundleContext, "com.github.dozermapper.dozer-core");
        assertNotNull(core);
        assertEquals(Bundle.ACTIVE, core.getState());

        assertNull(bundleContext.getServiceReference(DozerModule.class));

        for (Bundle current : bundleContext.getBundles()) {
            assertEquals(current.getSymbolicName(), Bundle.ACTIVE, current.getState());
        }
    }

    @Test
    public void canConstructDozerBeanMapper() {
        Mapper mapper = DozerBeanMapperBuilder.create()
                .withMappingFiles("mappings/mapping.xml")
                .withClassLoader(new OSGiClassLoader(com.github.dozermapper.osgitestsmodel.Activator.getBundleContext()))
                .build();

        assertNotNull(mapper);
    }

    @Test
    public void canMapUsingXML() {
        Mapper mapper = DozerBeanMapperBuilder.create()
                .withMappingFiles("mappings/mapping.xml")
                .withClassLoader(new OSGiClassLoader(com.github.dozermapper.osgitestsmodel.Activator.getBundleContext()))
                .build();

        Person answer = mapper.map(new Person("bob"), Person.class);

        assertNotNull(answer);
        assertNotNull(answer.getName());
        assertEquals("bob", answer.getName());
    }

    @Test
    public void canMapUsingXMLWithVariables() {
        Mapper mapper = DozerBeanMapperBuilder.create()
                .withMappingFiles("mappings/mapping-with-el.xml")
                .withClassLoader(new OSGiClassLoader(com.github.dozermapper.osgitestsmodel.Activator.getBundleContext()))
                .build();

        Person answer = mapper.map(new Person("bob"), Person.class);

        assertNotNull(answer);
        assertNotNull(answer.getName());
        assertEquals("bob", answer.getName());
    }

    @Test
    public void elSupported() {
        assertTrue(ELExpressionFactory.isSupported());
    }
}
