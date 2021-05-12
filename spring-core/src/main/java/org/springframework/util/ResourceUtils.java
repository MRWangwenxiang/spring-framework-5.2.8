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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.*;

/**
 * Utility methods for resolving resource locations to files in the
 * file system. Mainly for internal use within the framework.
 *
 * <p>Consider using Spring's Resource abstraction in the core package
 * for handling all kinds of file resources in a uniform manner.
 * {@link org.springframework.core.io.ResourceLoader}'s {@code getResource()}
 * method can resolve any location to a {@link org.springframework.core.io.Resource}
 * object, which in turn allows one to obtain a {@code java.io.File} in the
 * file system through its {@code getFile()} method.
 *
 * @author Juergen Hoeller
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ClassPathResource
 * @see org.springframework.core.io.FileSystemResource
 * @see org.springframework.core.io.UrlResource
 * @see org.springframework.core.io.ResourceLoader
 * @since 1.1.5
 */
// REMARK 小技巧使用抽象类来修饰工具类，可以让使用者无法实例化工具类，只能使用其静态方法
public abstract class ResourceUtils {

	/**
	 * Pseudo URL prefix for loading from the class path: "classpath:".
	 */
	public static final String CLASSPATH_URL_PREFIX = "classpath:";

	/**
	 * URL prefix for loading from the file system: "file:".
	 */
	public static final String FILE_URL_PREFIX = "file:";

	/**
	 * URL prefix for loading from a jar file: "jar:".
	 */
	public static final String JAR_URL_PREFIX = "jar:";

	/**
	 * URL prefix for loading from a war file on Tomcat: "war:".
	 */
	public static final String WAR_URL_PREFIX = "war:";

	/**
	 * 文件系统中文件资源的URL协议：file
	 */
	public static final String URL_PROTOCOL_FILE = "file";

	/**
	 * URL protocol for an entry from a jar file: "jar".
	 */
	public static final String URL_PROTOCOL_JAR = "jar";

	/**
	 * URL protocol for an entry from a war file: "war".
	 */
	public static final String URL_PROTOCOL_WAR = "war";

	/**
	 * URL protocol for an entry from a zip file: "zip".
	 */
	public static final String URL_PROTOCOL_ZIP = "zip";

	/**
	 * URL protocol for an entry from a WebSphere jar file: "wsjar".
	 */
	public static final String URL_PROTOCOL_WSJAR = "wsjar";

	/**
	 * URL protocol for an entry from a JBoss jar file: "vfszip".
	 */
	public static final String URL_PROTOCOL_VFSZIP = "vfszip";

	/**
	 * JBoss文件系统资源的URL协议：vfsfile
	 */
	public static final String URL_PROTOCOL_VFSFILE = "vfsfile";

	/**
	 * JBoss通用的VFS资源的URL协议：vfs
	 */
	public static final String URL_PROTOCOL_VFS = "vfs";

	/**
	 * File extension for a regular jar file: ".jar".
	 */
	public static final String JAR_FILE_EXTENSION = ".jar";

	/**
	 * Separator between JAR URL and file path within the JAR: "!/".
	 */
	public static final String JAR_URL_SEPARATOR = "!/";

	/**
	 * Special separator between WAR URL and jar part on Tomcat.
	 */
	public static final String WAR_URL_SEPARATOR = "*/";


