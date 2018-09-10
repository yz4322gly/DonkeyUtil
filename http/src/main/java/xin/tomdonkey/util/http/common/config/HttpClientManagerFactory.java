package xin.tomdonkey.util.http.common.config;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpClientManagerFactory implements FactoryBean<CloseableHttpClient>, InitializingBean, DisposableBean
{

    @Autowired
    private HttpClientConnectionManager poolingClientConnectionManager;
    @Autowired
    private ConnectionKeepAliveStrategy connectionKeepAliveStrategy;
    @Autowired
    private HttpRequestRetryHandler httpRequestRetryHandler;
    @Autowired
    private HttpRoutePlanner httpRoutePlanner;
    @Autowired
    private RequestConfig requestConfig;

    /**
     * 是否启用代理设置
     */
    @Value("${httpclient.config.proxy}")
    private boolean proxy = false;

    /**
     * FactoryBean生成的目标对象
     */
    private CloseableHttpClient client;

    @Override
    public CloseableHttpClient getObject() throws Exception
    {
        return this.client;
    }

    @Override
    public Class<?> getObjectType()
    {
        return this.client == null ? CloseableHttpClient.class : this.client.getClass();
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(poolingClientConnectionManager)
                .setKeepAliveStrategy(connectionKeepAliveStrategy)
                .setRetryHandler(httpRequestRetryHandler)
                .setDefaultRequestConfig(requestConfig);

        //默认不使用代理
        //若httpclient.config.proxy配置为true，则启用
        if (proxy)
        {
            httpClientBuilder.setRoutePlanner(httpRoutePlanner);
        }


        this.client = httpClientBuilder.build();

    }

    @Override
    public void destroy() throws Exception
    {
        /*
         * 调用httpClient.close()会先shut down connection manager，然后再释放该HttpClient所占用的所有资源，
         * 关闭所有在使用或者空闲的connection包括底层socket。由于这里把它所使用的connection manager关闭了，
         * 所以在下次还要进行http请求的时候，要重新new一个connection manager来build一个HttpClient,
         * 也就是在需要关闭和新建Client的情况下，connection manager不能是单例的.
         */
        if (null != this.client)
        {
            this.client.close();
        }
    }
}
