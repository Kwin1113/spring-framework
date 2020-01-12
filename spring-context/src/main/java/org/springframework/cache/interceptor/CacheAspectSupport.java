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

package org.springframework.cache.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.util.function.SupplierUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 缓存切面的基础类，例如缓存拦截器{@link CacheInterceptor}或AspectJ切面。
 * <p>
 * 该类使Spring底层缓存基础结构可以轻松地实现一个切面或其他任何切面系统。
 * <p>
 * 子类负责以正确的顺序调用相关方法。
 * <p>
 * 使用了设计模式中的策略模式。{@link CacheOperationSource}用于确定缓存操作，
 * {@link KeyGenerator}用于生成缓存key，{@link CacheResolver}用于解析实际
 * 使用的缓存。
 * <p>
 * 注意：缓存切面是可序列化的，但反序列化之后不执行任何缓存。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 3.1
 */
public abstract class CacheAspectSupport extends AbstractCacheInvoker
		implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Map<CacheOperationCacheKey, CacheOperationMetadata> metadataCache = new ConcurrentHashMap<>(1024);

	private final CacheOperationExpressionEvaluator evaluator = new CacheOperationExpressionEvaluator();

	@Nullable
	private CacheOperationSource cacheOperationSource;

	private SingletonSupplier<KeyGenerator> keyGenerator = SingletonSupplier.of(SimpleKeyGenerator::new);

	@Nullable
	private SingletonSupplier<CacheResolver> cacheResolver;

	@Nullable
	private BeanFactory beanFactory;

	private boolean initialized = false;


	/**
	 * 通过给定的错误处理器，key生成器和缓存处理器/管理器supplier来配置切面，当supplier不可用时，则使用默认配置。
	 *
	 * @since 5.1
	 */
	public void configure(
			@Nullable Supplier<CacheErrorHandler> errorHandler, @Nullable Supplier<KeyGenerator> keyGenerator,
			@Nullable Supplier<CacheResolver> cacheResolver, @Nullable Supplier<CacheManager> cacheManager) {

		this.errorHandler = new SingletonSupplier<>(errorHandler, SimpleCacheErrorHandler::new);
		this.keyGenerator = new SingletonSupplier<>(keyGenerator, SimpleKeyGenerator::new);
		this.cacheResolver = new SingletonSupplier<>(cacheResolver,
				() -> SimpleCacheResolver.of(SupplierUtils.resolve(cacheManager)));
	}


	/**
	 * 设置一个或多个缓存操作源，用来查询缓存参数。如果提供了多个源，则会被整合成一个
	 * {@link CompositeCacheOperationSource}
	 *
	 * @see #setCacheOperationSource
	 */
	public void setCacheOperationSources(CacheOperationSource... cacheOperationSources) {
		Assert.notEmpty(cacheOperationSources, "At least 1 CacheOperationSource needs to be specified");
		this.cacheOperationSource = (cacheOperationSources.length > 1 ?
				new CompositeCacheOperationSource(cacheOperationSources) : cacheOperationSources[0]);
	}

	/**
	 * 设置该缓存切面的缓存操作源CacheOperationSource
	 *
	 * @see #setCacheOperationSources
	 * @since 5.1
	 */
	public void setCacheOperationSource(@Nullable CacheOperationSource cacheOperationSource) {
		this.cacheOperationSource = cacheOperationSource;
	}

	/**
	 * 返回该缓存切面的缓存操作源CacheOperationSource
	 */
	@Nullable
	public CacheOperationSource getCacheOperationSource() {
		return this.cacheOperationSource;
	}

	/**
	 * 如果该操作没有指定特定的key生成器，设置该缓存切面需要委托的默认{@link KeyGenerator}
	 * 默认为{@link SimpleKeyGenerator}.
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = SingletonSupplier.of(keyGenerator);
	}

	/**
	 * 返回该缓存切面委托的默认{@link KeyGenerator}
	 */
	public KeyGenerator getKeyGenerator() {
		return this.keyGenerator.obtain();
	}

	/**
	 * 如果该操作没有指定特定的缓存处理器，设置该缓存切面需要委托的默认{@link CacheResolver}
	 * 默认处理器通过缓存名称和默认的缓存管理器来处理缓存。
	 *
	 * @see #setCacheManager
	 * @see SimpleCacheResolver
	 */
	public void setCacheResolver(@Nullable CacheResolver cacheResolver) {
		this.cacheResolver = SingletonSupplier.ofNullable(cacheResolver);
	}

	/**
	 * 返回该缓存切面委托的{@link CacheResolver}
	 */
	@Nullable
	public CacheResolver getCacheResolver() {
		return SupplierUtils.resolve(this.cacheResolver);
	}

	/**
	 * 设置创建默认{@link CacheResolver}的{@link CacheManager}
	 * 如果当前存在{@link CacheResolver}，则替代它。
	 *
	 * @see #setCacheResolver
	 * @see SimpleCacheResolver
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheResolver = SingletonSupplier.of(new SimpleCacheResolver(cacheManager));
	}

	/**
	 * 为{@link CacheManager}和其他的服务查询设置{@link BeanFactory}
	 *
	 * @since 4.3
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public void afterPropertiesSet() {
		Assert.state(getCacheOperationSource() != null, "The 'cacheOperationSources' property is required: " +
				"If there are no cacheable methods, then don't use a cache aspect.");
	}

	@Override
	public void afterSingletonsInstantiated() {
		//如果没有设置CacheResolver，则去那CacheMangager
		if (getCacheResolver() == null) {
			// Lazily initialize cache resolver via default cache manager...
			Assert.state(this.beanFactory != null, "CacheResolver or BeanFactory must be set on cache aspect");
			try {
				setCacheManager(this.beanFactory.getBean(CacheManager.class));
			} catch (NoUniqueBeanDefinitionException ex) {
				throw new IllegalStateException("No CacheResolver specified, and no unique bean of type " +
						"CacheManager found. Mark one as primary or declare a specific CacheManager to use.");
			} catch (NoSuchBeanDefinitionException ex) {
				throw new IllegalStateException("No CacheResolver specified, and no bean of type CacheManager found. " +
						"Register a CacheManager bean or remove the @EnableCaching annotation from your configuration.");
			}
		}
		this.initialized = true;
	}


	/**
	 * 返回用于日志记录的该方法String表达形式的便捷方法。子类可以重写，实现特定方法的不同返回。
	 *
	 * @param method      感兴趣的方法
	 * @param targetClass 方法所在类
	 * @return 记录此方法的日志信息
	 * @see org.springframework.util.ClassUtils#getQualifiedMethodName
	 */
	protected String methodIdentification(Method method, Class<?> targetClass) {
		Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
		return ClassUtils.getQualifiedMethodName(specificMethod);
	}

	protected Collection<? extends Cache> getCaches(
			CacheOperationInvocationContext<CacheOperation> context, CacheResolver cacheResolver) {

		Collection<? extends Cache> caches = cacheResolver.resolveCaches(context);
		if (caches.isEmpty()) {
			throw new IllegalStateException("No cache could be resolved for '" +
					context.getOperation() + "' using resolver '" + cacheResolver +
					"'. At least one cache should be provided per cache operation.");
		}
		return caches;
	}

	protected CacheOperationContext getOperationContext(
			CacheOperation operation, Method method, Object[] args, Object target, Class<?> targetClass) {
		//用入参生成元数据
		CacheOperationMetadata metadata = getCacheOperationMetadata(operation, method, targetClass);
		//再生成上下文
		return new CacheOperationContext(metadata, args, target);
	}

	/**
	 * 返回特定操作的{@link CacheOperationMetadata}。处理缓存中的{@link CacheResolver}
	 * 和{@link KeyGenerator}
	 *
	 * @param operation   操作
	 * @param method      调用操作的方法
	 * @param targetClass 目标类型
	 * @return 该操作处理的元数据
	 */
	protected CacheOperationMetadata getCacheOperationMetadata(
			CacheOperation operation, Method method, Class<?> targetClass) {

		CacheOperationCacheKey cacheKey = new CacheOperationCacheKey(operation, method, targetClass);
		CacheOperationMetadata metadata = this.metadataCache.get(cacheKey);
		if (metadata == null) {
			//从缓存操作的配置中获取KeyGenerator
			KeyGenerator operationKeyGenerator;
			if (StringUtils.hasText(operation.getKeyGenerator())) {
				operationKeyGenerator = getBean(operation.getKeyGenerator(), KeyGenerator.class);
			} else {
				operationKeyGenerator = getKeyGenerator();
			}
			//从缓存操作的配置中获取CacheResolver
			CacheResolver operationCacheResolver;
			if (StringUtils.hasText(operation.getCacheResolver())) {
				operationCacheResolver = getBean(operation.getCacheResolver(), CacheResolver.class);
			} else if (StringUtils.hasText(operation.getCacheManager())) {
				//没有的话获取CacheManager
				CacheManager cacheManager = getBean(operation.getCacheManager(), CacheManager.class);
				operationCacheResolver = new SimpleCacheResolver(cacheManager);
			} else {
				//再没有就使用默认
				operationCacheResolver = getCacheResolver();
				Assert.state(operationCacheResolver != null, "No CacheResolver/CacheManager set");
			}

			//东西都取出来之后封装成metadata
			metadata = new CacheOperationMetadata(operation, method, targetClass,
					operationKeyGenerator, operationCacheResolver);
			this.metadataCache.put(cacheKey, metadata);
		}
		return metadata;
	}

	/**
	 * 返回指定名称和类型的bean。用于处理{@link CacheOperation}中通过名称引用的服务
	 *
	 * @param beanName     操作定义的bean名称
	 * @param expectedType bean的类型
	 * @return 匹配该名称的bean
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if such bean does not exist
	 * @see CacheOperation#getKeyGenerator()
	 * @see CacheOperation#getCacheManager()
	 * @see CacheOperation#getCacheResolver()
	 */
	protected <T> T getBean(String beanName, Class<T> expectedType) {
		if (this.beanFactory == null) {
			throw new IllegalStateException(
					"BeanFactory must be set on cache aspect for " + expectedType.getSimpleName() + " retrieval");
		}
		return BeanFactoryAnnotationUtils.qualifiedBeanOfType(this.beanFactory, expectedType, beanName);
	}

	/**
	 * 清空缓存元数据
	 */
	protected void clearMetadataCache() {
		this.metadataCache.clear();
		this.evaluator.clear();
	}

	/**
	 * TODO 核心方法
	 * 执行目标方法 + 缓存操作
	 *
	 * @param invoker
	 * @param target
	 * @param method
	 * @param args
	 * @return
	 */
	@Nullable
	protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
		// Check whether aspect is enabled (to cope with cases where the AJ is pulled in automatically)
		//已经初始化CacheResolver了
		if (this.initialized) {
			//拿到目标原始Class
			Class<?> targetClass = getTargetClass(target);
			CacheOperationSource cacheOperationSource = getCacheOperationSource();
			if (cacheOperationSource != null) {
				Collection<CacheOperation> operations = cacheOperationSource.getCacheOperations(method, targetClass);
				if (!CollectionUtils.isEmpty(operations)) {
					//将缓存操作进行执行
					return execute(invoker, method,
							new CacheOperationContexts(operations, method, args, target, targetClass));
				}
			}
		}

		return invoker.invoke();
	}

	/**
	 * 执行底层操作（通常在缓存未命中的情况下），返回调用的结果。如果发生了异常，将会被包装在
	 * {@link CacheOperationInvoker.ThrowableWrapper}里：异常可以被处理或修改，但是
	 * 必须被{@link CacheOperationInvoker.ThrowableWrapper}包装。
	 *
	 * @param invoker 处理缓存操作的调用者
	 * @return 调用的结果
	 * @see CacheOperationInvoker#invoke()
	 */
	protected Object invokeOperation(CacheOperationInvoker invoker) {
		return invoker.invoke();
	}

	private Class<?> getTargetClass(Object target) {
		return AopProxyUtils.ultimateTargetClass(target);
	}

	@Nullable
	private Object execute(final CacheOperationInvoker invoker, Method method, CacheOperationContexts contexts) {
		// Special handling of synchronized invocation
		//如果需要同步执行
		if (contexts.isSynchronized()) {
			CacheOperationContext context = contexts.get(CacheableOperation.class).iterator().next();
			if (isConditionPassing(context, CacheOperationExpressionEvaluator.NO_RESULT)) {
				Object key = generateKey(context, CacheOperationExpressionEvaluator.NO_RESULT);
				Cache cache = context.getCaches().iterator().next();
				try {
					return wrapCacheValue(method, cache.get(key, () -> unwrapReturnValue(invokeOperation(invoker))));
				} catch (Cache.ValueRetrievalException ex) {
					// The invoker wraps any Throwable in a ThrowableWrapper instance so we
					// can just make sure that one bubbles up the stack.
					throw (CacheOperationInvoker.ThrowableWrapper) ex.getCause();
				}
			} else {
				// No caching required, only call the underlying method
				return invokeOperation(invoker);
			}
		}

		//不需要同步

		// Process any early evictions
		//先处理@CacheEvict注解
		processCacheEvicts(contexts.get(CacheEvictOperation.class), true,
				CacheOperationExpressionEvaluator.NO_RESULT);

		// Check if we have a cached item matching the conditions
		//查看@Cacheable是否命中缓存
		Cache.ValueWrapper cacheHit = findCachedItem(contexts.get(CacheableOperation.class));

		// Collect puts from any @Cacheable miss, if no cached item is found
		//如果缓存未命中，则准备一个CachePutRequest集合
		List<CachePutRequest> cachePutRequests = new LinkedList<>();
		if (cacheHit == null) {
			collectPutRequests(contexts.get(CacheableOperation.class),
					CacheOperationExpressionEvaluator.NO_RESULT, cachePutRequests);
		}

		Object cacheValue;
		Object returnValue;

		if (cacheHit != null && !hasCachePut(contexts)) {
			// If there are no put requests, just use the cache hit
			//缓存命中，并且上下文集合中没有其他的CachePut操作，返回值直接用命中的缓存值
			cacheValue = cacheHit.get();
			returnValue = wrapCacheValue(method, cacheValue);
		} else {
			// Invoke the method if we don't have a cache hit
			//如果缓存未命中，调用方法
			returnValue = invokeOperation(invoker);
			cacheValue = unwrapReturnValue(returnValue);
		}

		// Collect any explicit @CachePuts
		//收集所有明确声明的@CachePut
		collectPutRequests(contexts.get(CachePutOperation.class), cacheValue, cachePutRequests);

		// Process any collected put requests, either from @CachePut or a @Cacheable miss
		//执行所有CachePut操作，不管是未命中的@Cacheable或者@CachePut
		for (CachePutRequest cachePutRequest : cachePutRequests) {
			cachePutRequest.apply(cacheValue);
		}

		// Process any late evictions
		//执行方法之后的缓存移除操作
		processCacheEvicts(contexts.get(CacheEvictOperation.class), false, cacheValue);

		return returnValue;
	}

	@Nullable
	private Object wrapCacheValue(Method method, @Nullable Object cacheValue) {
		if (method.getReturnType() == Optional.class &&
				(cacheValue == null || cacheValue.getClass() != Optional.class)) {
			return Optional.ofNullable(cacheValue);
		}
		return cacheValue;
	}

	@Nullable
	private Object unwrapReturnValue(Object returnValue) {
		return ObjectUtils.unwrapOptional(returnValue);
	}

	/**
	 * 上下文集合中是否含有CachePut的上下文
	 * @param contexts 上下文集合
	 * @return 是否含有CachePut上下文
	 */
	private boolean hasCachePut(CacheOperationContexts contexts) {
		// Evaluate the conditions *without* the result object because we don't have it yet...
		//取出所有CachePut操作
		Collection<CacheOperationContext> cachePutContexts = contexts.get(CachePutOperation.class);
		//该集合用于之后判断不满足condition条件的CachePut操作
		Collection<CacheOperationContext> excluded = new ArrayList<>();
		for (CacheOperationContext context : cachePutContexts) {
			try {
				//不满足condition条件的CachePut操作放到excluded集合里
				if (!context.isConditionPassing(CacheOperationExpressionEvaluator.RESULT_UNAVAILABLE)) {
					excluded.add(context);
				}
			} catch (VariableNotAvailableException ex) {
				// Ignoring failure due to missing result, consider the cache put has to proceed
			}
		}
		// Check if all puts have been excluded by condition
		//只要两个集合数量不相等，说明有需要处理的CachePut操作（cachePutContexts集合一定是大于excluded集合的）
		return (cachePutContexts.size() != excluded.size());
	}

	private void processCacheEvicts(
			Collection<CacheOperationContext> contexts, boolean beforeInvocation, @Nullable Object result) {
		//循环处理移除操作
		for (CacheOperationContext context : contexts) {
			//因为是按照CacheEvictOperation取出来的，所以可以直接转型成CacheEvictOperation操作
			CacheEvictOperation operation = (CacheEvictOperation) context.metadata.operation;

			//如果需要在方法调用之前执行移除并且condition条件满足
			if (beforeInvocation == operation.isBeforeInvocation() && isConditionPassing(context, result)) {
				performCacheEvict(context, operation, result);
			}
		}
	}

	private void performCacheEvict(
			CacheOperationContext context, CacheEvictOperation operation, @Nullable Object result) {
		//从上下文中获取所有缓存，循环进行操作
		Object key = null;
		for (Cache cache : context.getCaches()) {
			if (operation.isCacheWide()) {
				//如果是清除整个缓存
				logInvalidating(context, operation, null);
				doClear(cache, operation.isBeforeInvocation());
			} else {
				//否则清除一个key
				if (key == null) {
					key = generateKey(context, result);
				}
				logInvalidating(context, operation, key);
				//因为是在其他操作之前执行的，所以需要马上移除缓存，避免影响后面的缓存操作
				doEvict(cache, key, operation.isBeforeInvocation());
			}
		}
	}

	private void logInvalidating(CacheOperationContext context, CacheEvictOperation operation, @Nullable Object key) {
		if (logger.isTraceEnabled()) {
			logger.trace("Invalidating " + (key != null ? "cache key [" + key + "]" : "entire cache") +
					" for operation " + operation + " on method " + context.metadata.method);
		}
	}

	/**
	 * 查询满足条件的{@link CacheableOperation}缓存
	 *
	 * @param contexts 可缓存操作
	 * @return 持有缓存项的{@link Cache.ValueWrapper}，如果没有找到则返回{@code null}
	 */
	@Nullable
	private Cache.ValueWrapper findCachedItem(Collection<CacheOperationContext> contexts) {
		Object result = CacheOperationExpressionEvaluator.NO_RESULT;
		for (CacheOperationContext context : contexts) {
			if (isConditionPassing(context, result)) {
				//循环满足condition条件的上下文，生成key
				Object key = generateKey(context, result);
				//在缓存中查询该key对应的缓存
				Cache.ValueWrapper cached = findInCaches(context, key);
				if (cached != null) {
					return cached;
				} else {
					if (logger.isTraceEnabled()) {
						logger.trace("No cache entry for key '" + key + "' in cache(s) " + context.getCacheNames());
					}
				}
			}
		}
		return null;
	}

	/**
	 * 为所有使用指定结果项的{@link CacheOperation}收集{@link CachePutRequest}
	 *
	 * @param contexts    处理的上下文
	 * @param result      结果项（永不为{@code null}）
	 * @param putRequests 更新的结果集
	 */
	private void collectPutRequests(Collection<CacheOperationContext> contexts,
									@Nullable Object result, Collection<CachePutRequest> putRequests) {
		for (CacheOperationContext context : contexts) {
			if (isConditionPassing(context, result)) {
				//循环满足条件的缓存操作上下文，并生成一个CachePutRequest放入集合
				Object key = generateKey(context, result);
				putRequests.add(new CachePutRequest(context, key));
			}
		}
	}

	@Nullable
	private Cache.ValueWrapper findInCaches(CacheOperationContext context, Object key) {
		for (Cache cache : context.getCaches()) {
			//去除上下文中的所有cache，做doGet操作
			Cache.ValueWrapper wrapper = doGet(cache, key);
			if (wrapper != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Cache entry for key '" + key + "' found in cache '" + cache.getName() + "'");
				}
				return wrapper;
			}
		}
		return null;
	}

	private boolean isConditionPassing(CacheOperationContext context, @Nullable Object result) {
		boolean passing = context.isConditionPassing(result);
		if (!passing && logger.isTraceEnabled()) {
			logger.trace("Cache condition failed on method " + context.metadata.method +
					" for operation " + context.metadata.operation);
		}
		return passing;
	}

	private Object generateKey(CacheOperationContext context, @Nullable Object result) {
		Object key = context.generateKey(result);
		if (key == null) {
			throw new IllegalArgumentException("Null key returned for cache operation (maybe you are " +
					"using named params on classes without debug info?) " + context.metadata.operation);
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Computed cache key '" + key + "' for operation " + context.metadata.operation);
		}
		return key;
	}


	/**
	 * 缓存操作的上下文集合，比如说一个类或方法上有多个注解
	 */
	private class CacheOperationContexts {

		//key是CacheOperation的那几个子类类型
		private final MultiValueMap<Class<? extends CacheOperation>, CacheOperationContext> contexts;

		//是否需要同步执行，默认false
		private final boolean sync;

		public CacheOperationContexts(Collection<? extends CacheOperation> operations, Method method,
									  Object[] args, Object target, Class<?> targetClass) {

			this.contexts = new LinkedMultiValueMap<>(operations.size());
			for (CacheOperation op : operations) {
				this.contexts.add(op.getClass(), getOperationContext(op, method, args, target, targetClass));
			}
			//直接通过给定参数算出来能不能同步
			this.sync = determineSyncFlag(method);
		}

		//获取某个类型的缓存操作（一般来说就是那三大类）
		public Collection<CacheOperationContext> get(Class<? extends CacheOperation> operationClass) {
			Collection<CacheOperationContext> result = this.contexts.get(operationClass);
			//不存在则返回空集合
			return (result != null ? result : Collections.emptyList());
		}

		public boolean isSynchronized() {
			return this.sync;
		}

		//计算sync
		private boolean determineSyncFlag(Method method) {
			//获取该缓存上下文集合中的所有Cacheable操作，因为只有这个操作才有sync属性
			List<CacheOperationContext> cacheOperationContexts = this.contexts.get(CacheableOperation.class);
			if (cacheOperationContexts == null) {  // no @Cacheable operation at all 根本没有缓存操作
				return false;
			}
			boolean syncEnabled = false;
			//只要有一个Cacheable操作需要同步就跳出
			for (CacheOperationContext cacheOperationContext : cacheOperationContexts) {
				if (((CacheableOperation) cacheOperationContext.getOperation()).isSync()) {
					syncEnabled = true;
					break;
				}
			}
			//需要同步
			if (syncEnabled) {
				//但是如果有多个操作上下文还需要同步就不行，直接报错
				if (this.contexts.size() > 1) {
					throw new IllegalStateException(
							"@Cacheable(sync=true) cannot be combined with other cache operations on '" + method + "'");
				}
				//只能有一个Cacheable操作的情况下sync才能生效，直接报错
				if (cacheOperationContexts.size() > 1) {
					throw new IllegalStateException(
							"Only one @Cacheable(sync=true) entry is allowed on '" + method + "'");
				}
				//筛选下来只有一个Cacheable操作
				CacheOperationContext cacheOperationContext = cacheOperationContexts.iterator().next();
				CacheableOperation operation = (CacheableOperation) cacheOperationContext.getOperation();
				//如果设置了sync=true，不能设置多个缓存，直接报错
				if (cacheOperationContext.getCaches().size() > 1) {
					throw new IllegalStateException(
							"@Cacheable(sync=true) only allows a single cache on '" + operation + "'");
				}
				//sync=true时不支持unless属性，直接报错
				if (StringUtils.hasText(operation.getUnless())) {
					throw new IllegalStateException(
							"@Cacheable(sync=true) does not support unless attribute on '" + operation + "'");
				}
				return true;
			}
			return false;
		}
	}


	/**
	 * 不依赖于特定调用的缓存操作的元数据，这使它成为缓存的良好候选对象。
	 */
	protected static class CacheOperationMetadata {

		private final CacheOperation operation;

		private final Method method;

		private final Class<?> targetClass;

		private final Method targetMethod;

		private final AnnotatedElementKey methodKey;

		private final KeyGenerator keyGenerator;

		private final CacheResolver cacheResolver;

		public CacheOperationMetadata(CacheOperation operation, Method method, Class<?> targetClass,
									  KeyGenerator keyGenerator, CacheResolver cacheResolver) {

			this.operation = operation;
			this.method = BridgeMethodResolver.findBridgedMethod(method);
			this.targetClass = targetClass;
			this.targetMethod = (!Proxy.isProxyClass(targetClass) ?
					AopUtils.getMostSpecificMethod(method, targetClass) : this.method);
			this.methodKey = new AnnotatedElementKey(this.targetMethod, targetClass);
			this.keyGenerator = keyGenerator;
			this.cacheResolver = cacheResolver;
		}
	}


	/**
	 * {@link CacheOperation}的{@link CacheOperationInvocationContext}上下文
	 */
	protected class CacheOperationContext implements CacheOperationInvocationContext<CacheOperation> {

		private final CacheOperationMetadata metadata;

		private final Object[] args;

		private final Object target;

		private final Collection<? extends Cache> caches;

		private final Collection<String> cacheNames;

		@Nullable
		private Boolean conditionPassing;

		public CacheOperationContext(CacheOperationMetadata metadata, Object[] args, Object target) {
			this.metadata = metadata;
			this.args = extractArgs(metadata.method, args);
			this.target = target;
			this.caches = CacheAspectSupport.this.getCaches(this, metadata.cacheResolver);
			this.cacheNames = createCacheNames(this.caches);
		}

		@Override
		public CacheOperation getOperation() {
			return this.metadata.operation;
		}

		@Override
		public Object getTarget() {
			return this.target;
		}

		@Override
		public Method getMethod() {
			return this.metadata.method;
		}

		@Override
		public Object[] getArgs() {
			return this.args;
		}

		private Object[] extractArgs(Method method, Object[] args) {
			if (!method.isVarArgs()) {
				return args;
			}
			Object[] varArgs = ObjectUtils.toObjectArray(args[args.length - 1]);
			Object[] combinedArgs = new Object[args.length - 1 + varArgs.length];
			System.arraycopy(args, 0, combinedArgs, 0, args.length - 1);
			System.arraycopy(varArgs, 0, combinedArgs, args.length - 1, varArgs.length);
			return combinedArgs;
		}

		protected boolean isConditionPassing(@Nullable Object result) {
			if (this.conditionPassing == null) {
				if (StringUtils.hasText(this.metadata.operation.getCondition())) {
					EvaluationContext evaluationContext = createEvaluationContext(result);
					this.conditionPassing = evaluator.condition(this.metadata.operation.getCondition(),
							this.metadata.methodKey, evaluationContext);
				} else {
					this.conditionPassing = true;
				}
			}
			return this.conditionPassing;
		}

		protected boolean canPutToCache(@Nullable Object value) {
			String unless = "";
			if (this.metadata.operation instanceof CacheableOperation) {
				unless = ((CacheableOperation) this.metadata.operation).getUnless();
			} else if (this.metadata.operation instanceof CachePutOperation) {
				unless = ((CachePutOperation) this.metadata.operation).getUnless();
			}
			if (StringUtils.hasText(unless)) {
				EvaluationContext evaluationContext = createEvaluationContext(value);
				return !evaluator.unless(unless, this.metadata.methodKey, evaluationContext);
			}
			return true;
		}

		/**
		 * 计算给定缓存操作的key
		 */
		@Nullable
		protected Object generateKey(@Nullable Object result) {
			if (StringUtils.hasText(this.metadata.operation.getKey())) {
				EvaluationContext evaluationContext = createEvaluationContext(result);
				return evaluator.key(this.metadata.operation.getKey(), this.metadata.methodKey, evaluationContext);
			}
			return this.metadata.keyGenerator.generate(this.target, this.metadata.method, this.args);
		}

		private EvaluationContext createEvaluationContext(@Nullable Object result) {
			return evaluator.createEvaluationContext(this.caches, this.metadata.method, this.args,
					this.target, this.metadata.targetClass, this.metadata.targetMethod, result, beanFactory);
		}

		protected Collection<? extends Cache> getCaches() {
			return this.caches;
		}

		protected Collection<String> getCacheNames() {
			return this.cacheNames;
		}

		private Collection<String> createCacheNames(Collection<? extends Cache> caches) {
			Collection<String> names = new ArrayList<>();
			for (Cache cache : caches) {
				names.add(cache.getName());
			}
			return names;
		}
	}


	private class CachePutRequest {

		private final CacheOperationContext context;

		private final Object key;

		public CachePutRequest(CacheOperationContext context, Object key) {
			this.context = context;
			this.key = key;
		}

		public void apply(@Nullable Object result) {
			if (this.context.canPutToCache(result)) {
				//判断一下unless条件再执行doPut操作
				for (Cache cache : this.context.getCaches()) {
					doPut(cache, this.key, result);
				}
			}
		}
	}


	private static final class CacheOperationCacheKey implements Comparable<CacheOperationCacheKey> {

		private final CacheOperation cacheOperation;

		private final AnnotatedElementKey methodCacheKey;

		private CacheOperationCacheKey(CacheOperation cacheOperation, Method method, Class<?> targetClass) {
			this.cacheOperation = cacheOperation;
			this.methodCacheKey = new AnnotatedElementKey(method, targetClass);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof CacheOperationCacheKey)) {
				return false;
			}
			CacheOperationCacheKey otherKey = (CacheOperationCacheKey) other;
			return (this.cacheOperation.equals(otherKey.cacheOperation) &&
					this.methodCacheKey.equals(otherKey.methodCacheKey));
		}

		@Override
		public int hashCode() {
			return (this.cacheOperation.hashCode() * 31 + this.methodCacheKey.hashCode());
		}

		@Override
		public String toString() {
			return this.cacheOperation + " on " + this.methodCacheKey;
		}

		@Override
		public int compareTo(CacheOperationCacheKey other) {
			int result = this.cacheOperation.getName().compareTo(other.cacheOperation.getName());
			if (result == 0) {
				result = this.methodCacheKey.compareTo(other.methodCacheKey);
			}
			return result;
		}
	}

}
