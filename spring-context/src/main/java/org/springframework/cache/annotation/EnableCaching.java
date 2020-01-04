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

package org.springframework.cache.annotation;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.*;

/**
 * 开启Spring的注解驱动功能，类似Spring XML命名空间的配置。
 * 配合@{@link org.springframework.context.annotation.Configuration Configuration}一起使用：
 * 
 * <pre class="code">
 * @ Configuration
 * @ EnableCaching
 * public class AppConfig {
 *
 *     @ Bean
 *     public MyService myService() {
 *         // configure and return a class having @ Cacheable methods
 *         return new MyService();
 *     }
 *
 *     @ Bean
 *     public CacheManager cacheManager() {
 *         // configure and return an implementation of Spring's CacheManager SPI
 *         SimpleCacheManager cacheManager = new SimpleCacheManager();
 *         cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
 *         return cacheManager;
 *     }
 * }</pre>
 *
 * 和以下Spring XML配置文件相比：
 *
 * <pre class="code">
 * < beans>
 *
 *     < cache:annotation-driven/>;
 *
 *     < bean id="myService" class="com.foo.MyService"/>;
 *
 *     < bean id="cacheManager" class="org.springframework.cache.support.SimpleCacheManager">
 *         < property name="caches">
 *             < set>
 *                 < bean class="org.springframework.cache.concurrent.ConcurrentMapCacheFactoryBean">
 *                     < property name="name" value="default"/>
 *                 < /bean>
 *             < /set>
 *         < /property>
 *     < /bean>
 *
 * < /beans>
 * </pre>
 *
 * 在以上的两种场景中 {@code @EnableCaching} 和 {@code < cache:annotation-driven/>} 都负责注册
 * Spring中注解驱动缓存管理的必要组件，
 * 比如 {@link org.springframework.cache.interceptor.CacheInterceptor CacheInterceptor}
 * 和调用 {@link org.springframework.cache.annotation.Cacheable @Cacheable}
 * 时通过基于代理或AspectJ的增强将拦截器编织到调用堆栈中。
 *
 * 如果存在 JSR-107 API和Spring JCache 实现，则还将注册用于管理标准缓存注解的必要组件。
 * 这会在被 {@code CacheResult}, {@code CachePut}, {@code CacheRemove} 或
 * {@code CacheRemoveAll} 注解的方法被调用时创建基于代理或AspectJ的增强，编制到调用堆栈中。
 *
 * 当框架没有一个合理的默认值可以使用的时候，会创建一个类型为
 * {@link org.springframework.cache.CacheManager CacheManager} 的bean。
 * {@code @EnableCaching} 通过类型 by type 寻找项目中的缓存管理器，而 {@code <cache:annotation-driven>}
 * 则通过名称 named "cacheManager"来寻找。因此缓存管理器的命名就不是很重要了。
 *
 * 对于想要在 {@code @EnableCaching} 和确切的缓存管理器之间建立更直接关系的用户，
 * 可以实现 {@link CachingConfigurer} 回调接口。
 * 注意看一下被 {@code @Override} 标记的方法：
 *
 * <pre class="code">
 * @ Configuration
 * @ EnableCaching
 * public class AppConfig extends CachingConfigurerSupport {
 *
 *     @ Bean
 *     public MyService myService() {
 *         // configure and return a class having @ Cacheable methods
 *         return new MyService();
 *     }
 *
 *     @ Bean
 *     @ Override
 *     public CacheManager cacheManager() {
 *         // configure and return an implementation of Spring's CacheManager SPI
 *         SimpleCacheManager cacheManager = new SimpleCacheManager();
 *         cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
 *         return cacheManager;
 *     }
 *
 *     @ Bean
 *     @ Override
 *     public KeyGenerator keyGenerator() {
 *         // configure and return an implementation of Spring's KeyGenerator SPI
 *         return new MyKeyGenerator();
 *     }
 * }</pre>
 *
 * 这种方式简单可取，因为它是显式的，或者说如果需要区分一个容器中两个 {@code CacheManager} 的时候
 * 是必须这么做的。注意以上例子中 {@code keyGenerator} 方法也被重写。这个方法允许通过
 * Spring's {@link org.springframework.cache.interceptor.KeyGenerator KeyGenerator} SPI
 * 自定义缓存key生成策略。一般情况下， {@code @EnableCaching} 会配置一个默认的
 * {@link org.springframework.cache.interceptor.SimpleKeyGenerator SimpleKeyGenerator}
 * 生成策略，但是实现了 {@code CachingConfigurer} 的话，必须显式地提供一个key生成器。
 * 如果不需要自定义的话，可以返回 {@code null} 或 {@code new SimpleKeyGenerator()}。
 *
 * <p>{@link CachingConfigurer} offers additional customization options: it is recommended
 * to extend from {@link org.springframework.cache.annotation.CachingConfigurerSupport
 * CachingConfigurerSupport} that provides a default implementation for all methods which
 * can be useful if you do not need to customize everything. See {@link CachingConfigurer}
 * Javadoc for further details.
 *
 * <p>The {@link #mode} attribute controls how advice is applied: If the mode is
 * {@link AdviceMode#PROXY} (the default), then the other attributes control the behavior
 * of the proxying. Please note that proxy mode allows for interception of calls through
 * the proxy only; local calls within the same class cannot get intercepted that way.
 *
 * <p>Note that if the {@linkplain #mode} is set to {@link AdviceMode#ASPECTJ}, then the
 * value of the {@link #proxyTargetClass} attribute will be ignored. Note also that in
 * this case the {@code spring-aspects} module JAR must be present on the classpath, with
 * compile-time weaving or load-time weaving applying the aspect to the affected classes.
 * There is no proxy involved in such a scenario; local calls will be intercepted as well.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see CachingConfigurer
 * @see CachingConfigurationSelector
 * @see ProxyCachingConfiguration
 * @see org.springframework.cache.aspectj.AspectJCachingConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CachingConfigurationSelector.class)
public @interface EnableCaching {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies. The default is {@code false}. <strong>
	 * Applicable only if {@link #mode()} is set to {@link AdviceMode#PROXY}</strong>.
	 * <p>Note that setting this attribute to {@code true} will affect <em>all</em>
	 * Spring-managed beans requiring proxying, not just those marked with {@code @Cacheable}.
	 * For example, other beans marked with Spring's {@code @Transactional} annotation will
	 * be upgraded to subclass proxying at the same time. This approach has no negative
	 * impact in practice unless one is explicitly expecting one type of proxy vs another,
	 * e.g. in tests.
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate how caching advice should be applied.
	 * <p><b>The default is {@link AdviceMode#PROXY}.</b>
	 * Please note that proxy mode allows for interception of calls through the proxy
	 * only. Local calls within the same class cannot get intercepted that way;
	 * a caching annotation on such a method within a local call will be ignored
	 * since Spring's interceptor does not even kick in for such a runtime scenario.
	 * For a more advanced mode of interception, consider switching this to
	 * {@link AdviceMode#ASPECTJ}.
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * Indicate the ordering of the execution of the caching advisor
	 * when multiple advices are applied at a specific joinpoint.
	 * <p>The default is {@link Ordered#LOWEST_PRECEDENCE}.
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
