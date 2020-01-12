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

package org.springframework.cache.support;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 实现了常见{@link CacheManager}方法的抽象基类。
 * 对于备用缓存不变的'静态'环境很有用。
 * Useful for 'static' environments where the backing caches do not change.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public abstract class AbstractCacheManager implements CacheManager, InitializingBean {

	//存放缓存的集合
	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

	private volatile Set<String> cacheNames = Collections.emptySet();


	// 启动时的前期缓存初始化

	@Override
	public void afterPropertiesSet() {
		initializeCaches();
	}

	/**
	 * 初始化缓存的静态配置。
	 * 启动时通过{@link #afterPropertiesSet()}触发；也能在运行时通过调用再初始化re-initialize。
	 * <p>
	 * 模板方法模式。
	 *
	 * @see #loadCaches()
	 * @since 4.2.2
	 */
	public void initializeCaches() {
		//加载缓存管理器CacheManager里的缓存
		Collection<? extends Cache> caches = loadCaches();

		synchronized (this.cacheMap) {
			//初始化先都置空
			this.cacheNames = Collections.emptySet();
			this.cacheMap.clear();

			Set<String> cacheNames = new LinkedHashSet<>(caches.size());

			//循环填满该CacheManager所管理的缓存
			for (Cache cache : caches) {
				String name = cache.getName();
				this.cacheMap.put(name, decorateCache(cache));
				cacheNames.add(name);
			}
			//最后设置成不可便
			this.cacheNames = Collections.unmodifiableSet(cacheNames);
		}
	}

	/**
	 * 加载该缓存管理器cache manager的初始化缓存
	 * 启动时被{@link #afterPropertiesSet()}调用
	 * 返回的集合可以为空但是不能为{@code null}
	 */
	protected abstract Collection<? extends Cache> loadCaches();


	// 在调用时的缓存懒加载

	@Override
	@Nullable
	public Cache getCache(String name) {
		// 快速检查已存在的缓存
		//在初始化之后的缓存集合Map里按名称查询
		Cache cache = this.cacheMap.get(name);
		if (cache != null) {
			//查询到了直接返回
			return cache;
		}

		// 提供者可能支持缓存按需创建

		Cache missingCache = getMissingCache(name);
		if (missingCache != null) {
			// 注册未命中缓存时完全同步
			synchronized (this.cacheMap) {
				cache = this.cacheMap.get(name);
				if (cache == null) {
					//getMissingCache默认直接返回null，交给子类实现
					cache = decorateCache(missingCache);
					this.cacheMap.put(name, cache);
					//添加一个cache
					updateCacheNames(name);
				}
			}
		}
		return cache;
	}

	@Override
	public Collection<String> getCacheNames() {
		return this.cacheNames;
	}


	// 子类的通用缓存初始化委托

	/**
	 * 检查给定名称的注册缓存
	 * 和{@link #getCache(String)}相比，该方法不会通过{@link #getMissingCache(String)}
	 * 触发延迟创建丢失的缓存。
	 *
	 * @param name 缓存标识符（不能为{@code null}）
	 * @return 关联的缓存实例，如果未找到返回{@code null}
	 * @see #getCache(String)
	 * @see #getMissingCache(String)
	 * @since 4.1
	 */
	@Nullable
	protected final Cache lookupCache(String name) {
		return this.cacheMap.get(name);
	}

	/**
	 * 向该管理器动态注册一个额外的缓存
	 *
	 * @param cache 注册的缓存
	 * @deprecated as of Spring 4.3, in favor of {@link #getMissingCache(String)}
	 */
	@Deprecated
	protected final void addCache(Cache cache) {
		String name = cache.getName();
		synchronized (this.cacheMap) {
			if (this.cacheMap.put(name, decorateCache(cache)) == null) {
				updateCacheNames(name);
			}
		}
	}

	/**
	 * 通过给定的名称更新暴露的{@link #cacheNames}集合。
	 * <p>
	 * 该方法始终在完整的{@link #cacheMap}锁内调用，有效地表现的和
	 * {@code CopyOnWriteArraySet}一样，但是作为一个不可修改的引用暴露。
	 *
	 * @param name 要添加的缓存的名字
	 */
	private void updateCacheNames(String name) {
		Set<String> cacheNames = new LinkedHashSet<>(this.cacheNames.size() + 1);
		cacheNames.addAll(this.cacheNames);
		cacheNames.add(name);
		this.cacheNames = Collections.unmodifiableSet(cacheNames);
	}


	// 可以重写缓存初始化的模板方法

	/**
	 * 如果有必要的话包装一下缓存
	 *
	 * @param cache 添加到该缓存管理器CacheManager的缓存对象
	 * @return 装饰后的缓存对象，或者默认返回原来的对象
	 */
	protected Cache decorateCache(Cache cache) {
		return cache;
	}

	/**
	 * 返回指定名称{@code name}的未命中缓存，缓存不存在或可按需创建时返回{@code null}
	 * 如果本地提供者支持的话可以在运行时懒加载创建缓存
	 * 如果按名称查找未产生任何结果，则{@code AbstractCacheManager}子类将在运行时注册此类缓存。
	 * 返回的缓存将被自动加入缓存管理器。
	 *
	 * @param name 检索的缓存的名字
	 * @return 未命中的缓存，如果缓存不存在或可按需创建时返回{@code null}
	 * @see #getCache(String)
	 * @since 4.1
	 */
	@Nullable
	protected Cache getMissingCache(String name) {
		return null;
	}

}
