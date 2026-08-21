package com.lulala.langchain4j.utils;

import cn.hutool.core.util.IdUtil;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ID 生成器 —— 整合滴滴（号段模式）和美团（Snowflake 增强版）两种方案
 * <pre>
 * 滴滴 TinyId 号段模式：预分配号段 [start, end]，用完切换下一段
 * 美团 Leaf-Snowflake：标准 64 位雪花算法，含时钟回拨保护
 * </pre>
 * 只是想在一个 Spring Boot 单体应用中生成 ID，直接用项目里这份手写的 IdWorker 就够可以了，
 * 后续服务多实例需要协调 workerId，再考虑引入：
 * <pre>
 * 滴滴 TinyId 号段模式：https://github.com/didi/tinyid
 * 美团 Leaf-Snowflake：https://github.com/Meituan-Dianping/Leaf
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
                    // 双重检查：确认号段确实耗尽（可能已被其他线程切换）
                    if (cursor.get() > currentEnd) {
                        // 如果有预加载的下一段，直接切换
                        if (nextStart != -1) {
                            // 注意：先保存再清空，避免 preloadNextSegment() 并发覆盖
                            long ns = nextStart;
                            long ne = nextEnd;
                            nextStart = -1;
                            nextEnd = -1;
                            currentStart = ns;
                            currentEnd = ne;
                            // 不调用 cursor.set()：号段耗尽时 cursor 已自增到 currentEnd+1，
                            // 而预加载的 nextStart 理应 == currentEnd+1，cursor 已在正确位置。
                            // 若用 set() 回退 cursor，会与锁外已取走 ID 的线程产生重复。
                        } else {
                            // 没有预加载，同步生成新号段（模拟 DB 拉取）
                            long newStart = currentEnd + 1;
                            long newEnd = newStart + step - 1;
                            currentStart = newStart;
                            currentEnd = newEnd;
                            // 同样不调用 cursor.set()，理由同上
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        /**
         * 预加载下一号段（模拟异步从 DB 拉取，实际应放在定时任务或回调中调用）
         * <p>
         * 注意：通过加锁保证与 {@link #nextId()} 切换号段时的可见性和原子性，
         * 防止 nextStart/nextEnd 被读到不配对的值。
         * </p>
         */
        public void preloadNextSegment(long start, long end) {
            lock.lock();
            try {
                this.nextStart = start;
                this.nextEnd = end;
            } finally {
                lock.unlock();
            }
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
                // 短暂让出 CPU，避免忙等消耗
                try {
                    Thread.sleep(0, 500_000); // 0.5ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
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

    /**
     * 真正上生产，用`IdUtil.getSnowflake()`更省心<br/>
     * 雪花算法，跟 nextMeituanId 为同一档方案
     * @return long
     * @author shenjh
     * @since 2026/8/21 11:29
     */
    public static long getSnowflakeNextId() {
        return IdUtil.getSnowflakeNextId();
    }

    /** Hutool 工具类，生成纳秒级 ID（字符串格式） */
    public static String nextNanoId() {
        return IdUtil.nanoId();
    }

    /*
        ## 结论：`nextMeituanId()` 更符合你的要求，且它与 `IdUtil.getSnowflake()` 是同一档的方案

        先把你提的要求拆成两条硬指标逐项对照：

        | 指标 | `nextDidiId()`（号段模式） | `nextMeituanId()`（雪花模式） | Hutool `IdUtil.getSnowflake()` |
        |---|---|---|---|
        | 微服务多实例不重复 | ❌ 不满足（demo 实现无跨实例协调） | ✅ 满足（workerId 区分节点，最多 1024 个） | ✅ 满足（workerId + dataCenterId） |
        | 时钟回拨不重复 | ✅ 完全免疫（根本不读时钟） | ✅ 满足（≤5ms 等待，>5ms 抛异常拒绝） | ✅ 满足（同款策略） |
        | 重启后不重复 | ❌ 不满足（重启后游标重置） | ✅ 满足（时间戳趋势递增） | ✅ 满足 |
        | 成熟度/开箱即用 | 玩具级（无 DB 持久化） | 手写但逻辑完整 | ✅ 久经考验，支持 nextIdStr 等 |

        ### `nextDidiId()` 为什么不达标

        号段模式对时钟回拨**天然免疫**（[IdWorker.java](file:///C:/git/demo/LangChain4j-demo/src/main/java/com/lulala/langchain4j/utils/IdWorker.java#L37-L124) 里全程没碰系统时钟），但这份 demo 实现有两处硬伤：

        1. **多实例必重复**：`DIDI` 是写死的 `new DidiSegmentGenerator(1, 1000, 1000)`（[第 256 行](file:///C:/git/demo/LangChain4j-demo/src/main/java/com/lulala/langchain4j/utils/IdWorker.java#L256)），号段耗尽时也只是本地 `currentEnd + 1` 续段（[第 95 行](file:///C:/git/demo/LangChain4j-demo/src/main/java/com/lulala/langchain4j/utils/IdWorker.java#L95)）。微服务里每个实例都会从 1 开始发号，互相不知道对方发到哪了，必然撞 ID。真正的滴滴 TinyId 是靠**中心化 DB 统一切分号段**来保证各实例拿到的区间不重叠。
        2. **重启即重复**：没有把游标/号段落库，进程重启后 `cursor` 回到 1，重新发一遍 1~1000。

        所以号段模式本身是好方案，但**这份手写实现目前只配在单体 + 不重启的场景下用**，与"微服务"前提冲突。

        ### `nextMeituanId()` 为什么达标

        它的回拨策略（[第 179-199 行](file:///C:/git/demo/LangChain4j-demo/src/main/java/com/lulala/langchain4j/utils/IdWorker.java#L179-L199)）与 Hutool 5.8.46 `cn.hutool.core.util.Snowflake` 的源码逻辑完全同源：**回拨 ≤5ms 等待追平继续发号，>5ms 直接拒绝生成**——宁可短暂不可用，也绝不发重复号。同时 10-bit workerId 内嵌在 ID 里，各实例分配不同 workerId 即可互不干扰，这正是微服务场景需要的。

        ### 与 Hutool 的横向比较

        - `nextMeituanId()` ≈ `IdUtil.getSnowflake(workerId, dataCenterId)`：两者等价，Hutool 版还多几个便利点（`nextIdStr()`、`isUseSystemClock` 缓存时钟参数、双维度机器标识），要上生产直接用它即可。
        - `IdUtil.objectId()`：计数器兜底，回拨也免疫，是除雪花外第二个"回拨不重复"的 Hutool 选项，但它是字符串且非严格递增，做数据库主键不如雪花友好。
        - `IdUtil.nanoId()` / UUID：无时钟依赖，但无序，不符合"微服务 ID"通常想要的趋势递增特性。

        **一句话总结**：手写方案里选 `nextMeituanId()`；要真正上生产，用等价的 `IdUtil.getSnowflake()` 更省心。
     */

    public static void main(String[] args) {
        // didi: 1
        System.out.println("didi: " + nextDidiId());
        // meituan: 57479593916895232
        System.out.println("meituan: " + nextMeituanId());
    }
}
