package bdsm.kelovp.com.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Consts;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通用HttpClients
 */
@Slf4j
@SuppressWarnings("unused")
public class HttpClients {

	private static final ContentType GLOBAL_FORM_URLENCODED = ContentType.create(ContentType.APPLICATION_FORM_URLENCODED.getMimeType(), Consts.UTF_8);
	public static final ContentType GLOBAL_TEXT_PLAIN = ContentType.create(ContentType.TEXT_PLAIN.getMimeType(), Consts.UTF_8);
	private static final ContentType GLOBAL_APPLICATION_JSON = ContentType.APPLICATION_JSON;

	private static final String IGNORE_PROPERTY_NAME = "class";

	private static final Set<Integer> SUCCESS_STATUS = new HashSet<>();

	static {
		SUCCESS_STATUS.add(HttpStatus.SC_OK);
		SUCCESS_STATUS.add(HttpStatus.SC_CREATED);
		SUCCESS_STATUS.add(HttpStatus.SC_NO_CONTENT);
	}

	// 请求配置常量
	private static final int CONNECTION_REQUEST_TIMEOUT = 100000 * 30;
	private static final int CONNECT_TIMEOUT = 100000 * 30;
	private static final int SOCKET_TIMEOUT = 100000 * 30;
	// 默认请求配置
	private static final RequestConfig DEFAULT_REQUEST_CONFIG;
	// 连接池配置常量
	private static final int MAX_TOTAL = 200;
	private static final int DEFAULT_MAX_PER_ROUTE = 20;
	// 默认连接池配置
	private static final PoolingHttpClientConnectionManager DEFAULT_CONNECTION_MANAGER;
	// 默认http连接
	private static final CloseableHttpClient DEFAULT_HTTP_CLIENT;

	static {
		// 默认请求配置
		DEFAULT_REQUEST_CONFIG = RequestConfig
				.custom()
				.setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT) // 从连接池中获取连接的超时时间
				.setConnectTimeout(CONNECT_TIMEOUT) // 与服务器连接超时时间：HttpClient会创建一个异步线程用以创建socket连接，此处设置该socket的连接超时时间
				.setSocketTimeout(SOCKET_TIMEOUT) // socket读数据超时时间：从服务器获取响应数据的超时时间
				.build();

