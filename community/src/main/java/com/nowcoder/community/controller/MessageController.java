package com.nowcoder.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {

    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    // 私信列表
    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    public String getLetterList(Model model, Page page)
    {
        // 分页信息
        User user = hostHolder.getUser();
        page.setRows(messageService.findConversationCount(user.getId()));
        page.setLimit(5);
        page.setPath("/letter/list");

        // 会话列表
        List<Message> messagesList = messageService.findConversations(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> messageVo = new ArrayList<>();
        for(Message message : messagesList)
        {
            Map<String, Object> map = new HashMap<>();
            map.put("conversation",message);
            map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
            map.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
            int targetId = message.getFromId() == user.getId() ? message.getToId() : message.getFromId();
            map.put("target", userService.findUserById(targetId));
            messageVo.add(map);
        }

        // 查询未读消息数量
        model.addAttribute("conversations",messageVo);
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);
        return "/site/letter";
    }

    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Model model, Page page)
    {
        // 分页信息
        User user = hostHolder.getUser();
        page.setRows(messageService.findLetterCount(conversationId));
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);

        //私信列表
        List<Message> messagesList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> messageVo = new ArrayList<>();
        //List<Integer> ids = new ArrayList<>();
        int target = 0;
        for(Message message : messagesList)
        {
            Map<String, Object> map = new HashMap<>();
            map.put("letter",message);
            int targetId = message.getFromId();
            map.put("target", userService.findUserById(targetId));
            messageVo.add(map);
            target = message.getFromId() == user.getId() ? message.getToId() : message.getFromId();
            //ids.add(message.getId());
        }
        //messageService.readMessage(ids, 1);
        User tar = userService.findUserById(target);
        model.addAttribute("letters", messageVo);
        model.addAttribute("person", tar);

        // 设置已读
        List<Integer> ids = getLetterIds(messagesList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/site/letter-detail";
    }

    private List<Integer> getLetterIds(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();

        if (letterList != null) {
            for (Message message : letterList) {
                if (hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0) {
                    ids.add(message.getId());
                }
            }
        }

        return ids;
    }


    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName, String content)
    {
        Integer.valueOf("abc");
        if(userService.findUserByName(toName) == null)
        {
            return CommunityUtil.getJSONString(1,"目标用户不存在！");
        }
        Message message = new Message();
        message.setContent(content);
        message.setCreateTime(new Date());
        message.setStatus(0);
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(userService.findUserByName(toName).getId());
        String conversationId = message.getFromId() > message.getToId() ? message.getToId() + "_" + message.getFromId() : message.getFromId() + "_" + message.getToId();
        message.setConversationId(conversationId);
        messageService.addMessage(message);

        return CommunityUtil.getJSONString(0);

    }

    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    public String getNoticeList(Model model)
    {
        User user = hostHolder.getUser();
        //查询评论通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);

        if(message != null)
        {
            Map<String, Object> messageVO = new HashMap<>();
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVO.put("message", message);
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("unread", unread);
            model.addAttribute("commentNotice", messageVO);
        }


        //查询点赞通知

        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);

        if(message != null)
        {
            Map<String, Object> messageVO = new HashMap<>();
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVO.put("message", message);
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVO.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVO.put("unread", unread);
            model.addAttribute("likeNotice", messageVO);
        }

        //查询关注通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        if(message != null)
        {
            Map<String, Object> messageVO = new HashMap<>();
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVO.put("message", message);
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("entityType", data.get("entityType"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("unread", unread);
            model.addAttribute("followNotice", messageVO);
        }

        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/notice";
    }

    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model)
    {
        User user = hostHolder.getUser();
        page.setLimit(5);
        page.setPath("/notice/detail/" + topic);
        page.setRows(messageService.findNoticeCount(user.getId(), topic));

        List<Message> noticeList = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if (noticeList != null) {
            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>();
                // 通知
                map.put("notice", notice);
                // 内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());//转换为json字符串
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);//然后再转换成正常对象
                map.put("user", userService.findUserById((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));
                // 通知作者
                map.put("fromUser", userService.findUserById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);

        // 设置已读
        List<Integer> ids = getLetterIds(noticeList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/site/notice-detail";


    }

}
