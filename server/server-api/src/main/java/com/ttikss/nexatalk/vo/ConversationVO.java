package com.ttikss.nexatalk.vo;

import com.ttikss.nexatalk.entity.Conversation;

import java.time.LocalDateTime;

/**
 * 会话视图对象
 * 包含会话基本信息及对方用户详情，用于会话列表展示
 */
public class ConversationVO {

    private Long id;
    private Long user1Id;
    private Long user2Id;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;

    // 对方用户信息（用于会话列表展示）
    private Long opponentId;
    private String opponentNickname;
    private String opponentAvatar;
    private String opponentUsername;

    /** 是否置顶 */
    private Boolean isPinned;

    /** 是否为与 AI 好友的会话（用于前端展示「AI好友Talk」及空状态文案） */
    private Boolean isAiConversation;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUser1Id() { return user1Id; }
    public void setUser1Id(Long user1Id) { this.user1Id = user1Id; }

    public Long getUser2Id() { return user2Id; }
    public void setUser2Id(Long user2Id) { this.user2Id = user2Id; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public Integer getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Integer unreadCount) { this.unreadCount = unreadCount; }

    public Long getOpponentId() { return opponentId; }
    public void setOpponentId(Long opponentId) { this.opponentId = opponentId; }

    public String getOpponentNickname() { return opponentNickname; }
    public void setOpponentNickname(String opponentNickname) { this.opponentNickname = opponentNickname; }

    public String getOpponentAvatar() { return opponentAvatar; }
    public void setOpponentAvatar(String opponentAvatar) { this.opponentAvatar = opponentAvatar; }

    public String getOpponentUsername() { return opponentUsername; }
    public void setOpponentUsername(String opponentUsername) { this.opponentUsername = opponentUsername; }

    public Boolean getIsPinned() { return isPinned; }
    public void setIsPinned(Boolean isPinned) { this.isPinned = isPinned; }

    public Boolean getIsAiConversation() { return isAiConversation; }
    public void setIsAiConversation(Boolean isAiConversation) { this.isAiConversation = isAiConversation; }

    /**
     * 将 Conversation 实体转换为 VO
     *
     * @param conversation 会话实体
     * @param currentUserId 当前用户ID（用于确定对方用户）
     * @param opponent 对方用户信息
     * @return 会话视图对象
     */
    public static ConversationVO from(Conversation conversation, Long currentUserId, UserVO opponent) {
        if (conversation == null) return null;
        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setUser1Id(conversation.getUser1Id());
        vo.setUser2Id(conversation.getUser2Id());
        vo.setLastMessage(conversation.getLastMessage());
        vo.setLastMessageAt(conversation.getLastMessageAt());

        // 计算当前用户的未读数
        Integer unreadCount = currentUserId.equals(conversation.getUser1Id())
                ? conversation.getUser1Unread()
                : conversation.getUser2Unread();
        vo.setUnreadCount(unreadCount != null ? unreadCount : 0);

        // 设置对方用户信息
        if (opponent != null) {
            vo.setOpponentId(opponent.getId());
            vo.setOpponentNickname(opponent.getNickname());
            vo.setOpponentAvatar(opponent.getAvatarUrl());
            vo.setOpponentUsername(opponent.getUsername());
        }

        // 设置置顶状态
        vo.setIsPinned(conversation.getIsPinned() != null && conversation.getIsPinned() == 1);

        return vo;
    }
}
