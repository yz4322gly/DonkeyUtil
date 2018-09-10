package xin.tomdonkey.util.http.common.config;

import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.net.ssl.SSLException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * @author guolinyuan
 */
@Configuration
@PropertySource("classpath:httpclient.properties")
public class HttpClientConfig
{

    /*
     *******************************保持连接策略********************************
     */

    @Value("${httpclient.config.keepAliveTime}")
    private int keeAliveTime = 30;

    @Bean
    public ConnectionKeepAliveStrategy connectionKeepAliveStrategy()
    {
        return (response, context) ->
        {
            HeaderElementIterator it = new BasicHeaderElementIterator(
                    response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext())
            {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase("timeout"))
                {
                    try
                    {
                        return Long.parseLong(value) * 1000;
                    }
                    catch (NumberFormatException ignore)
                    {
                    }
                }
            }
            return 30 * 1000;
        };
    }


    /*
     *******************************请求重试处理********************************
     */

    /**
     *  此处建议采用@ConfigurationProperties(prefix="httpclient.config")方式，方便复用
     */
    @Value("${httpclient.config.retryTime}")
    private int retryTime = 3;

    @Bean
    public HttpRequestRetryHandler httpRequestRetryHandler()
    {
        // 请求重试
        final int retryTime = this.retryTime;

        return (exception, executionCount, context) ->
        {
            // Do not retry if over max retry count,如果重试次数超过了retryTime,则不再重试请求
            if (executionCount >= retryTime)
            {
                return false;
            }
            // 服务端断掉客户端的连接异常
            if (exception instanceof NoHttpResponseException)
            {
                return true;
            }
            // time out 超时重试
            if (exception instanceof InterruptedIOException)
            {
                return true;
            }
            // Unknown host
            if (exception instanceof UnknownHostException)
            {
                return false;
            }
            // Connection refused
            // SSL handshake exception
            if (exception instanceof SSLException)
            {
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            return !(request instanceof HttpEntityEnclosingRequest);

        };
    }

    /*
     *******************************连接池管理********************************
     */

    /**
     * 连接池最大连接数
     */
    @Value("${httpclient.config.connMaxTotal}")
    private int connMaxTotal = 100;

    /**
     * 每个路由的最大连接数
     */
    @Value("${httpclient.config.maxPerRoute}")
    private int maxPerRoute = 20;

    /**
     * 连接存活时间，单位为s
     */
    @Value("${httpclient.config.timeToLive}")
    private int timeToLive = 60;

    @Bean
    public PoolingHttpClientConnectionManager poolingClientConnectionManager()
    {
        PoolingHttpClientConnectionManager poolHttpConnManager = new PoolingHttpClientConnectionManager(60, TimeUnit.SECONDS);
        // 最大连接数
        poolHttpConnManager.setMaxTotal(this.connMaxTotal);
        // 路由基数
        poolHttpConnManager.setDefaultMaxPerRoute(this.maxPerRoute);
        return poolHttpConnManager;
    }

    /*
     *******************************HttpClient代理********************************
     */

    /**
     *     代理的host地址
     */

    @Value("${httpclient.config.proxyHost}")
    private String proxyHost = "127.0.0.1";

    /**
     * 代理的端口号
     */
    @Value("${httpclient.config.proxyPort}")
    private int proxyPort = 8080;

    @Bean
    public DefaultProxyRoutePlanner defaultProxyRoutePlanner()
    {
        HttpHost proxy = new HttpHost(this.proxyHost, this.proxyPort);
        return new DefaultProxyRoutePlanner(proxy);
    }

    /*
     *******************************设置请求的各种配置********************************
     */

    @Value("${httpclient.config.connectTimeout}")
    private int connectTimeout = 2000;

    @Value("${httpclient.config.connectRequestTimeout}")
    private int connectRequestTimeout = 2000;

    @Value("${httpclient.config.socketTimeout}")
    private int socketTimeout = 2000;

    @Bean
    public RequestConfig requestConfig()
    {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(this.connectRequestTimeout)
                .setConnectTimeout(this.connectTimeout)
                .setSocketTimeout(this.socketTimeout)
                .build();
    }
}

