package com.scaffolding.sophia.common.feign.config;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.scaffolding.sophia.common.util.HttpCallOtherInterfaceUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

/**
 * @author: LHL
 * @ProjectName: sophia_scaffolding
 * @Package: com.scaffolding.sophia.common.feign
 * @ClassName: FeignRequestInterceptorConfig
 * @Description:
 * @Version: 1.0
 */
@Component
public class FeignRequestInterceptorConfig implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignRequestInterceptorConfig.class);

    private final String AUTHORIZATION_HEADER = "Authorization";

    @Value("${gateway.url}")
    private String url;

    /**
     * 内部
     */
    public final String FROM_IN = "Y";

    /**
     * 标志
     */
    public final String FROM = "from";

    @Value("${security.oauth2.client.client-id}")
    private String clientId;

    @Value("${security.oauth2.client.client-secret}")
    private String clientSecret;


    /**
     * Create a template with the header of provided name and extracted extract
     * 1. 如果使用 非web 请求，header 区别
     * 2. 根据authentication 还原请求token
     *
     * @param template
     */
    @Override
    public void apply(RequestTemplate template) {
        Collection<String> fromHeader = template.headers().get(FROM);
        if (CollUtil.isNotEmpty(fromHeader) && fromHeader.contains(FROM_IN)) {
            log.debug("内部调用feign");
            String s = "?client_id=" + clientId + "&client_secret=" + clientSecret + "&grant_type=client_credentials&scope=all";
            String sr = HttpCallOtherInterfaceUtils.callOtherInterface(url, "/api/auth/oauth/token" + s);
            Map srmap = JSON.parseObject(sr);
            if (null == srmap) {
                log.debug("内部调用feign传递失败");
                return;
            }
            String access_token = (String) srmap.get("access_token");
            System.out.println(access_token);
            template.header(AUTHORIZATION_HEADER, "Bearer " + access_token);
            return;
        }
        log.debug("外部调用feign");
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        if (request != null) {
            log.debug("调用feign传递header携带token");
            //只携带token
            String authorization = request.getHeader(AUTHORIZATION_HEADER);
            //requestTemplate.header("Authorization", authorization);
            log.debug("Authorization :\t\t" + authorization);
            //携带全部
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    String values = request.getHeader(name);
                    template.header(name, values);
                    log.debug("name ：\t\t" + name);
                    log.debug("values ： \t\t" + values);
                }
            }
        }
    }
}