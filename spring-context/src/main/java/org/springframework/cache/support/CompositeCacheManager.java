/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.cache.support;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * 组合{@link CacheManager}实现，可对委托{@link CacheManager}实例的给定集合进行迭代。
 * <p>
 * 允许将{@link NoOpCacheManager}自动添加到列表的末尾，以便在没有后备存储的情况下处理
 * 缓存声明。否则，任何自定义{@link CacheManager}也可以扮演最后一个委托的角色，为任何
 * 请求的名称延迟创建缓存区域。
 * <p>
 * 注意：该复合管理器委托的常规CacheManager如果不知道指定的缓存名称，则需要从
 * {@link #getCache（String）}返回{@code null}，以便迭代到下一个委托。
 * 但是，大部分{@link CacheManager}实现失败回退回缓存刚请求时的初始化创建；
 * 查看具有固定缓存名称的“静态”模式的特定配置详细信息（如果有）。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @see #setFallbackToNoOpCache
 * @see org.springframework.cache.concurrent.ConcurrentMapCacheManager#setCacheNames
 * @since 3.1
 */
public class CompositeCacheManager implements CacheManager, InitializingBean {

	//管理CacheManager的集合
	private final List<CacheManager> cacheManagers = new ArrayList<>();

	private boolean fallbackToNoOpCache = false;


	/**
	 * 构造一个空CompositeCacheManager，通过{@link #setCacheManagers "cacheManagers"}
	 * 添加代理CacheManagers
	 */
	public CompositeCacheManager() {
	}

	/**
	 * 通过给定的代理CacheManagers搞糟一个CompositeCacheManager
	 *
	 * @param cacheManagers 委托代理的CacheManagers
	 */
	public CompositeCacheManager(CacheManager... cacheManagers) {
		setCacheManagers(Arrays.asList(cacheManagers));
	}


	/**
	 * 指定委托代理的CacheManagers
	 */
	public void setCacheManagers(Collection<CacheManager> cacheManagers) {
		this.cacheManagers.addAll(cacheManagers);
	}

	/**
	 * 指示{@link NoOpCacheManager}是否应该添加到委托队列的最后
	 * 这种情况下，任何通过配置的CacheManages无法处理{@code getCache}操作的的请求将自动
	 * 委托给{@link NoOpCacheManager}处理（因此永远不会返回{@code null}）
	 */
	public void setFallbackToNoOpCache(boolean fallbackToNoOpCache) {
		this.fallbackToNoOpCache = fallbackToNoOpCache;
	}

	//如果fallbackToNoOpCache=true，在bean初始化之后，在管理的CacheManager集合最后添加一个NoOpCacheManager
	@Override
	public void afterPropertiesSet() {
		if (this.fallbackToNoOpCache) {
			this.cacheManagers.add(new NoOpCacheManager());
		}
	}


	@Override
	@Nullable
	public Cache getCache(String name) {
		for (CacheManager cacheManager : this.cacheManagers) {
			Cache cache = cacheManager.getCache(name);
			if (cache != null) {
				return cache;
			}
		}
		return null;
	}

	@Override
	public Collection<String> getCacheNames() {
		Set<String> names = new LinkedHashSet<>();
		for (CacheManager manager : this.cacheManagers) {
			names.addAll(manager.getCacheNames());
		}
		return Collections.unmodifiableSet(names);
	}

}
