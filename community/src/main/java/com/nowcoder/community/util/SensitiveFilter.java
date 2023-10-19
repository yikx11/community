package com.nowcoder.community.util;

import com.nowcoder.community.controller.LoginController;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    //替换符
    private static final String REPLACEMENT = "***";
    //根节点
    private TrieNode rootNode = new TrieNode();

    //当容器SensitiveFilter被调用实例化后，这个方法会自动调用.服务器启动后容器就被初始化了
    @PostConstruct
    public void init()
    {
        try(
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");//从target目录下加载到敏感词文件
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                ) {
                String keyWord;
                while ((keyWord = reader.readLine()) != null)
                {
                    this.addKeyWord(keyWord);
                }

        } catch (IOException e) {
            logger.error("加载敏感词文件失败："+ e.getMessage());
        }

    }

    //将一个敏感词添加到前缀树中
    private void addKeyWord(String keyWord) //由于构建树不需要对外使用此方法，所以方法为私有的
    {
        TrieNode tempNode = rootNode;
        for(int i = 0; i < keyWord.length(); i++)
        {
            char c = keyWord.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            if(subNode == null)
            {
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }
            tempNode = subNode;
            if(i == keyWord.length()-1)
            {
                tempNode.setKeyWordEnd(true);
            }
        }
    }


    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        // 指针1
        TrieNode tempNode = rootNode;
        // 指针2
        int begin = 0;
        // 指针3
        int position = 0;
        // 结果
        StringBuilder sb = new StringBuilder();

        while(begin < text.length())
        {
            if(position < text.length())
            {
                char c = text.charAt(position);
                // 跳过符号
                if (isSymbol(c)) {
                    if (tempNode == rootNode) {
                        begin++;
                        sb.append(c);
                    }
                    position++;
                    continue;
                }
                tempNode = tempNode.getSubNode(c);
                if(tempNode != null)//找到可疑敏感词
                {
                    if(tempNode.isKeyWordEnd)//找到敏感词
                    {
                        sb.append(REPLACEMENT);
                        begin = ++position;
                        tempNode = rootNode;
                    }
                    else
                    {
                        position++;
                    }
                }
                else if(tempNode != rootNode)
                {
                    sb.append(text.charAt(begin));
                    position = ++begin;
                    tempNode = rootNode;
                }
                else
                {
                    begin = ++position;
                    sb.append(c);
                }
            }
            else //begin到position是敏感词前缀，但position已经越界
            {
                sb.append(text.charAt(begin));
                begin++;
            }

        }
        return sb.toString();
    }
    private boolean isSymbol(Character c)
    {
        return !CharUtils.isAsciiAlphanumeric(c) && (c<0x2E80 || c>0x9FFF);
    }

    private class TrieNode //前缀树根节点为空，每个节点只包含一个字符
    {
        //关键词结束标识
        private boolean isKeyWordEnd;

        //前缀树子节点，key是下级字符，value是下级节点
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeyWordEnd() {
            return isKeyWordEnd;
        }

        public void setKeyWordEnd(boolean keyWordEnd) {
            isKeyWordEnd = keyWordEnd;
        }

        //添加子节点
        public void addSubNode(Character c, TrieNode node)
        {
            subNodes.put(c, node);
        }
        //获取子节点
        public TrieNode getSubNode(Character c)
        {
            return subNodes.get(c);
        }
    }
}
