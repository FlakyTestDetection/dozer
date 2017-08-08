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
package org.dozer;

import java.util.ServiceLoader;

import org.dozer.builder.DestBeanBuilderCreator;
import org.dozer.classmap.generator.BeanMappingGenerator;
import org.dozer.config.BeanContainer;
import org.dozer.config.GlobalSettings;
import org.dozer.factory.DestBeanCreator;
import org.dozer.loader.xml.ELEngine;
import org.dozer.loader.xml.ExpressionElementReader;
import org.dozer.propertydescriptor.PropertyDescriptorFactory;
import org.dozer.stats.StatisticsManager;
import org.dozer.util.DefaultClassLoader;
import org.dozer.util.DozerClassLoader;
import org.dozer.util.DozerConstants;
import org.dozer.util.DozerProxyResolver;
import org.dozer.util.MappingUtils;
import org.dozer.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal class that performs one time Dozer initializations. Only intended for internal use.
 * Registers internal JMX MBeans if those are enabled in the configuration.
 *
 * @author tierney.matt
 * @author dmitry.buzdin
 */
public final class DozerInitializer {

  private final Logger log = LoggerFactory.getLogger(DozerInitializer.class);

  private volatile boolean isInitialized;

  public DozerInitializer() {
  }

  public void init(GlobalSettings globalSettings, StatisticsManager statsMgr, BeanContainer beanContainer,
                   DestBeanBuilderCreator destBeanBuilderCreator, BeanMappingGenerator beanMappingGenerator, PropertyDescriptorFactory propertyDescriptorFactory,
                   DestBeanCreator destBeanCreator) {
    init(globalSettings, getClass().getClassLoader().getParent(), statsMgr, beanContainer, destBeanBuilderCreator, beanMappingGenerator, propertyDescriptorFactory, destBeanCreator);
  }

  public void init(GlobalSettings globalSettings, ClassLoader classLoader, StatisticsManager statsMgr, BeanContainer beanContainer,
                   DestBeanBuilderCreator destBeanBuilderCreator, BeanMappingGenerator beanMappingGenerator, PropertyDescriptorFactory propertyDescriptorFactory,
                   DestBeanCreator destBeanCreator) {
    // Multiple threads may try to initialize simultaneously
    synchronized (this) {
      if (isInitialized) {
        log.debug("Tried to perform initialization when Dozer was already started.");
        return;
      }

      log.info("Initializing Dozer. Version: {}, Thread Name: {}",
              DozerConstants.CURRENT_VERSION, Thread.currentThread().getName());

      initialize(globalSettings, classLoader, statsMgr, beanContainer, destBeanBuilderCreator, beanMappingGenerator, propertyDescriptorFactory, destBeanCreator);

      isInitialized = true;
    }
  }

  void initialize(GlobalSettings globalSettings, ClassLoader classLoader, StatisticsManager statsMgr, BeanContainer beanContainer,
                  DestBeanBuilderCreator destBeanBuilderCreator, BeanMappingGenerator beanMappingGenerator, PropertyDescriptorFactory propertyDescriptorFactory,
                  DestBeanCreator destBeanCreator) {
    registerClassLoader(globalSettings, classLoader, beanContainer);
    registerProxyResolver(globalSettings, beanContainer);

    if (globalSettings.isElEnabled()) {
      ELEngine engine = new ELEngine();
      engine.init();
      beanContainer.setElEngine(engine);
      beanContainer.setElementReader(new ExpressionElementReader(engine));
    }

    for (DozerModule module : ServiceLoader.load(DozerModule.class)) {
      module.init();
      module.init(beanContainer, destBeanCreator, propertyDescriptorFactory);

      destBeanBuilderCreator.addPluggedStrategies(module.getBeanBuilderCreationStrategies());
      beanMappingGenerator.addPluggedFieldDetectors(module.getBeanFieldsDetectors());
      propertyDescriptorFactory.addPluggedPropertyDescriptorCreationStrategies(module.getPropertyDescriptorCreationStrategies());
    }
  }

  private void registerClassLoader(GlobalSettings globalSettings, ClassLoader classLoader, BeanContainer beanContainer) {
    String classLoaderName = globalSettings.getClassLoaderName();
    if (!DozerConstants.DEFAULT_CLASS_LOADER_BEAN.equals(classLoaderName)) {
      DefaultClassLoader defaultClassLoader = new DefaultClassLoader(classLoader);
      Class<? extends DozerClassLoader> classLoaderType = loadBeanType(classLoaderName, defaultClassLoader, DozerClassLoader.class);
      DozerClassLoader classLoaderBean = ReflectionUtils.newInstance(classLoaderType);
      beanContainer.setClassLoader(classLoaderBean);
    }
  }

  private void registerProxyResolver(GlobalSettings globalSettings, BeanContainer beanContainer) {
    String proxyResolverName = globalSettings.getProxyResolverName();
    if (!DozerConstants.DEFAULT_PROXY_RESOLVER_BEAN.equals(proxyResolverName)) {
      DozerClassLoader initializedClassLoader = beanContainer.getClassLoader();
      Class<? extends DozerProxyResolver> proxyResolverType = loadBeanType(proxyResolverName, initializedClassLoader, DozerProxyResolver.class);
      DozerProxyResolver proxyResolverBean = ReflectionUtils.newInstance(proxyResolverType);
      beanContainer.setProxyResolver(proxyResolverBean);
    }
  }

  private <T> Class<? extends T> loadBeanType(String classLoaderName, DozerClassLoader classLoader, Class<T> iface) {
    Class<?> beanType = classLoader.loadClass(classLoaderName);
    if (beanType != null && !iface.isAssignableFrom(beanType)) {
      MappingUtils.throwMappingException("Incompatible types: " + iface.getName() + " and " + classLoaderName);
    }
    return (Class<? extends T>) beanType;
  }

  /**
   * Performs framework shutdown sequence. Includes de-registering existing Dozer JMX MBeans.
   */
  public void destroy(GlobalSettings globalSettings) {
    synchronized (this) {
      if (!isInitialized) {
        log.debug("Tried to destroy when no Dozer instance started.");
        return;
      }

      isInitialized = false;
    }
  }

  public boolean isInitialized() {
    return isInitialized;
  }
}
