/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.io;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ResourceLoader}接口的默认实现类
 * 用于{@link ResourceEditor}，并且作为
 * {@link org.springframework.context.support.AbstractApplicationContext}
 * 的基类。
 * 也可以直接单独地使用。
 *
 * <p>如果location值是一个URL，则会返回一个{@link UrlResource}，
 * 如果是一个非URL路径或"classpath:"伪路径，则会返回一个{@link ClassPathResource}
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see FileSystemResourceLoader
 * @see org.springframework.context.support.ClassPathXmlApplicationContext
 */
public class DefaultResourceLoader implements ResourceLoader {

	//该ResourceLoader持有的ClassLoader
	@Nullable
	private ClassLoader classLoader;

	private final Set<ProtocolResolver> protocolResolvers = new LinkedHashSet<>(4);

	private final Map<Class<?>, Map<Resource, ?>> resourceCaches = new ConcurrentHashMap<>(4);


	/**
	 * 创建一个新的DefaultResourceLoader
	 * <p>在此ResourceLoader初始化时，将使用线程上下文类加载器进行ClassLoader赋值。
	 * @see java.lang.Thread#getContextClassLoader()
	 */
	public DefaultResourceLoader() {
		this.classLoader = ClassUtils.getDefaultClassLoader();
	}

	/**
	 * 创建一个新的DefaultResourceLoader
	 * @param classLoader 加载类路径资源的类加载器ClassLoader，或者传参{@code null}
	 * 来指定访问实际资源时调用的线程上下文类加载器
	 */
	public DefaultResourceLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * 指定加载类路径资源的类加载器ClassLoader，或者传参{@code null}
	 * 来指定访问实际资源时调用的线程上下文类加载器
	 * <p>默认的类加载器ClassLoader为资源加载器ResourceLoader初始化时的线程线上文类加载器
	 */
	public void setClassLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * 返回加载类路径资源的类加载器
	 * <p>将会传参给ClassPathResource的构造器，用于该资源加载器创建所有ClassPathResource
	 * @see ClassPathResource
	 */
	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return (this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 向该资源加载器注册给定的处理器，用于处理其他额外的协议。
	 * <p>任何此类处理器都将在此资源加载器的标准解析规则之前调用
	 * 因此，它也可能会覆盖默认规则
	 * @since 4.3
	 * @see #getProtocolResolvers()
	 */
	public void addProtocolResolver(ProtocolResolver resolver) {
		Assert.notNull(resolver, "ProtocolResolver must not be null");
		this.protocolResolvers.add(resolver);
	}

	/**
	 * 返回当前注册的协议处理器，允许用于检查或修改
	 * @since 4.3
	 */
	public Collection<ProtocolResolver> getProtocolResolvers() {
		return this.protocolResolvers;
	}

	/**
	 * 获取给定valueType的缓存，key为{@link Resource}
	 * @param valueType value的类型，比如 ASM {@code MetadataReader}
	 * @return 缓存{@link Map},在{@code ResourceLoader}级别共享
	 * @since 5.0
	 */
	@SuppressWarnings("unchecked")
	public <T> Map<Resource, T> getResourceCache(Class<T> valueType) {
		return (Map<Resource, T>) this.resourceCaches.computeIfAbsent(valueType, key -> new ConcurrentHashMap<>());
	}

	/**
	 * 清除该资源加载器中的所有资源缓存
	 * @since 5.0
	 * @see #getResourceCache
	 */
	public void clearResourceCaches() {
		this.resourceCaches.clear();
	}


	//通过给定的location，获取Resource。该方法在接口中定义
	@Override
	public Resource getResource(String location) {
		Assert.notNull(location, "Location must not be null");

		//先获取所有的ProtocolResolver，在资源解析之前先调用这些处理
		for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
			Resource resource = protocolResolver.resolve(location, this);
			if (resource != null) {
				return resource;
			}
		}

		//绝对路径的资源
		if (location.startsWith("/")) {
			return getResourceByPath(location);
		}
		//类路径的资源
		else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
		}
		//两者都不是
		else {
			try {
				// Try to parse the location as a URL...
				//尝试使用URL的方式解析
				URL url = new URL(location);
				return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
			}
			catch (MalformedURLException ex) {
				// No URL -> resolve as resource path.
				//不是URL -> 按资源路径的方式解析
				return getResourceByPath(location);
			}
		}
	}

	/**
	 * 返回给定路径资源的资源句柄
	 * <p>默认的实现类支持classpath下的资源。这对独立实现但可被重写的实现类是合理的，
	 * 例如：针对Servlet容器的实现类
	 * @param path 资源的路径
	 * @return 相应的资源句柄
	 * @see ClassPathResource
	 * @see org.springframework.context.support.FileSystemXmlApplicationContext#getResourceByPath
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#getResourceByPath
	 */
	protected Resource getResourceByPath(String path) {
		return new ClassPathContextResource(path, getClassLoader());
	}


	/**
	 * 类路径资源ClassPathResource，通过实现ContextResource接口显式地表达了上下文相关context-relative的路径
	 */
	protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

		public ClassPathContextResource(String path, @Nullable ClassLoader classLoader) {
			super(path, classLoader);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}

		@Override
		public Resource createRelative(String relativePath) {
			String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
			return new ClassPathContextResource(pathToUse, getClassLoader());
		}
	}

}
