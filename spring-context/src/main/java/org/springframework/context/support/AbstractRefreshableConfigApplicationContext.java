/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.support;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link AbstractRefreshableApplicationContext}的子类
 * <p>
 * 扩展添加对指定配置位置的通用处理
 * <p>
 * 作为基于XML的应用程序上下文实现
 * <p>
 * 例如:{@link ClassPathXmlApplicationContext},{@link FileSystemXmlApplicationContext},
 * {@link org.springframework.web.context.support.XmlWebApplicationContext}
 *
 * @author Juergen Hoeller
 * @see #setConfigLocation
 * @see #setConfigLocations
 * @see #getDefaultConfigLocations
 * @since 2.5.2
 */
public abstract class AbstractRefreshableConfigApplicationContext extends AbstractRefreshableApplicationContext
		implements BeanNameAware, InitializingBean {

	/**
	 * 应用程序上下文的配置文件位置
	 */
	@Nullable
	private String[] configLocations;

	private boolean setIdCalled = false;


	/**
	 * Create a new AbstractRefreshableConfigApplicationContext with no parent.
	 */
	public AbstractRefreshableConfigApplicationContext() {
	}

	/**
	 * Create a new AbstractRefreshableConfigApplicationContext with the given parent context.
	 *
	 * @param parent the parent context
	 */
	public AbstractRefreshableConfigApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * 如果初始化参数模式(应用传入配置文件路径位置)
	 * <p>
	 * 如果路径以号，分号或空格分隔，则将其解析为路径数组
	 * <p>
	 * 实际调用{@link AbstractRefreshableConfigApplicationContext#setConfigLocations(String... locations) "setConfigLoacation(String ...)"}
	 *
	 * @param location
	 */
	public void setConfigLocation(String location) {
		setConfigLocations(StringUtils.tokenizeToStringArray(location, CONFIG_LOCATION_DELIMITERS));
	}

	/**
	 * 设置应用上下文中的配置文件位置
	 * <p>
	 * 如果设置为null，则再{@link AbstractRefreshableConfigApplicationContext#getConfigLocations() "getConfigLocations"}中返回默认值
	 *
	 * @param locations
	 */
	// TODO 传入配置的路径根据环境变量解析为实际路径
	public void setConfigLocations(@Nullable String... locations) {
		if (locations != null) {
			Assert.noNullElements(locations, "Config locations must not be null");
			this.configLocations = new String[locations.length];
			for (int i = 0; i < locations.length; i++) {
				this.configLocations[i] = resolvePath(locations[i]).trim();
			}
		} else {
			this.configLocations = null;
		}
	}

	/**
	 * Return an array of resource locations, referring to the XML bean definition
	 * files that this context should be built with. Can also include location
	 * patterns, which will get resolved via a ResourcePatternResolver.
	 * <p>The default implementation returns {@code null}. Subclasses can override
	 * this to provide a set of resource locations to load bean definitions from.
	 *
	 * @return an array of resource locations, or {@code null} if none
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	@Nullable
	protected String[] getConfigLocations() {
		return (this.configLocations != null ? this.configLocations : getDefaultConfigLocations());
	}


	/**
	 * 对于未指定显式配置位置的情况，返回要使用的默认配置位置。
	 * 默认实现返回null，可以子类重写覆盖默认位置
	 *
	 * @return 默认配置位置的数组
	 * @see #setConfigLocations
	 */
	@Nullable
	protected String[] getDefaultConfigLocations() {
		return null;
	}

	/**
	 * Resolve the given path, replacing placeholders with corresponding
	 * environment property values if necessary. Applied to config locations.
	 *
	 * @param path the original file path
	 * @return the resolved file path
	 * @see org.springframework.core.env.Environment#resolveRequiredPlaceholders(String)
	 */
	protected String resolvePath(String path) {
		return getEnvironment().resolveRequiredPlaceholders(path);
	}


	@Override
	public void setId(String id) {
		super.setId(id);
		this.setIdCalled = true;
	}

	/**
	 * Sets the id of this context to the bean name by default,
	 * for cases where the context instance is itself defined as a bean.
	 */
	@Override
	public void setBeanName(String name) {
		if (!this.setIdCalled) {
			super.setId(name);
			setDisplayName("ApplicationContext '" + name + "'");
		}
	}

	/**
	 * Triggers {@link #refresh()} if not refreshed in the concrete context's
	 * constructor already.
	 */
	@Override
	public void afterPropertiesSet() {
		if (!isActive()) {
			refresh();
		}
	}

}