	/**
	 * 判断给定资源位置是否为URL：
	 * 1. "classpath:"开头的伪URL，例如:classpath:org/example/App.class
	 * 2. 标准的URL
	 * <p>
	 * http协议：
	 * http://10.100.23.82
	 * <p>
	 * 访问本地计算机文件：
	 * file:///E:/test
	 * (文件可以不必一定存在，注意后边是三个斜杠)
	 * <p>
	 * ftp协议：
	 * ftp://10.100.23.82
	 * <p>
	 * https协议：
	 * https://10.100.23.82
	 * <p>
	 * 电子邮件：
	 * mailto://10.100.23.82
	 * <p>
	 * 错误(缺少协议或者非标准协议):
	 * www.baidu.com
	 * <p>
	 * ssh://10.100.23.82
	 *
	 * @param resourceLocation 资源路径
	 * @return 该位置是否符合URL要求
	 * @see #CLASSPATH_URL_PREFIX
	 * @see java.net.URL
	 */
	// REMARK 标准协议的种类，只是检查url格式是否符合规范
	public static boolean isUrl(@Nullable String resourceLocation) {
		if (resourceLocation == null) {
			return false;
		}
		if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
			return true;
		}
		try {
			new URL(resourceLocation);
			return true;
		} catch (MalformedURLException ex) {
			return false;
		}
	}


	/**
	 * 将给定的资源位置解析为{@code java.net.URL}.
	 * <p>
	 * 不检查URL是否实际存在,只需返回给定位置将对应的URL
	 * <p>
	 * 除了{@link ResourceUtils#isUrl "isUrl()"}中支持的类型，还支持直接的文件磁盘路径，例如：E:/test
	 *
	 * @param resourceLocation 要解析的资源路径
	 * @return 对应的URL对象
	 * @throws FileNotFoundException 如果资源无法解析为URL
	 */
	public static URL getURL(String resourceLocation) throws FileNotFoundException {
		Assert.notNull(resourceLocation, "Resource location must not be null");
		if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
			// 处理classpath: 伪URL,获取后面的路径
			String path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length());
			// TODO classLoader使用的是TCCL的线程上下文加载器，打破了双亲委派模型?
			ClassLoader cl = ClassUtils.getDefaultClassLoader();
			URL url = (cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path));
			if (url == null) {
				String description = "class path resource [" + path + "]";
				throw new FileNotFoundException(description +
						" cannot be resolved to URL because it does not exist");
			}
			return url;
		}
		try {
			// try URL
			return new URL(resourceLocation);
		} catch (MalformedURLException ex) {
			// no URL -> treat as file path
			try {
				// 对于直接的文件磁盘路径获取URL
				return new File(resourceLocation).toURI().toURL();
			} catch (MalformedURLException ex2) {
				throw new FileNotFoundException("Resource location [" + resourceLocation +
						"] is neither a URL not a well-formed file path");
			}
		}
	}

	/**
	 * Resolve the given resource location to a {@code java.io.File},
	 * i.e. to a file in the file system.
	 * <p>Does not check whether the file actually exists; simply returns
	 * the File that the given location would correspond to.
	 *
	 * @param resourceLocation the resource location to resolve: either a
	 *                         "classpath:" pseudo URL, a "file:" URL, or a plain file path
	 * @return a corresponding File object
	 * @throws FileNotFoundException if the resource cannot be resolved to
	 *                               a file in the file system
	 */
	public static File getFile(String resourceLocation) throws FileNotFoundException {
		Assert.notNull(resourceLocation, "Resource location must not be null");
		if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
			String path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length());
			String description = "class path resource [" + path + "]";
			ClassLoader cl = ClassUtils.getDefaultClassLoader();
			URL url = (cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path));
			if (url == null) {
				throw new FileNotFoundException(description +
						" cannot be resolved to absolute file path because it does not exist");
			}
			return getFile(url, description);
		}
		try {
			// try URL
			return getFile(new URL(resourceLocation));
		} catch (MalformedURLException ex) {
			// no URL -> treat as file path
			return new File(resourceLocation);
		}
	}

	/**
	 * 获取文件，实际调用 {@link ResourceUtils#getFile(URL resourceUrl, String description) "getFile(URL resourceUrl, String description)"}
	 *
	 * @param resourceUrl
	 * @return
	 * @throws FileNotFoundException
	 */
	public static File getFile(URL resourceUrl) throws FileNotFoundException {
		return getFile(resourceUrl, "URL");
	}


	/**
	 * 将给定的资源URL解析为{@code java.io.File} ，即文件系统中的文件
	 *
	 * @param resourceUrl 资源URL
	 * @param description 创建URL的原始资源的描述（例如，类路径位置）
	 * @return File对象
	 * @throws FileNotFoundException 如果无法将URL解析为文件系统中的文件
	 */
	public static File getFile(URL resourceUrl, String description) throws FileNotFoundException {
		Assert.notNull(resourceUrl, "Resource URL must not be null");
		// 判断URL的协议是否为file
		if (!URL_PROTOCOL_FILE.equals(resourceUrl.getProtocol())) {
			throw new FileNotFoundException(
					description + " cannot be resolved to absolute file path " +
							"because it does not reside in the file system: " + resourceUrl);
		}
		try {
			// getSchemeSpecificPart()实际就是去掉uri的协议头以及#后锚点，这里相当转换成了文件磁盘路径
			return new File(toURI(resourceUrl).getSchemeSpecificPart());
		} catch (URISyntaxException ex) {
			// Fallback for URLs that are not valid URIs (should hardly ever happen).
			// TODO 不清楚这里
			return new File(resourceUrl.getFile());
		}
	}


	/**
	 * 获取资源文件，实际调用 {@link ResourceUtils#getFile(URI resourceUri, String description) "getFile(URI resourceUri, String description)"}
	 *
	 * @param resourceUri
	 * @return
	 * @throws FileNotFoundException
	 */
	public static File getFile(URI resourceUri) throws FileNotFoundException {
		return getFile(resourceUri, "URI");
	}

	/**
	 * 将给定的资源URI解析为{@code java.io.File} ，即文件系统中的文件
	 *
	 * @param resourceUri 资源URI
	 * @param description 创建URI的原始资源的描述（例如，类路径位置）
	 * @return File对象
	 * @throws FileNotFoundException 如果无法将URI解析为文件系统中的文件
	 */
	public static File getFile(URI resourceUri, String description) throws FileNotFoundException {
		Assert.notNull(resourceUri, "Resource URI must not be null");
		if (!URL_PROTOCOL_FILE.equals(resourceUri.getScheme())) {
			throw new FileNotFoundException(
					description + " cannot be resolved to absolute file path " +
							"because it does not reside in the file system: " + resourceUri);
		}
		return new File(resourceUri.getSchemeSpecificPart());
	}

	/**
	 * 判断URL是否为文件协议资源
	 * <p>
	 * 具体包含协议:file、vfsfile、vfs
	 *
	 * @param url
	 * @return
	 */
	public static boolean isFileURL(URL url) {
		String protocol = url.getProtocol();
		return (URL_PROTOCOL_FILE.equals(protocol) || URL_PROTOCOL_VFSFILE.equals(protocol) ||
				URL_PROTOCOL_VFS.equals(protocol));
	}

	/**
	 * 判断URL是否为jar协议资源
	 * <p>
	 * 具体包含协议:jar、war、zip、vfszip、wsjar
	 *
	 * @param url
	 * @return
	 */
	public static boolean isJarURL(URL url) {
		String protocol = url.getProtocol();
		return (URL_PROTOCOL_JAR.equals(protocol) || URL_PROTOCOL_WAR.equals(protocol) ||
				URL_PROTOCOL_ZIP.equals(protocol) || URL_PROTOCOL_VFSZIP.equals(protocol) ||
				URL_PROTOCOL_WSJAR.equals(protocol));
	}

	/**
	 * 判断URL是否为jar文件
	 * <p>
	 * 具体协议为file,且文件名为.jar
	 *
	 * @param url
	 * @return
	 */
	public static boolean isJarFileURL(URL url) {
		return (URL_PROTOCOL_FILE.equals(url.getProtocol()) &&
				url.getPath().toLowerCase().endsWith(JAR_FILE_EXTENSION));
	}

	/**
	 * Extract the URL for the actual jar file from the given URL
	 * (which may point to a resource in a jar file or to a jar file itself).
	 *
	 * @param jarUrl the original URL
	 * @return the URL for the actual jar file
	 * @throws MalformedURLException if no valid jar file URL could be extracted
	 */
	public static URL extractJarFileURL(URL jarUrl) throws MalformedURLException {
		String urlFile = jarUrl.getFile();
		int separatorIndex = urlFile.indexOf(JAR_URL_SEPARATOR);
		if (separatorIndex != -1) {
			String jarFile = urlFile.substring(0, separatorIndex);
			try {
				return new URL(jarFile);
			} catch (MalformedURLException ex) {
				// Probably no protocol in original jar URL, like "jar:C:/mypath/myjar.jar".
				// This usually indicates that the jar file resides in the file system.
				if (!jarFile.startsWith("/")) {
					jarFile = "/" + jarFile;
				}
				return new URL(FILE_URL_PREFIX + jarFile);
			}
		} else {
			return jarUrl;
		}
	}

	/**
	 * Extract the URL for the outermost archive from the given jar/war URL
	 * (which may point to a resource in a jar file or to a jar file itself).
	 * <p>In the case of a jar file nested within a war file, this will return
	 * a URL to the war file since that is the one resolvable in the file system.
	 *
	 * @param jarUrl the original URL
	 * @return the URL for the actual jar file
	 * @throws MalformedURLException if no valid jar file URL could be extracted
	 * @see #extractJarFileURL(URL)
	 * @since 4.1.8
	 */
	public static URL extractArchiveURL(URL jarUrl) throws MalformedURLException {
		String urlFile = jarUrl.getFile();

		int endIndex = urlFile.indexOf(WAR_URL_SEPARATOR);
		if (endIndex != -1) {
			// Tomcat's "war:file:...mywar.war*/WEB-INF/lib/myjar.jar!/myentry.txt"
			String warFile = urlFile.substring(0, endIndex);
			if (URL_PROTOCOL_WAR.equals(jarUrl.getProtocol())) {
				return new URL(warFile);
			}
			int startIndex = warFile.indexOf(WAR_URL_PREFIX);
			if (startIndex != -1) {
				return new URL(warFile.substring(startIndex + WAR_URL_PREFIX.length()));
			}
		}

		// Regular "jar:file:...myjar.jar!/myentry.txt"
		return extractJarFileURL(jarUrl);
	}

	/**
	 * 获取URI实例，实际调用{@link ResourceUtils#toURI(String location) "toURI(String location)"}
	 *
	 * @param url
	 * @return
	 * @throws URISyntaxException
	 */
	public static URI toURI(URL url) throws URISyntaxException {
		return toURI(url.toString());
	}


	/**
	 * 为给定的位置字符串创建URI实例
	 * <p>
	 * 首先会用"％20"(即为/) 替换空格
	 *
	 * @param location 资源路径
	 * @return URI实例
	 * @throws URISyntaxException 如果资源路径不是有效的URI
	 */
	// REMARK 标准的URI格式 [scheme:]scheme-specific-part[#fragment]
	public static URI toURI(String location) throws URISyntaxException {
		// 替换资源路径中的" "为/,并转为URI
		return new URI(StringUtils.replace(location, " ", "%20"));
	}

	/**
	 * Set the {@link URLConnection#setUseCaches "useCaches"} flag on the
	 * given connection, preferring {@code false} but leaving the
	 * flag at {@code true} for JNLP based resources.
	 *
	 * @param con the URLConnection to set the flag on
	 */
	public static void useCachesIfNecessary(URLConnection con) {
		con.setUseCaches(con.getClass().getSimpleName().startsWith("JNLP"));
	}

}
