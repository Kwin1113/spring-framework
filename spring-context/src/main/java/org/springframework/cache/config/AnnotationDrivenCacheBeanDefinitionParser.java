/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.config;

import org.w3c.dom.Element;

import org.springframework.aop.config.AopNamespaceUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser}
 * 的实现类，允许用户轻松配置所有需要注解缓存区分的基础bean。
 *
 * 默认情况下，所有代理都是通过JDK代理完成。当你通过聚合类注入对象而不是通过接口注入
 * 时，可能会出问题。你可以把'{@code proxy-target-class}' 属性设置为 '{@code true}',
 * 实现基于类的代理。
 *
 * 如果存在JSR-107和Spring的JCache的实现类，处理 {@code CacheResult},
 * {@code CachePut}, {@code CacheRemove} 或 {@code CacheRemoveAll}
 * 注解方法的必要基础bean也会被创建。
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @since 3.1
 */
class AnnotationDrivenCacheBeanDefinitionParser implements BeanDefinitionParser {

	private static final String CACHE_ASPECT_CLASS_NAME =
			"org.springframework.cache.aspectj.AnnotationCacheAspect";

	private static final String JCACHE_ASPECT_CLASS_NAME =
			"org.springframework.cache.aspectj.JCacheCacheAspect";

	private static final boolean jsr107Present;

	private static final boolean jcacheImplPresent;

	static {
		//获得类加载器查看jsr107和jcache的相关类是否被加载，并初始化这两个布尔值变量
		ClassLoader classLoader = AnnotationDrivenCacheBeanDefinitionParser.class.getClassLoader();
		jsr107Present = ClassUtils.isPresent("javax.cache.Cache", classLoader);
		jcacheImplPresent = ClassUtils.isPresent(
				"org.springframework.cache.jcache.interceptor.DefaultJCacheOperationSource", classLoader);
	}


