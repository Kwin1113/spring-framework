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

package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * 加载资源的策略接口（例如：classpath或文件系统file system下的资源）
 * 该功能需要{@link org.springframework.context.ApplicationContext}以及扩展的
 * {@link org.springframework.core.io.support.ResourcePatternResolver}支持
 *
 * {@link DefaultResourceLoader}是一个独立的实现类，可以在ApplicationContext外部
 * 使用，也可以由{@link ResourceEditor}使用
 *
 * 当在ApplicationContext中运行时，可以使用特定的上下文资源加载策略从字符串中获取
 * 资源类型和资源数组的Bean属性
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see Resource
 * @see org.springframework.core.io.support.ResourcePatternResolver
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 */
public interface ResourceLoader {

	/** 从类路径"classpath:"加载的伪URL前缀 */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;


	/**
	 * 返回指定资源位置的资源句柄。句柄必须是一个可重用的资源描述符，允许多个
	 * {@link Resource#getInputStream()}调用
	 * <p><ul>
	 * <li>必须支持完整的合法URL，如："file:C:/test.dat"
	 * <li>必须支持类路径的伪URL，如："classpath:test.dat"
	 * <li>应该支持相对文件路径，如："WEB-INF/test.dat".
	 * (该方法特定于实现类，通常有ApplicationContext实现提供)
	 * </ul>
	 * <p>注意，资源句柄并不意味着资源一定存在；你需要调用{@link Resource#exists}
	 * 来检查资源是否存在
	 * @param location 资源路径
	 * @return 相应的资源句柄(never {@code null})
	 * @see #CLASSPATH_URL_PREFIX
	 * @see Resource#exists()
	 * @see Resource#getInputStream()
	 */
	Resource getResource(String location);

	/**
	 * 返回该资源加载器ResourceLoader使用的类加载器ClassLoader
	 * <p>需要直接访问ClassLoader的客户端可以使用ResourceLoader以统一的方式进行操作，
	 * 而不是依赖于线程上下文ClassLoader
	 * @return 类加载器ClassLoader
	 * (only {@code null} if even the system ClassLoader isn't accessible)
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getClassLoader();

}
