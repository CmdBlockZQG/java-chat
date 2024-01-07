package client;

public class User {
    public final int id; // 用户id
    public final String name; // 用户名

    /**
     * 构造用户对象
     * @param userId 用户id
     * @param userName 用户名
     */
    public User(int userId, String userName) {
        id = userId;
        name = userName;
    }

    /**
     * 将用户对象转为字符串，供调试输出
     * @return 结果字符串
     */
    public String toString() {
        return String.format("ID: %d, Name: %s", id, name);
    }
}
