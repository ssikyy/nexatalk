package com.ttikss.nexatalk.service;

import com.ttikss.nexatalk.dto.SendMessageRequest;
import com.ttikss.nexatalk.entity.Conversation;
import com.ttikss.nexatalk.entity.Message;
import com.ttikss.nexatalk.vo.ConversationVO;
import com.ttikss.nexatalk.vo.PageVO;

/**
 * 私信模块业务层接口
 *
 * 会话维度未读计数设计：
 * - conversation.user1_unread / user2_unread 分别记录各自的未读数
 * - 打开会话时调用 readConversation() 清零并标记消息已读
 */
public interface MessageService {

    /**
     * 发送私信（自动创建或复用会话）
     *
     * @param senderId 发送方用户 ID
     * @param request  请求体
     * @return 生成的消息实体
     */
    Message sendMessage(Long senderId, SendMessageRequest request);

    /**
     * 查询我的会话列表（按最后消息时间倒序）
     * 返回 ConversationVO 包含对方用户信息和当前用户的未读数
     *
     * @param userId   当前用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     */
    PageVO<ConversationVO> listMyConversations(Long userId, int page, int pageSize);

    /**
     * 查询某会话的消息记录（按时间升序，适合聊天界面加载）
     *
     * @param userId         当前用户 ID（用于鉴权）
     * @param conversationId 会话 ID
     * @param page           页码
     * @param pageSize       每页大小
     */
    PageVO<Message> listMessages(Long userId, Long conversationId, int page, int pageSize);

    /**
     * 打开会话：标记消息已读 + 清零会话未读计数
     *
     * @param userId         当前用户 ID
     * @param conversationId 会话 ID
     */
    void readConversation(Long userId, Long conversationId);

    /**
     * 统计当前用户所有会话的未读消息总数
     *
     * @param userId 用户 ID
     */
    long countTotalUnread(Long userId);

    /**
     * 删除指定会话（删除会话及其所有消息）
     *
     * @param userId         当前用户 ID
     * @param conversationId 会话 ID
     */
    void deleteConversation(Long userId, Long conversationId);

    /**
     * 清空指定会话的所有消息记录（保留会话）
     *
     * @param userId         当前用户 ID
     * @param conversationId 会话 ID
     */
    void clearMessages(Long userId, Long conversationId);

    /**
     * 撤回指定消息（只能撤回自己发送的消息）
     *
     * @param userId    当前用户 ID
     * @param messageId 消息 ID
     */
    void recallMessage(Long userId, Long messageId);

    /**
     * 删除单条消息
     *
     * @param userId    当前用户 ID
     * @param messageId 消息 ID
     */
    void deleteMessage(Long userId, Long messageId);
}