		// 默认连接池配置
		DEFAULT_CONNECTION_MANAGER = new PoolingHttpClientConnectionManager();
		DEFAULT_CONNECTION_MANAGER.setMaxTotal(MAX_TOTAL); // 设置连接池的最大连接数
		DEFAULT_CONNECTION_MANAGER.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);

		// 默认http连接
		DEFAULT_HTTP_CLIENT = HttpClientBuilder
				.create()
				.setDefaultRequestConfig(DEFAULT_REQUEST_CONFIG) // 设置默认请求配置
				.setConnectionManager(DEFAULT_CONNECTION_MANAGER) // 设置默认连接池
				.build();
	}

	/**
	 * GET方法
	 * @param url 接口url
	 * @param param 请求url的数据
	 * @param header 请求header的数据
	 * @param printLog 是否打印流程日志
	 * @return 接口出参String
	 */
	private static String baseGet(String url, List<NameValuePair> param, Map<String, Object> header, List<NameValuePair> body, String content, ContentType contentType,
			boolean printLog) {
		// url参数设置
		url = createUrlParam(url, param);
		// GET请求
		HttpGetWithEntity httpGetWithEntity = new HttpGetWithEntity(url);
		// 其他参数设置
		createHeader(httpGetWithEntity, mapToList(header));
		if (null == contentType) {
			contentType = GLOBAL_FORM_URLENCODED;
		}
		createBodyParam(httpGetWithEntity, body, contentType);
		createBodyContent(httpGetWithEntity, content, contentType);
		// 执行请求
		return baseHttp(httpGetWithEntity, url, header, null, null, printLog);
	}

	/**
	 * POST方法
	 * @param url 接口url
	 * @param param 请求url的数据
	 * @param header 请求header的数据
	 * @param body 请求body的数据
	 * @param content 请求body的字符串
	 * @param contentType 请求body的数据类型
	 * @param printLog 是否打印流程日志
	 * @return 接口出参String
	 */
	@SuppressWarnings("SameParameterValue")
	private static String basePost(String url, List<NameValuePair> param, Map<String, Object> header, List<NameValuePair> body, String content, ContentType contentType,
			boolean printLog) {
		// url参数设置
		url = createUrlParam(url, param);
		// POST请求
		HttpPost httpPost = new HttpPost(url);
		// 其他参数设置
		createHeader(httpPost, mapToList(header));
		if (null == contentType) {
			contentType = GLOBAL_FORM_URLENCODED;
		}
		createBodyParam(httpPost, body, contentType);
		createBodyContent(httpPost, content, contentType);
		// 执行请求
		return baseHttp(httpPost, url, header, body, content, printLog);
	}

	/**
	 * 执行基础http请求
	 */
	private static String baseHttp(HttpUriRequest request, String url, Map<String, Object> header, List<NameValuePair> body, String content, boolean printLog) {
		try {
			// 请求发送
			long startTime = System.currentTimeMillis();
			CloseableHttpResponse response = DEFAULT_HTTP_CLIENT.execute(request);
			long requestTime = System.currentTimeMillis() - startTime;
			// 响应解析
			if (null != response) {
				try {
					StatusLine statusLine = response.getStatusLine();
					Integer status = statusLine.getStatusCode();
					String result = null == response.getEntity() ? null : EntityUtils.toString(response.getEntity(), "UTF-8");
					if (SUCCESS_STATUS.contains(status)) {
						if (printLog) {
							log.info(joinLogString(true, url, header, body, content, status, result, requestTime));
						}
						return result;
					} else {
						log.error(joinLogString(false, url, header, body, content, status, result, requestTime));
						throw new RuntimeException("接口返回状态异常");
					}
				} finally {
					response.close();
				}
			}
		} catch (IOException e) {
			log.error(joinLogString(false, url, header, body, content, null, null, null), e);
		}
		throw new RuntimeException("接口调用失败");
	}

	/**
	 * Entry 转换 NameValuePair
	 */
	private static NameValuePair entryToNameValuePair(Map.Entry<String, Object> entry) {
		return new BasicNameValuePair(entry.getKey(), (entry.getValue() instanceof Date) ? DateUtil.dateToString((Date) entry.getValue()) : Strings.toString(entry.getValue()));
	}

	/**
	 * Map 转换 List<NameValuePair>
	 */
	private static List<NameValuePair> mapToList(Map<String, Object> map) {
		if (Maps.noEmpty(map)) {
			return map.entrySet()
					.stream()
					.filter(entry -> Strings.noEmpty(entry.getKey()) && Strings.noEmpty(entry.getValue()))
					.map(HttpClients::entryToNameValuePair)
					.collect(Collectors.toList());
		}
		return null;
	}

	/**
	 * List<NameValuePair> 转换 Map
	 */
	@SuppressWarnings({ "unchecked", "ConstantConditions" })
	private static Map<String, Object> listToMap(List<NameValuePair> list) {
		try {
			if (Lists.noEmpty(list)) {
				return list.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue, (left, right) -> {
					if (left instanceof ArrayList) {
						((ArrayList) left).add(right);
					} else {
						Object temp = left;
						left = new ArrayList<>();
						((ArrayList) left).add(temp);
						((ArrayList) left).add(right);
					}
					return left;
				}));
			}
		} catch (Exception e) {
			log.error("", e);
		}
		return null;
	}

	/**
	 * Bean 转换 Map
	 */
	private static Map<String, Object> beanToMap(Object bean) {
		Map<String, Object> map = new HashMap<>();
		try {
			PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors();
			for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
				String propertyName = propertyDescriptor.getName();
				if (!propertyName.equals(IGNORE_PROPERTY_NAME)) {
					Method readMethod = propertyDescriptor.getReadMethod();
					Object result = readMethod.invoke(bean);
					if (result != null) {
						map.put(propertyName, result);
					}
				}
			}
		} catch (Exception e) {
			log.error("Bean 转换 Map 异常", e);
		}
		return map;
	}

	/**
	 * Bean 转换 List<NameValuePair>
	 */
	private static List<NameValuePair> beanToList(Object bean) {
		return mapToList(beanToMap(bean));
	}

	/**
	 * 拼装url参数
	 */
	private static String createUrlParam(String url, List<NameValuePair> urlParam) {
		if (Lists.noEmpty(urlParam)) {
			String urlSuffix = URLEncodedUtils.format(urlParam, Consts.UTF_8);
			if (url.indexOf(Strings.GLOBAL_QUESTION_MARK) < 1) {
				url = url + Strings.GLOBAL_QUESTION_MARK + urlSuffix;
			} else {
				if (url.endsWith(Strings.GLOBAL_AND)) {
					url = url + urlSuffix;
				} else {
					url = Strings.GLOBAL_AND + url + urlSuffix;
				}
			}
		}
		return url;
	}

	/**
	 * 拼装header参数
	 */
	private static void createHeader(AbstractHttpMessage httpMessage, List<NameValuePair> header) {
		if (Lists.noEmpty(header)) {
			header.forEach(nameValuePair -> {
				if (Strings.isEmpty(httpMessage.getFirstHeader(nameValuePair.getName()))) {
					httpMessage.addHeader(nameValuePair.getName(), Strings.toString(nameValuePair.getValue()));
				} else {
					httpMessage.setHeader(nameValuePair.getName(), Strings.toString(nameValuePair.getValue()));
				}
			});
		}
	}

	/**
	 * 拼装body参数
	 */
	private static void createBodyParam(HttpEntityEnclosingRequestBase requestBase, List<NameValuePair> nameValuePairList, ContentType contentType) {
		if (Lists.noEmpty(nameValuePairList)) {
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairList, Consts.UTF_8);
			entity.setContentType(contentType.toString());
			requestBase.setEntity(entity);
		}
	}

	/**
	 * 拼装body参数
	 */
	private static void createBodyContent(HttpEntityEnclosingRequestBase requestBase, String bodyContent, ContentType contentType) {
		if (Strings.noEmpty(bodyContent)) {
			StringEntity entity = new StringEntity(bodyContent, Consts.UTF_8);
			entity.setContentType(contentType.toString());
			requestBase.setEntity(entity);
		}
	}

	/**
	 * 通用日志打印字符串拼接
	 */
	private static String joinLogString(boolean success, String url, Map<String, Object> header, List<NameValuePair> body, String content, Integer status, String result,
			Long requestTime) {
		StringBuilder stringBuilder = new StringBuilder();
		if (success) {
			stringBuilder.append("【success】");
		} else {
			stringBuilder.append("【exception】");
		}
		stringBuilder.append("【url】").append(url);
		if (Maps.noEmpty(header)) {
			stringBuilder.append("【header】").append(JSONObject.toJSONString(header));
		}
		if (Strings.noEmpty(body)) {
			stringBuilder.append("【body】").append(URLEncodedUtils.format(body, Consts.UTF_8));
		}
		if (Strings.noEmpty(content)) {
			stringBuilder.append("【content】").append(content);
		}
		if (Objects.nonNull(status)) {
			stringBuilder.append("【status】").append(status);
		}
		if (Strings.noEmpty(result)) {
			stringBuilder.append("【result】").append(result);
		}
		if (Objects.nonNull(requestTime)) {
			stringBuilder.append("【cost】").append(requestTime);
		}
		return stringBuilder.toString();
	}

	/**
	 * 获取文件的ContentType
	 */
	private static ContentType getContentType(File file) {
		ContentType contentType;
		try {
			contentType = ContentType.create(Files.probeContentType(Paths.get(file.getPath())), (Charset) null);
		} catch (IOException e) {
			contentType = ContentType.DEFAULT_BINARY;
		}
		log.info("ContentType - {}", contentType.toString());
		return contentType;
	}

	/**
	 * 根据ContentType获取文件后缀
	 */
	private static String getSuffix(String contentType) {
		String suffix;
		try {
			suffix = MimeTypes.getDefaultMimeTypes().forName(contentType).getExtension();
		} catch (MimeTypeException e) {
			suffix = "";
		}
		log.info("Suffix - {}", suffix);
		return suffix;
	}

	/**
	 * 资源文件上传
	 */
	@SuppressWarnings("UnusedReturnValue")
	public static String upload(String url, Map<String, Object> params, Map<String, File> fileMap, Boolean printLog) {
		// POST请求
		HttpPost httpPost = new HttpPost(url);
		// 文件解析处理器，自定义charset，RFC6532=utf-8，STRICT=iso-8859-1
		MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.RFC6532);
		// 普通入参处理
		if (Maps.noEmpty(params)) {
			params.forEach((key, value) -> {
				StringBody stringBody = new StringBody(Strings.toString(value), ContentType.create("multipart/form-data", Consts.UTF_8));
				multipartEntityBuilder.addPart(key, stringBody);
			});
		}
		// 文件入参处理
		if (Maps.noEmpty(fileMap)) {
			fileMap.forEach((key, value) -> {
				FileBody fileBody = new FileBody(value, getContentType(value));
				multipartEntityBuilder.addPart(key, fileBody);
			});
		}
		httpPost.setEntity(multipartEntityBuilder.build());

		// 执行请求
		return baseHttp(httpPost, url, null, mapToList(params), null, printLog);
	}

	/**
	 * 资源文件下载
	 */
	@SuppressWarnings("unused")
	public static File download(String fileUrl) {
		return download(
				fileUrl,
				System.getProperty("user.dir") + File.separator + "temp" + File.separator + "file",
				UUID.randomUUID().toString());
	}

	/**
	 * 资源文件下载
	 */
	@SuppressWarnings("unused")
	public static File download(String fileUrl, String saveName) {
		return download(
				fileUrl,
				System.getProperty("user.dir") + File.separator + "temp" + File.separator + "file",
				saveName);
	}

	/**
	 * 资源文件下载
	 */
	public static File download(String fileUrl, String savePath, String saveName) {
		// 构造URL
		URL url;
		try {
			url = new URL(fileUrl);
		} catch (MalformedURLException e) {
			throw new RuntimeException("文件url不正确", e);
		}
		// 打开连接
		URLConnection urlConnection;
		try {
			urlConnection = url.openConnection();
		} catch (IOException e) {
			throw new RuntimeException("文件url打开失败", e);
		}
		// 设置请求超时为5s
		urlConnection.setConnectTimeout(5 * 1000);
		// 输入流
		InputStream inputStream;
		try {
			inputStream = urlConnection.getInputStream();
		} catch (IOException e) {
			throw new RuntimeException("输入流异常", e);
		}
		// 1K的数据缓冲
		byte[] bytes = new byte[1024];
		// 读取到的数据长度
		int len;
		// 根据ContentType确定文件实际后缀名
		String suffix = getSuffix(urlConnection.getHeaderField("Content-Type"));
		// 文件全路径拼接
		String filePath = savePath + File.separator + saveName + suffix;
		log.info("filePath = {}", filePath);
		File file = new File(filePath);
		if (!file.getParentFile().exists()) {
			// noinspection ResultOfMethodCallIgnored
			file.getParentFile().mkdirs();
		}
		OutputStream outputStream;
		try {
			outputStream = new FileOutputStream(file);
			// 开始读取
			while ((len = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, len);
			}
			// 完毕，关闭所有链接
			inputStream.close();
			outputStream.close();
		} catch (Exception e) {
			throw new RuntimeException("文件输出发生异常", e);
		}
		return file;
	}

	/** base function **/
	public static String GET(String url, Boolean printLog) {
		return baseGet(url, null, null, null, null, null, printLog);
	}

	public static String GET(String url, Map<String, Object> params, Boolean printLog) {
		return baseGet(url, mapToList(params), null, null, null, null, printLog);
	}

	public static String GET(String url, Map<String, Object> headers, Map<String, Object> params, Boolean printLog) {
		return baseGet(url, mapToList(params), headers, null, null, null, printLog);
	}

	public static String GET(String url, Object bean, Boolean printLog) {
		return baseGet(url, beanToList(bean), null, null, null, null, printLog);
	}

	public static String GET(String url, List<NameValuePair> nameValuePairList, Boolean printLog) {
		return baseGet(url, null, null, nameValuePairList, null, null, printLog);
	}

	public static String GET(String url, String content, Boolean printLog) {
		return baseGet(url, null, null, null, content, GLOBAL_APPLICATION_JSON, printLog);
	}

	/** auto log function **/
	public static String GET(String url) {
		return GET(url, true);
	}

	public static String GET(String url, Map<String, Object> params) {
		return GET(url, params, true);
	}

	public static String GET(String url, Map<String, Object> headers, Map<String, Object> params) {
		return GET(url, params, headers, true);
	}

	public static String GET(String url, Object bean) {
		return GET(url, bean, true);
	}

	public static String GET(String url, String json) {
		return GET(url, json, true);
	}

	/** auto convert function **/
	public static <G> G GET(String url, Class<G> rClass) {
		return JSONObject.parseObject(GET(url), rClass);
	}

	public static <G> G GET(String url, Map<String, Object> params, Class<G> rClass) {
		return JSONObject.parseObject(GET(url, params), rClass);
	}

	public static <G> G GET(String url, Map<String, Object> headers, Map<String, Object> params, Class<G> rClass) {
		return JSONObject.parseObject(GET(url, headers, params), rClass);
	}

	public static <G> G GET(String url, Object bean, Class<G> rClass) {
		return JSONObject.parseObject(GET(url, bean), rClass);
	}

	/** auto convert function **/
	public static String assertGet(String url) {
		JSONObject jsonObject = JSONObject.parseObject(GET(url));
		if (Objects.equals(HttpStatus.SC_OK, jsonObject.getIntValue("status"))) {
			return jsonObject.getString("data");
		} else {
			throw new RuntimeException(jsonObject.getString("message"));
		}
	}

	public static <G> G assertGet(String url, Class<G> rClass) {
		JSONObject jsonObject = JSONObject.parseObject(GET(url));
		if (Objects.equals(HttpStatus.SC_OK, jsonObject.getIntValue("status"))) {
			return JSONObject.parseObject(jsonObject.getString("data"), rClass);
		} else {
			throw new RuntimeException(jsonObject.getString("message"));
		}
	}

	public static <G> G assertGet(String url, Map<String, Object> params, Class<G> rClass) {
		JSONObject jsonObject = JSONObject.parseObject(GET(url, params));
		if (Objects.equals(HttpStatus.SC_OK, jsonObject.getIntValue("status"))) {
			return JSONObject.parseObject(jsonObject.getString("data"), rClass);
		} else {
			throw new RuntimeException(jsonObject.getString("message"));
		}
	}

	public static <G> G assertGet(String url, Map<String, Object> headers, Map<String, Object> params, Class<G> rClass) {
		JSONObject jsonObject = JSONObject.parseObject(GET(url, headers));
		if (Objects.equals(HttpStatus.SC_OK, jsonObject.getIntValue("status"))) {
			return JSONObject.parseObject(jsonObject.getString("data"), rClass);
		} else {
			throw new RuntimeException(jsonObject.getString("message"));
		}
	}

	public static <G> G assertGet(String url, Object bean, Class<G> rClass) {
		JSONObject jsonObject = JSONObject.parseObject(GET(url, bean));
		if (Objects.equals(HttpStatus.SC_OK, jsonObject.getIntValue("status"))) {
			return JSONObject.parseObject(jsonObject.getString("data"), rClass);
		} else {
			throw new RuntimeException(jsonObject.getString("message"));
		}
	}

	/** manual convert function **/
	public static <R> R assertResult(String url, Map<String, Object> params, Function<? super JSONObject, ? extends R> successFunction) {
		JSONObject jsonObject = JSONObject.parseObject(GET(url, params));
		if (Objects.equals(HttpStatus.SC_OK, jsonObject.getIntValue("status"))) {
			return successFunction.apply(jsonObject);
		} else {
			throw new RuntimeException(jsonObject.getString("message"));
		}
	}

	/** base function **/
	public static String POST(String url, Map<String, Object> params, Boolean printLog) {
		return basePost(url, null, null, mapToList(params), null, null, printLog);
	}

	public static String POST(String url, String json, Boolean printLog) {
		return basePost(url, null, null, null, json, GLOBAL_APPLICATION_JSON, printLog);
	}

	public static String POST(String url, String json, ContentType contentType, Boolean printLog) {
		return basePost(url, null, null, null, json, contentType, printLog);
	}

	public static String POST(String url, Object bean, Boolean printLog) {
		return basePost(url, null, null, beanToList(bean), null, null, printLog);
	}

	public static String POST(String url, List<NameValuePair> nameValuePairList, Boolean printLog) {
		return basePost(url, null, null, nameValuePairList, null, null, printLog);
	}

	public static String POST(String url, Map<String, Object> headers, Map<String, Object> params, Boolean printLog) {
		return basePost(url, null, headers, mapToList(params), null, null, printLog);
	}

	public static String POST(String url, Map<String, Object> headers, String content, Boolean printLog) {
		return basePost(url, null, headers, null, content, GLOBAL_APPLICATION_JSON, printLog);
	}

	public static String POST(String url, Map<String, Object> headers, Object bean, Boolean printLog) {
		return basePost(url, null, headers, beanToList(bean), null, null, printLog);
	}

	public static String POST(String url, Map<String, Object> headers, List<NameValuePair> nameValuePairList, Boolean printLog) {
		return basePost(url, null, headers, nameValuePairList, null, null, printLog);
	}

	/** auto log function **/
	public static String POST(String url, Map<String, Object> params) {
		return POST(url, params, true);
	}

	public static String POST(String url, String json) {
		return POST(url, json, true);
	}

	public static String POST(String url, String json, ContentType contentType) {
		return POST(url, json, contentType, true);
	}

	public static String POST(String url, Object bean) {
		return POST(url, bean, true);
	}

	public static String POST(String url, List<NameValuePair> nameValuePairList) {
		return POST(url, nameValuePairList, true);
	}

	public static String POST(String url, Map<String, Object> headers, Map<String, Object> params) {
		return POST(url, headers, params, true);
	}

	public static String POST(String url, Map<String, Object> headers, String json) {
		return POST(url, headers, json, true);
	}

	public static String POST(String url, Map<String, Object> headers, Object bean) {
		return POST(url, headers, bean, true);
	}

	public static String POST(String url, Map<String, Object> headers, List<NameValuePair> nameValuePairList) {
		return POST(url, headers, nameValuePairList, true);
	}

	/** auto convert function **/
	public static <P> P POST(String url, Map<String, Object> params, Class<P> pClass) {
		return JSONObject.parseObject(POST(url, params), pClass);
	}

	public static <P> P POST(String url, String json, Class<P> pClass) {
		return JSONObject.parseObject(POST(url, json), pClass);
	}

	public static <P> P POST(String url, String json, ContentType contentType, Class<P> pClass) {
		return JSONObject.parseObject(POST(url, json, contentType), pClass);
	}

	public static <P> P POST(String url, Object bean, Class<P> pClass) {
		return JSONObject.parseObject(POST(url, bean), pClass);
	}

	public static <P> P POST(String url, List<NameValuePair> nameValuePairList, Class<P> pClass) {
		return JSONObject.parseObject(POST(url, nameValuePairList), pClass);
	}

	public static <P> P POST(String url, Map<String, Object> headers, Map<String, Object> params, Class<P> pClass) {
		return JSONObject.parseObject(POST(url, headers, params), pClass);
	}

	public static <P> P POST(String url, Map<String, Object> headers, String json, Class<P> pClass) {
		return JSONObject.parseObject(POST(url, headers, json), pClass);
	}

	public static <P> P POST(String url, Map<String, Object> headers, Object bean, Class<P> pClass) {
		return JSONObject.parseObject(POST(url, headers, bean), pClass);
	}

	public static <P> P POST(String url, Map<String, Object> headers, List<NameValuePair> nameValuePairList, Class<P> pClass) {
		return JSONObject.parseObject(POST(url, headers, nameValuePairList), pClass);
	}

	/** manual convert function **/
	public static <R> R assertResult(String result, Function<? super JSONObject, ? extends R> successFunction) {
		JSONObject jsonObject = JSONObject.parseObject(result);
		if (Objects.equals(HttpStatus.SC_OK, jsonObject.getIntValue("status"))) {
			return successFunction.apply(jsonObject);
		} else {
			throw new RuntimeException(jsonObject.getString("message"));
		}
	}

	private static class HttpGetWithEntity extends HttpEntityEnclosingRequestBase {

		public static final String METHOD_NAME = "GET";

		public HttpGetWithEntity() {
			super();
		}

		public HttpGetWithEntity(final URI uri) {
			super();
			setURI(uri);
		}

		HttpGetWithEntity(final String uri) {
			super();
			setURI(URI.create(uri));
		}

		@Override
		public String getMethod() {
			return METHOD_NAME;
		}
	}
}
