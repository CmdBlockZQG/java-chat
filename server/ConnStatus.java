package server;

// 描述连接接收数据包的状态
class ConnStatus {
    // 数据包接收超时时间，接收单个数据包超过这个时间连接将会强制断开
    private static final long TIMEOUT = 5000;
    private boolean receiving = false; // 是否正在接收数据包
    private long startTime; // 本次接收开始的时间

    /**
     * 查询当前状态是否意味着连接已经超时
     * @return 是否超时
     */
    public synchronized boolean isTimeout() {
        long curTime = System.currentTimeMillis();
        return receiving && (curTime - startTime > TIMEOUT);
    }

    /**
     * 设置接收状态
     * @param receive true:开始接收 false:接收结束
     */
    public synchronized void setStatus(boolean receive) {
        if (receive) {
            receiving = true;
            startTime = System.currentTimeMillis();
        } else {
            receiving = false;
        }
    }
}