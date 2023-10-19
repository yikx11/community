package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}/")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${quniu.bucket.header.url}")
    private String headerBucketUrl;


    //将头像上传到云服务器上
    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        // 上传文件名称
        String fileName = CommunityUtil.generateUUID();
        // 设置响应信息
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);

        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);

        return "/site/setting";
    }

    // 更新头像路径
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空!");
        }

        String url = headerBucketUrl + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(), url);

        return CommunityUtil.getJSONString(0);
    }

    // 废弃
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadImage(MultipartFile headerImage, Model model) //这个headerImage是前端里表单提交上来的，所以前端input里name要和headerImage同名
    {
        if(headerImage == null)
        {
            model.addAttribute("error","您还没选择图片！");
            return "/site/setting";
        }
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if(StringUtils.isBlank(suffix))
        {
            model.addAttribute("error","文件格式不正确！");
            return "/site/setting";
        }
        //生成随机字符串
        fileName = CommunityUtil.generateUUID() + suffix;
        //保存路径
        File dest = new File(uploadPath + fileName);
        //保存文件
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败"+ e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！",e);
        }
        //更新当前用户头像路径
        //http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(),headerUrl);

        return "redirect:/index";
    }

    // 废弃
    //由于是二进制读取图片信息，需要手动对得到的值进行处理
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET) //获取图片
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response)
    {
        //服务器存放路径
        fileName = uploadPath + "/" + fileName;
        //文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf(".")+1);
        //响应图片
        response.setContentType("image/"+suffix);

        try (
                FileInputStream fis = new FileInputStream(fileName);//从存放路径读出文件
                OutputStream os = response.getOutputStream();//设置一个读入流写入读出的东西
                )//用这样的try结构就可以自动在finally阶段将文件读取工具关闭
        {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != 0)
            {
                os.write(buffer, 0, b);
            }

        } catch (IOException e) {
            logger.error("读取头像失败：" + e.getMessage());
        }

    }

    @LoginRequired
    @RequestMapping(path = "/change", method = RequestMethod.POST)
    public String changePassword(String originPassword, String newPassword, Model model)
    {
        User user = hostHolder.getUser();
        Map<String,Object> map =userService.changePassword(originPassword, newPassword, user.getId());
        if(map.isEmpty())
        {
            return "redirect:/logout";
        }
        else
        {
            model.addAttribute("oldPasswordMsg", map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg", map.get("newPasswordMsg"));
            return "/site/setting";
        }

    }

    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model)
    {
        User user = userService.findUserById(userId);
        if(user == null)
        {
            throw new RuntimeException("该用户不存在！");
        }
        int count = likeService.findUserLikeCount(userId);
        model.addAttribute("user",user);
        model.addAttribute("count",count);

        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }



}
