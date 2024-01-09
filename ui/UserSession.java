package ui;

import client.User;

// 用户会话类
public class UserSession {
    final User user; // 用户对象
    int msgCnt; // 未读消息数量

    /**
     * 构造列表项
     * @param user 用户对象
     */
    public UserSession(User user) {
        this.user = user;
        msgCnt = 0;
    }

    /**
     * ui列表中展示的字符串生成
     * @return 用于展示的字符串
     */
    public String toString() {
        if (msgCnt != 0) return String.format("%d条新消息 - %s (%d)", msgCnt, user.name, user.id);
        else return String.format("%s (%d)", user.name, user.id);
    }
}
