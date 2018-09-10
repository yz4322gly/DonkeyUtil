package xin.tomdonkey.util.http.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.sun.istack.internal.NotNull;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;


/**
 * httpClient 相关工具类
 *
 * @author guolinyuan
 */
@Component
public class HttpUtil
{
    private static  Charset DEFAULT_CHARSET = Charset.forName("utf-8");
    private static Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    private static ContentType JSON_CONTENT_TYPE = ContentType.create("application/json", DEFAULT_CHARSET);

    @Autowired
    private CloseableHttpClient client;

    /*
     *******************************请求方法******************************************************
     */

    /**
     * 同步请求，使用此请求方法作为基础方法的请求，均为同步请求，请求会一直等待
     * 直到：
     * 如果设置了连接超时时间（connectTimeout）和重试次数 （retryTime）
     * 则在尝试了retryTime次数，每次时长为connectTimeout时，返回null，并打印日志：发生io异常
     *
     * 使用指定的httpRequestBase 发送请求
     * 获取原始CloseableHttpResponse响应
     * 需要指定请求头时，请修改指定的httpRequestBase
     * 作为作为原始基础方法，此类不推荐直接使用
     * 此方法的所有异常都被捕获，如发生异常，请查看日志，此类会返回null
     *
     * 注意，此方法返回后，自行解析响应资源，解析完成之后
     * 资源并不会被释放，请调用EntityUtils.consume(response.getEntity());释放连接资源
     *
     * @param httpRequest HttpRequestBase类型的请求，不使用HttpRequest的原因是太过底层
     * @return CloseableHttpResponse 类型的原始请求结果 如果发生异常，则返回null。
     */
    public CloseableHttpResponse sendSynHttpRequest(@NotNull HttpRequestBase httpRequest)
    {
        Assert.notNull(httpRequest, "需要发送的请求为null");

        CloseableHttpResponse response = null;
        logger.debug("httpclient向：" + httpRequest.getURI() + "发送" + httpRequest.getMethod() + "请求：" + httpRequest.toString());
        try
        {
            response = client.execute(httpRequest);
            logger.debug("通讯正常，远程回复：" + httpRequest.toString());
        }
        catch (ClientProtocolException e)
        {
            logger.error("客户端协议异常，远程响应出错,尝试增加http://再次访问", e);
            try
            {
                httpRequest.setURI(new URI("http://"+httpRequest.getURI().toString()));
                response = client.execute(httpRequest);
            }
            catch (URISyntaxException | IOException e1)
            {
                logger.error("再次访问客户端协议异常，远程响应出错", e);
                return null;
            }
        }
        catch (IOException e)
        {
            logger.error("发生io异常，远程响应出错", e);
        }
        return response;
    }

    public CloseableHttpResponse sendSynGet(String url)
    {
        //请求对象
        HttpGet get;

        // 响应对象
        CloseableHttpResponse response;

        //构造请求对象
        try
        {
            get = new HttpGet(url);
        }
        catch (IllegalArgumentException e)
        {
            logger.error("构造Get请求时，传入了非法的参数，请求失败",e);
            return null;
        }

        //发送请求
        response = sendSynHttpRequest(get);

        //获得请求
        if (response != null)
        {
            return response;
        }

        return null;
    }

    public CloseableHttpResponse sendSynPost(String url, @Nullable HttpEntity entity, @Nullable List<Header> headers)
    {

        //请求对象
        HttpPost post ;

        // 响应对象
        CloseableHttpResponse response;

        try
        {
            post = new HttpPost(url);
        }
        catch (IllegalArgumentException e)
        {
            try
            {
                logger.debug("构造Post请求时，传入了非法的参数，可能是没有协议，增加默认http协议，重试");
                post = new HttpPost("http://" + url);
            }
            catch (IllegalArgumentException e2)
            {
                logger.error("构造Post请求时，传入了非法的参数，请求失败", e);
                return null;
            }
        }
        if (entity != null)
        {
            post.setEntity(entity);
        }
        if (headers != null && headers.size() > 0)
        {
            for (Header header : headers)
            {
                post.setHeader(header);
            }
        }

        //发送请求
        response = sendSynHttpRequest(post);

        //获得请求
        if (response != null)
        {
            return response;
        }

        return null;
    }

