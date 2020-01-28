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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CachePutOperation;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.cache.interceptor.NameMatchCacheOperationSource;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser
 * BeanDefinitionParser} 用来解析 {@code <tx:advice/>} 标签。
 *
 * 扩展父类以支持解析复杂的自定义XML元素。
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class CacheAdviceParser extends AbstractSingleBeanDefinitionParser {

	//cacheable元素
	private static final String CACHEABLE_ELEMENT = "cacheable";
	//cache-evict元素
	private static final String CACHE_EVICT_ELEMENT = "cache-evict";
	//cache-put元素
	private static final String CACHE_PUT_ELEMENT = "cache-put";
	//方法参数
	private static final String METHOD_ATTRIBUTE = "method";
	//caching元素
	private static final String DEFS_ELEMENT = "caching";


	@Override
	protected Class<?> getBeanClass(Element element) {
		return CacheInterceptor.class;
	}

	/**
	 * 先解析XML文件中的参数bean定义，如果不存在，则以
	 * {@link org.springframework.cache.annotation.AnnotationCacheOperationSource}
	 * 加载bean定义。因此是XML属性优先于缓存注解定义
	 */
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		//获取cacheManager标签
		builder.addPropertyReference("cacheManager", CacheNamespaceHandler.extractCacheManager(element));
		//往builder里添加keyGenerator
		CacheNamespaceHandler.parseKeyGenerator(element, builder.getBeanDefinition());

		//解析caching标签里的子标签
		List<Element> cacheDefs = DomUtils.getChildElementsByTagName(element, DEFS_ELEMENT);
		if (!cacheDefs.isEmpty()) {
			// Using attributes source.
			//解析所有缓存操作，作为ManagedList返回
			List<RootBeanDefinition> attributeSourceDefinitions = parseDefinitionsSources(cacheDefs, parserContext);
			builder.addPropertyValue("cacheOperationSources", attributeSourceDefinitions);
		}
		else {
			// Assume annotations source.
			builder.addPropertyValue("cacheOperationSources",
					new RootBeanDefinition("org.springframework.cache.annotation.AnnotationCacheOperationSource"));
		}
	}

	/**
	 * 从{@code definitions}集合中解析所有缓存操作，并作为ManagedList集合返回
	 */
	private List<RootBeanDefinition> parseDefinitionsSources(List<Element> definitions, ParserContext parserContext) {
		ManagedList<RootBeanDefinition> defs = new ManagedList<>(definitions.size());

		// extract default param for the definition
		for (Element element : definitions) {
			defs.add(parseDefinitionSource(element, parserContext));
		}

		return defs;
	}

	/**
	 * 从{@code definition}中解析出缓存操作，并作为nameMap属性返回一个bean根定义
	 */
	private RootBeanDefinition parseDefinitionSource(Element definition, ParserContext parserContext) {
		//从definition中取出一些默认属性
		Props prop = new Props(definition);
		// add cacheable first
		//获取cacheable

		ManagedMap<TypedStringValue, Collection<CacheOperation>> cacheOpMap = new ManagedMap<>();
		cacheOpMap.setSource(parserContext.extractSource(definition));

		//获取cacheable的子元素
		List<Element> cacheableCacheMethods = DomUtils.getChildElementsByTagName(definition, CACHEABLE_ELEMENT);

		//解析出来所有cacheable操作
		for (Element opElement : cacheableCacheMethods) {
			String name = prop.merge(opElement, parserContext.getReaderContext());
			TypedStringValue nameHolder = new TypedStringValue(name);
			nameHolder.setSource(parserContext.extractSource(opElement));
			CacheableOperation.Builder builder = prop.merge(opElement,
					parserContext.getReaderContext(), new CacheableOperation.Builder());
			builder.setUnless(getAttributeValue(opElement, "unless", ""));
			builder.setSync(Boolean.parseBoolean(getAttributeValue(opElement, "sync", "false")));

			//从缓存操作Map里获取nameHolder
			Collection<CacheOperation> col = cacheOpMap.get(nameHolder);
			if (col == null) {
				col = new ArrayList<>(2);
				cacheOpMap.put(nameHolder, col);
			}
			col.add(builder.build());
		}

		//获取cacheevict

		//获取cacheevict的子元素
		List<Element> evictCacheMethods = DomUtils.getChildElementsByTagName(definition, CACHE_EVICT_ELEMENT);

		//解析出来所有cacheevict操作
		for (Element opElement : evictCacheMethods) {
			String name = prop.merge(opElement, parserContext.getReaderContext());
			TypedStringValue nameHolder = new TypedStringValue(name);
			nameHolder.setSource(parserContext.extractSource(opElement));
			CacheEvictOperation.Builder builder = prop.merge(opElement,
					parserContext.getReaderContext(), new CacheEvictOperation.Builder());

			String wide = opElement.getAttribute("all-entries");
			if (StringUtils.hasText(wide)) {
				builder.setCacheWide(Boolean.parseBoolean(wide.trim()));
			}

			String after = opElement.getAttribute("before-invocation");
			if (StringUtils.hasText(after)) {
				builder.setBeforeInvocation(Boolean.parseBoolean(after.trim()));
			}

			//同上
			Collection<CacheOperation> col = cacheOpMap.get(nameHolder);
			if (col == null) {
				col = new ArrayList<>(2);
				cacheOpMap.put(nameHolder, col);
			}
			col.add(builder.build());
		}

		//获取cacheput

		//获取cacheput的子元素
		List<Element> putCacheMethods = DomUtils.getChildElementsByTagName(definition, CACHE_PUT_ELEMENT);

		//解析所有cacheout操作
		for (Element opElement : putCacheMethods) {
			String name = prop.merge(opElement, parserContext.getReaderContext());
			TypedStringValue nameHolder = new TypedStringValue(name);
			nameHolder.setSource(parserContext.extractSource(opElement));
			CachePutOperation.Builder builder = prop.merge(opElement,
					parserContext.getReaderContext(), new CachePutOperation.Builder());
			builder.setUnless(getAttributeValue(opElement, "unless", ""));

			//同上
			Collection<CacheOperation> col = cacheOpMap.get(nameHolder);
			if (col == null) {
				col = new ArrayList<>(2);
				cacheOpMap.put(nameHolder, col);
			}
			col.add(builder.build());
		}

		//定义一个bean根定义，带有一个nameMap参数
		RootBeanDefinition attributeSourceDefinition = new RootBeanDefinition(NameMatchCacheOperationSource.class);
		attributeSourceDefinition.setSource(parserContext.extractSource(definition));
		attributeSourceDefinition.getPropertyValues().add("nameMap", cacheOpMap);
		return attributeSourceDefinition;
	}


	private static String getAttributeValue(Element element, String attributeName, String defaultValue) {
		String attribute = element.getAttribute(attributeName);
		if (StringUtils.hasText(attribute)) {
			return attribute.trim();
		}
		return defaultValue;
	}


	/**
	 * 用于重载默认值的简单可重用的类。
	 */
	private static class Props {

		private String key;

		private String keyGenerator;

		private String cacheManager;

		private String condition;

		private String method;

		@Nullable
		private String[] caches;

		//构造参数从root元素中提取默认名称的元素
		Props(Element root) {
			String defaultCache = root.getAttribute("cache");
			this.key = root.getAttribute("key");
			this.keyGenerator = root.getAttribute("key-generator");
			this.cacheManager = root.getAttribute("cache-manager");
			this.condition = root.getAttribute("condition");
			this.method = root.getAttribute(METHOD_ATTRIBUTE);

			if (StringUtils.hasText(defaultCache)) {
				this.caches = StringUtils.commaDelimitedListToStringArray(defaultCache.trim());
			}
		}

		<T extends CacheOperation.Builder> T merge(Element element, ReaderContext readerCtx, T builder) {
			String cache = element.getAttribute("cache");

			// sanity check
			String[] localCaches = this.caches;
			if (StringUtils.hasText(cache)) {
				localCaches = StringUtils.commaDelimitedListToStringArray(cache.trim());
			}
			if (localCaches != null) {
				builder.setCacheNames(localCaches);
			}
			else {
				readerCtx.error("No cache specified for " + element.getNodeName(), element);
			}

			builder.setKey(getAttributeValue(element, "key", this.key));
			builder.setKeyGenerator(getAttributeValue(element, "key-generator", this.keyGenerator));
			builder.setCacheManager(getAttributeValue(element, "cache-manager", this.cacheManager));
			builder.setCondition(getAttributeValue(element, "condition", this.condition));

			if (StringUtils.hasText(builder.getKey()) && StringUtils.hasText(builder.getKeyGenerator())) {
				throw new IllegalStateException("Invalid cache advice configuration on '" +
						element.toString() + "'. Both 'key' and 'keyGenerator' attributes have been set. " +
						"These attributes are mutually exclusive: either set the SpEL expression used to" +
						"compute the key at runtime or set the name of the KeyGenerator bean to use.");
			}

			return builder;
		}

		@Nullable
		String merge(Element element, ReaderContext readerCtx) {
			String method = element.getAttribute(METHOD_ATTRIBUTE);
			if (StringUtils.hasText(method)) {
				return method.trim();
			}
			if (StringUtils.hasText(this.method)) {
				return this.method;
			}
			readerCtx.error("No method specified for " + element.getNodeName(), element);
			return null;
		}
	}

}