	/**
	 * 解析'{@code <cache:annotation-driven>}'标签。必要时将会通过
	 * {@link AopNamespaceUtils#registerAutoProxyCreatorIfNecessary}在容器中创建一个自动代理创建者AutoProxyCreator。
	 */
	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		String mode = element.getAttribute("mode");
		if ("aspectj".equals(mode)) {
			// mode="aspectj"
			registerCacheAspect(element, parserContext);
		}
		else {
			// mode="proxy"
			registerCacheAdvisor(element, parserContext);
		}
		return null;
	}

	/** 通过AspectJ方式来拦截 */
	private void registerCacheAspect(Element element, ParserContext parserContext) {
		SpringCachingConfigurer.registerCacheAspect(element, parserContext);
		if (jsr107Present && jcacheImplPresent) {
			JCacheCachingConfigurer.registerCacheAspect(element, parserContext);
		}
	}

	/** 通过Spring代理来拦截 */
	private void registerCacheAdvisor(Element element, ParserContext parserContext) {
		AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(parserContext, element);
		SpringCachingConfigurer.registerCacheAdvisor(element, parserContext);
		if (jsr107Present && jcacheImplPresent) {
			JCacheCachingConfigurer.registerCacheAdvisor(element, parserContext);
		}
	}

	/**
	 * 解析使用的缓存解析策略。如果设置了'cache-resolver'属性，则将其注入；否则设置'cache-manager'
	 * 如果 {@code setBoth} 为 {@code true}，两个组件都会被注入。
	 */
	private static void parseCacheResolution(Element element, BeanDefinition def, boolean setBoth) {
		String name = element.getAttribute("cache-resolver");
		boolean hasText = StringUtils.hasText(name);
		if (hasText) {
			def.getPropertyValues().add("cacheResolver", new RuntimeBeanReference(name.trim()));
		}
		if (!hasText || setBoth) {
			def.getPropertyValues().add("cacheManager",
					new RuntimeBeanReference(CacheNamespaceHandler.extractCacheManager(element)));
		}
	}

	private static void parseErrorHandler(Element element, BeanDefinition def) {
		String name = element.getAttribute("error-handler");
		if (StringUtils.hasText(name)) {
			def.getPropertyValues().add("errorHandler", new RuntimeBeanReference(name.trim()));
		}
	}


	/**
	 * 配置基础属性，以支持Spring缓存注解。
	 */
	private static class SpringCachingConfigurer {

		private static void registerCacheAdvisor(Element element, ParserContext parserContext) {
			if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME)) {
				Object eleSource = parserContext.extractSource(element);

				// 创建缓存操作资源定义
				RootBeanDefinition sourceDef = new RootBeanDefinition(
						"org.springframework.cache.annotation.AnnotationCacheOperationSource");
				sourceDef.setSource(eleSource);
				sourceDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				String sourceName = parserContext.getReaderContext().registerWithGeneratedName(sourceDef);

				// 创建缓存拦截器定义
				RootBeanDefinition interceptorDef = new RootBeanDefinition(CacheInterceptor.class);
				interceptorDef.setSource(eleSource);
				interceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				parseCacheResolution(element, interceptorDef, false);
				parseErrorHandler(element, interceptorDef);
				CacheNamespaceHandler.parseKeyGenerator(element, interceptorDef);
				interceptorDef.getPropertyValues().add("cacheOperationSources",
						new RuntimeBeanReference(sourceName));
				String interceptorName = parserContext.getReaderContext().registerWithGeneratedName(interceptorDef);

				// 创建缓存增强定义
				RootBeanDefinition advisorDef = new RootBeanDefinition(BeanFactoryCacheOperationSourceAdvisor.class);
				advisorDef.setSource(eleSource);
				advisorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				advisorDef.getPropertyValues().add("cacheOperationSource",
						new RuntimeBeanReference(sourceName));
				advisorDef.getPropertyValues().add("adviceBeanName", interceptorName);
				if (element.hasAttribute("order")) {
					advisorDef.getPropertyValues().add("order", element.getAttribute("order"));
				}
				parserContext.getRegistry().registerBeanDefinition(CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME,
						advisorDef);

				CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), eleSource);
				compositeDef.addNestedComponent(new BeanComponentDefinition(sourceDef, sourceName));
				compositeDef.addNestedComponent(new BeanComponentDefinition(interceptorDef, interceptorName));
				compositeDef.addNestedComponent(new BeanComponentDefinition(advisorDef, CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME));
				parserContext.registerComponent(compositeDef);
			}
		}

		/**
		 * 注册一个缓存切面。
		 * <pre class="code">
		 * < bean id="cacheAspect" class="org.springframework.cache.aspectj.AnnotationCacheAspect" factory-method="aspectOf">
		 *   < property name="cacheManager" ref="cacheManager"/>
		 *   < property name="keyGenerator" ref="keyGenerator"/>
		 * < /bean>
		 * </pre>
		 */
		private static void registerCacheAspect(Element element, ParserContext parserContext) {
			if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.CACHE_ASPECT_BEAN_NAME)) {
				RootBeanDefinition def = new RootBeanDefinition();
				def.setBeanClassName(CACHE_ASPECT_CLASS_NAME);
				def.setFactoryMethodName("aspectOf");
				parseCacheResolution(element, def, false);
				CacheNamespaceHandler.parseKeyGenerator(element, def);
				parserContext.registerBeanComponent(new BeanComponentDefinition(def, CacheManagementConfigUtils.CACHE_ASPECT_BEAN_NAME));
			}
		}
	}


	/**
	 * 配置必要的基础属性以支持JSR-107标准的缓存注解。
	 */
	private static class JCacheCachingConfigurer {

		private static void registerCacheAdvisor(Element element, ParserContext parserContext) {
			if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME)) {
				Object source = parserContext.extractSource(element);

				// 创建缓存操作资源定义
				BeanDefinition sourceDef = createJCacheOperationSourceBeanDefinition(element, source);
				String sourceName = parserContext.getReaderContext().registerWithGeneratedName(sourceDef);

				// 创建缓存拦截器定义
				RootBeanDefinition interceptorDef =
						new RootBeanDefinition("org.springframework.cache.jcache.interceptor.JCacheInterceptor");
				interceptorDef.setSource(source);
				interceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				interceptorDef.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(sourceName));
				parseErrorHandler(element, interceptorDef);
				String interceptorName = parserContext.getReaderContext().registerWithGeneratedName(interceptorDef);

				// 创建缓存增强定义
				RootBeanDefinition advisorDef = new RootBeanDefinition(
						"org.springframework.cache.jcache.interceptor.BeanFactoryJCacheOperationSourceAdvisor");
				advisorDef.setSource(source);
				advisorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				advisorDef.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(sourceName));
				advisorDef.getPropertyValues().add("adviceBeanName", interceptorName);
				if (element.hasAttribute("order")) {
					advisorDef.getPropertyValues().add("order", element.getAttribute("order"));
				}
				parserContext.getRegistry().registerBeanDefinition(CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME, advisorDef);

				CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), source);
				compositeDef.addNestedComponent(new BeanComponentDefinition(sourceDef, sourceName));
				compositeDef.addNestedComponent(new BeanComponentDefinition(interceptorDef, interceptorName));
				compositeDef.addNestedComponent(new BeanComponentDefinition(advisorDef, CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME));
				parserContext.registerComponent(compositeDef);
			}
		}

		private static void registerCacheAspect(Element element, ParserContext parserContext) {
			if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.JCACHE_ASPECT_BEAN_NAME)) {
				Object eleSource = parserContext.extractSource(element);
				RootBeanDefinition def = new RootBeanDefinition();
				def.setBeanClassName(JCACHE_ASPECT_CLASS_NAME);
				def.setFactoryMethodName("aspectOf");
				BeanDefinition sourceDef = createJCacheOperationSourceBeanDefinition(element, eleSource);
				String sourceName =
						parserContext.getReaderContext().registerWithGeneratedName(sourceDef);
				def.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(sourceName));

				parserContext.registerBeanComponent(new BeanComponentDefinition(sourceDef, sourceName));
				parserContext.registerBeanComponent(new BeanComponentDefinition(def, CacheManagementConfigUtils.JCACHE_ASPECT_BEAN_NAME));
			}
		}

		private static RootBeanDefinition createJCacheOperationSourceBeanDefinition(Element element, @Nullable Object eleSource) {
			RootBeanDefinition sourceDef =
					new RootBeanDefinition("org.springframework.cache.jcache.interceptor.DefaultJCacheOperationSource");
			sourceDef.setSource(eleSource);
			sourceDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// JSR-107 support should create an exception cache resolver with the cache manager
			// and there is no way to set that exception cache resolver from the namespace
			parseCacheResolution(element, sourceDef, true);
			CacheNamespaceHandler.parseKeyGenerator(element, sourceDef);
			return sourceDef;
		}
	}

}