    @Deprecated
    public static String getAsyn()
    {
        return null;
    }

    /*
     *******************************请求结果转换方法*************************************************
     */

    /**
     * 将响应的响应实体，转化为二进制数组
     * 如果响应行为null，则返回null，如果响应行的状态码大于400或为0，返回null,打印日志
     * 如果获取响应体为null了，返回null，打印日志
     *
     * @param response 响应
     * @return 返回响应实体中的数据，以byte[]数组处理
     * @throws IllegalArgumentException 参数必须保证 response不为null
     */
    public static byte[] responseToBytes(CloseableHttpResponse response)
    {
        Assert.notNull(response, "需要转化的响应实体为null");

        //请求行null和错误码处理
        try
        {
            StatusLine line = response.getStatusLine();
            if (line.getStatusCode() >= 400 || line.getStatusCode() == 0)
            {
                logger.error("响应发生错误！状态码为：" + line.getStatusCode());
                return null;
            }
        }
        catch (NullPointerException e)
        {
            logger.error("获取响应行时，空指针异常，获取到 HttpEntity.getStatusLine()为null", e);
            return null;
        }

        //获取请求实体byte流
        try
        {
            return EntityUtils.toByteArray(response.getEntity());
        }
        catch (NullPointerException e)
        {
            logger.error("获取请求结果时，空指针异常，获取到 HttpEntity.getContent()为null", e);
        }
        catch (IOException e)
        {
            logger.error("获取请求结果时，发生io异常，解析结果失败", e);
        }
        catch (ParseException e)
        {
            logger.error("转换异常，响应结果无法使用默认的字符集UTF-8解析为字符串", e);
        }
        finally
        {
            try
            {
                logger.debug("释放资源：" + response.getEntity());
                EntityUtils.consume(response.getEntity());
            }
            catch (IOException e)
            {
                logger.error("释放资源时发生异常", e);
            }
        }

        return null;
    }

    public static String responseToString(CloseableHttpResponse response, Charset charset)
    {
        byte[] bytes = responseToBytes(response);
        Assert.notNull(bytes,"获得的二进制流为null");

        try
        {
            return new String(bytes, charset);
        }
        catch (Exception e)
        {
            logger.error("使用字符集"+charset.name()+"转换出错！",e);
            return null;
        }
    }

    public static JSONObject responseToJSONObject(CloseableHttpResponse response, Charset charset)
    {
        try
        {
            return JSON.parseObject(responseToString(response,charset));
        }
        catch (JSONException e)
        {
            logger.error("此json不是一个合法的json对象，请尝试使用sendGetSynJSONArray()方法转换，或者检查json",e);
            return null;
        }
    }

    public static JSONArray responseToJSONArray(CloseableHttpResponse response, Charset charset)
    {
        try
        {
            return JSON.parseArray(responseToString(response,charset));
        }
        catch (JSONException e)
        {
            logger.error("此json不是一个合法的json数组，请尝试使用sendGetSynJSONObject()方法转换，或者检查json",e);
            return null;
        }
    }

    public static <T> T responseToObject(CloseableHttpResponse response, Class<T> clazz, Charset charset)
    {
        JSONObject jsonObject = responseToJSONObject(response,charset);
        if (jsonObject != null)
        {
            try
            {
                return jsonObject.toJavaObject(clazz);
            }
            catch (Exception e)
            {
                return null;
            }
        }
        return null;
    }

    public static <T> List<T> responseToList(CloseableHttpResponse response, Class<T> clazz, Charset charset)
    {
        JSONArray jsonArray = responseToJSONArray(response,charset);
        if (jsonArray != null)
        {
            try
            {
                return jsonArray.toJavaList(clazz);
            }
            catch (Exception e)
            {
                return null;
            }
        }
        return null;
    }

    /*
     *******************************发送获得结果转换方法，主要工具方法*****************************************
     */

    /**
     * 向指定的URL发送一个同步请求，阻塞等待响应，直到获得结果
     * 获得的结果不会被解析，而是作为byte[]返回
     * 或者在指定了超时时间后放弃
     * 或者在重试若干次后放弃
     * @see HttpUtil#sendSynHttpRequest(HttpRequestBase)
     * @see HttpUtil#responseToBytes(CloseableHttpResponse)
     * @param url 请求地址
     * @return 二进制流，以提供下一步操作
     */
    public byte[] sendSynGetBytes(String url)
    {
        CloseableHttpResponse response =  sendSynGet(url);
        if (response != null)
        {
            return responseToBytes(response);
        }
        else
        {
            return null;
        }
    }

