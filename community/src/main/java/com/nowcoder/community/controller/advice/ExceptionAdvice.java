package com.nowcoder.community.controller.advice;

import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@ControllerAdvice(annotations = Controller.class)//括号里是只扫描带有controller注解的bean
//controller的异常配置类，对所有controller的错误进行处理
public class ExceptionAdvice {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler({Exception.class})
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("服务器发生异常: " + e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
        }

        String xRequestedWith = request.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1, "服务器异常!"));
        } else {
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }
}

//通过 request.getHeader("x-requested-with") 获取请求头中的 "x-requested-with" 字段的值，该字段用于标识请求是异步请求还是正常请求。
//使用条件语句判断请求类型是否为异步请求，即判断 "x-requested-with" 字段的值是否为 "XMLHttpRequest"。
//如果是异步请求，通过设置响应的内容类型为 "application/plain;charset=utf-8"，表示响应内容为纯文本，使用 response.setContentType() 方法进行设置。
//获取响应的 PrintWriter 对象，通过 response.getWriter() 方法获取。
//使用 writer.write() 方法将自定义的错误信息写入响应中。在这里，使用 CommunityUtil.getJSONString(1, "服务器异常！") 生成一个表示错误信息的 JSON 字符串，并将其写入响应中。
//如果是正常请求，使用 response.sendRedirect() 方法将请求重定向到 "/error" 路径。这个路径可以是应用程序中的错误处理页面或错误提示页面。
//通过这段代码，根据请求的类型，将不同类型的错误信息返回给客户端。对于异步请求，返回一个包含错误信息的 JSON 字符串；对于正常请求，将请求重定向到错误处理页面或错误提示页面，以展示更友好的错误信息给用户。