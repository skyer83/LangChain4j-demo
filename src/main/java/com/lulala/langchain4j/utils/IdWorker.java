package com.lulala.langchain4j.utils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ID 生成器 —— 整合滴滴（号段模式）和美团（Snowflake 增强版）两种方案
 * <pre>
 * 滴滴 TinyId 号段模式：预分配号段 [start, end]，用完切换下一段
 * 美团 Leaf-Snowflake：标准 64 位雪花算法，含时钟回拨保护
 * </pre>
 *
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 14:28
 */
public class IdWorker {

    // ======================== 滴滴 TinyId 号段模式 ========================

    /**
     * 滴滴号段 ID 生成器（线程安全）
     * <p>
     * 核心思路：一次性从"数据库"拉取一个号段 [start, end]，
     * 本地以 CAS 自增分配，号段耗尽时切换下一段。
     * 这里省略实际 DB 交互，用方法参数模拟。
     * </p>
     */
    public static class DidiSegmentGenerator {
        /** 当前号段起点 */
        private volatile long currentStart;
        /** 当前号段终点 */
        private volatile long currentEnd;
        /** 预加载的下一号段起点 */
        private volatile long nextStart = -1;
        /** 预加载的下一号段终点 */
        private volatile long nextEnd = -1;
        /** 当前已分配到的游标 */
        private final AtomicLong cursor = new AtomicLong(0);
        /** 切换号段时的锁 */
        private final ReentrantLock lock = new ReentrantLock();
        /** 号段步长 */
        private final long step;

        public DidiSegmentGenerator(long start, long end, long step) {
            this.currentStart = start;
            this.currentEnd = end;
            this.step = step;
            this.cursor.set(start);
        }

        /**
         * 获取下一个 ID（滴滴号段模式）
         */
        public long nextId() {
            while (true) {
                long current = cursor.get();
                long max = currentEnd;

                // 当前号段还未用完
                if (current <= max) {
                    if (cursor.compareAndSet(current, current + 1)) {
                        return current;
                    }
                    continue;
                }

                // 号段用完，尝试切换
                lock.lock();
                try {
                    // 双重检查
                    if (cursor.get() > currentEnd) {
                        // 如果有预加载的下一段，直接切换
                        if (nextStart != -1) {
                            currentStart = nextStart;
                            currentEnd = nextEnd;
                            cursor.set(currentStart);
                            nextStart = -1;
                            nextEnd = -1;
                        } else {
                            // 没有预加载，同步生成新号段（模拟 DB 拉取）
                            long newStart = currentEnd + 1;
                            long newEnd = newStart + step - 1;
                            currentStart = newStart;
                            currentEnd = newEnd;
                            cursor.set(newStart);
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        /**
         * 预加载下一号段（模拟异步从 DB 拉取，实际应放在定时任务或回调中调用）
         */
        public void preloadNextSegment(long start, long end) {
            this.nextStart = start;
            this.nextEnd = end;
        }
    }

    // ======================== 美团 Leaf-Snowflake ========================

    /**
     * 美团 Leaf-Snowflake ID 生成器（线程安全）
     * <p>
     * 64 位长整型结构：
     * ┌─┬─────────────────────────────┬────────────────┬──────────────┐
     * │0│      41-bit 时间戳 (ms)      │ 10-bit 机器ID  │ 12-bit 序列号 │
     * └─┴─────────────────────────────┴────────────────┴──────────────┘
     * </p>
     * <p>
     * 美团版增强点：
     * 1. workerId 通过 ZooKeeper 自动注册（这里简化为构造传入）
     * 2. 遇到时钟回拨时自旋等待，而非直接抛异常
     * </p>
     */
    public static class MeituanSnowflakeGenerator {
        /** 自定义起始时间戳（2026-01-01 00:00:00） */
        private static final long START_TIMESTAMP = 1767196800000L;
        /** 机器 ID 所占位数 */
        private static final long WORKER_ID_BITS = 10L;
        /** 序列号所占位数 */
        private static final long SEQUENCE_BITS = 12L;
        /** 最大机器 ID */
        private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
        /** 序列号掩码 */
        private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
        /** 时间戳左移位数 */
        private static final long TIMESTAMP_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;
        /** 机器 ID 左移位数 */
        private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

        private final long workerId;
        private long sequence = 0L;
        /** 上次生成 ID 的时间戳 */
        private volatile long lastTimestamp = -1L;
        /** 时钟回拨容忍阈值（毫秒） */
        private static final long CLOCK_BACKWARD_TOLERANCE = 5L;

        public MeituanSnowflakeGenerator(long workerId) {
            if (workerId > MAX_WORKER_ID || workerId < 0) {
                throw new IllegalArgumentException(
                        "workerId 必须在 [0, " + MAX_WORKER_ID + "] 之间，当前值: " + workerId);
            }
            this.workerId = workerId;
        }

        /**
         * 获取下一个 ID（美团 Snowflake 模式）
         */
        public synchronized long nextId() {
            long currentTimestamp = System.currentTimeMillis();

            // 时钟回拨处理：如果在容忍范围内则自旋等待
            if (currentTimestamp < lastTimestamp) {
                long offset = lastTimestamp - currentTimestamp;
                if (offset <= CLOCK_BACKWARD_TOLERANCE) {
                    try {
                        // 等待追上之前的时间
                        Thread.sleep(offset + 1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("时钟回拨等待被中断", e);
                    }
                    currentTimestamp = System.currentTimeMillis();
                    if (currentTimestamp < lastTimestamp) {
                        throw new RuntimeException(
                                "时钟回拨超限: 上次=" + lastTimestamp + ", 当前=" + currentTimestamp);
                    }
                } else {
                    throw new RuntimeException(
                            "时钟回拨超限: 上次=" + lastTimestamp + ", 当前=" + currentTimestamp);
                }
            }

            // 同一毫秒内
            if (currentTimestamp == lastTimestamp) {
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0) {
                    // 序列号用完，等待下一毫秒
                    currentTimestamp = waitNextMillis(lastTimestamp);
                }
            } else {
                // 不同毫秒，序列号归零
                sequence = 0L;
            }

            lastTimestamp = currentTimestamp;

            return ((currentTimestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                    | (workerId << WORKER_ID_SHIFT)
                    | sequence;
        }

        /**
         * 自旋等待下一毫秒
         */
        private long waitNextMillis(long lastTimestamp) {
            long timestamp = System.currentTimeMillis();
            while (timestamp <= lastTimestamp) {
                timestamp = System.currentTimeMillis();
            }
            return timestamp;
        }

        /**
         * 从 ID 中反解出时间戳
         */
        public static long extractTimestamp(long id) {
            return (id >> TIMESTAMP_SHIFT) + START_TIMESTAMP;
        }

        /**
         * 从 ID 中反解出机器 ID
         */
        public static long extractWorkerId(long id) {
            return (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
        }
    }

    // ======================== 便捷入口 ========================

    /** 滴滴号段生成器示例 */
    private static final DidiSegmentGenerator DIDI = new DidiSegmentGenerator(1, 1000, 1000);
    /** 美团 Snowflake 生成器示例（workerId=1） */
    private static final MeituanSnowflakeGenerator MEITUAN = new MeituanSnowflakeGenerator(1);

    /** 生成滴滴风格 ID */
    public static long nextDidiId() {
        return DIDI.nextId();
    }

    /** 生成美团风格 ID */
    public static long nextMeituanId() {
        return MEITUAN.nextId();
    }

    public static void main(String[] args) {
        System.out.println("didi: " + nextDidiId());
        System.out.println("meituan: " + nextMeituanId());
    }
}