    /**
     * 向指定的URL发送一个同步请求，阻塞等待响应，直到获得结果
     * 获得的结果会被直接解析成为String字符串形式，解析出错不会抛出异常，而是返回null
     * 或者在指定了超时时间后放弃
     * 或者在重试若干次后放弃
     * @see HttpUtil#sendSynGetBytes(String)
     * @param url 请求地址
     * @return 相应的数据使用指定的字符集转换的结果
     */
    public String sendSynGetString(String url,Charset charset)
    {
        CloseableHttpResponse response =  sendSynGet(url);
        if (response != null)
        {
            return responseToString(response,charset);
        }
        else
        {
            return null;
        }
    }

    /**
     * 向指定的URL发送一个同步请求，阻塞等待响应，直到获得结果
     * 获得的结果会使用utf-8被直接解析成为String字符串形式，解析出错不会抛出异常，而是返回null
     * 或者在指定了超时时间后放弃
     * 或者在重试若干次后放弃
     * @see HttpUtil#sendSynGetBytes(String)
     * @param url 请求地址
     * @return 相应的数据使用指定的字符集转换的结果
     */
    public String sendSynGetString(String url)
    {
        return sendSynGetString(url,DEFAULT_CHARSET);
    }

    /**
     * 当且仅当 请求实体的返回的字符串是
     * 一个可以被解析的json对象，非数组时，返回此JSONObject
     * 否则返回null
     * 注意，默认使用utf-8字符集解析此字符串
     * 注意，默认使用fastjson解析json
     * @see HttpUtil#sendSynGetString(String)
     * @param url
     * @return
     */
    public JSONObject sendSynGetJSONObject(String url,Charset charset)
    {
        CloseableHttpResponse response =  sendSynGet(url);
        if (response != null)
        {
            return responseToJSONObject(response,charset);
        }
        else
        {
            return null;
        }
    }

    public JSONObject sendSynGetJSONObject(String url)
    {
        return sendSynGetJSONObject(url,DEFAULT_CHARSET);
    }

    /**
     * 当且仅当 请求实体的返回的字符串是
     * 一个可以被解析的json数组，非对象，返回此JSONObject
     * 否则返回null
     * 注意，默认使用utf-8字符集解析此字符串
     * 注意，默认使用fastjson解析json
     * @see HttpUtil#sendSynGetString(String)
     * @param url
     * @return
     */
    public JSONArray sendSynGetJSONArray(String url,Charset charset)
    {
        CloseableHttpResponse response =  sendSynGet(url);
        if (response != null)
        {
            return responseToJSONArray(response,charset);
        }
        else
        {
            return null;
        }
    }

    public JSONArray sendGetSynJSONArray(String url)
    {
        return sendSynGetJSONArray(url,DEFAULT_CHARSET);
    }

    /**
     * @param clazz
     * @param url
     * @param <T>
     * @return
     */
    public <T> T sendSynGetObject(Class<T> clazz, String url,Charset charset)
    {
        CloseableHttpResponse response =  sendSynGet(url);
        if (response != null)
        {
            return responseToObject(response,clazz,charset);
        }
        else
        {
            return null;
        }
    }

    public <T> T sendSynGetObject(Class<T> clazz, String url)
    {
        return sendSynGetObject(clazz,url,DEFAULT_CHARSET);
    }


    public String sendSynJsonStrPostStr(String url,String requestJson)
    {
        CloseableHttpResponse response = sendSynPost(url,new StringEntity(requestJson,JSON_CONTENT_TYPE),null);
        if (response != null)
        {
            return responseToString(response,DEFAULT_CHARSET);
        }
        return null;
    }

    public <T> T  sendSynJsonStrPostObject(String url,String requestJson,Class<T> clazz)
    {
        CloseableHttpResponse response = sendSynPost(url,new StringEntity(requestJson,JSON_CONTENT_TYPE),null);
        if (response != null)
        {
            return responseToObject(response,clazz,DEFAULT_CHARSET);
        }
        return null;
    }
}
