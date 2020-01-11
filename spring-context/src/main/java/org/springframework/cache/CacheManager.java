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

package org.springframework.cache;

import java.util.Collection;

import org.springframework.lang.Nullable;

/**
 * Spring的主要缓存管理SPI
 *
 * 用来检索命名缓存空间named {@link Cache} regions.。
 *
 * @author Costin Leau
 * @author Sam Brannen
 * @since 3.1
 */
public interface CacheManager {

	/**
	 * 获取给定名称对应的缓存。
	 * 注意：如果本地提供者支持的话，缓存可能在运行时进行懒加载。
	 *
	 * @param name 缓存标识符（不能为{@code null}）
	 * @return 对应的缓存，如果不存在该缓存或无法被创建则返回{@code null}
	 */
	@Nullable
	Cache getCache(String name);

	/**
	 * 返回该缓存管理器已知的缓存名称集合。
	 * @return 该缓存管理器一直的所有缓存名称集合
	 */
	Collection<String> getCacheNames();

}
