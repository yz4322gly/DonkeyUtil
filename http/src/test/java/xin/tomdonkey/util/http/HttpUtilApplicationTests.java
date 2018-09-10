package xin.tomdonkey.util.http;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;
import xin.tomdonkey.util.http.common.utils.HttpUtil;

@RunWith(SpringRunner.class)
@SpringBootTest
public class HttpUtilApplicationTests
{

    @Autowired
    HttpUtil httpUtil;

	@Test
	public void contextLoads()
	{
        System.out.println(httpUtil);
        System.out.println(httpUtil);
        System.out.println(httpUtil.sendSynGetString("www.baidu.com"));
	}

}
