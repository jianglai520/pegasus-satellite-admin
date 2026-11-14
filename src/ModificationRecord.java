import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 不可变的修改记录。
 *
 * <p>每一条记录由系统在状态或信息发生变更时自动创建，包含时间、
 * 修改事项和具体说明。类中所有字段均为 {@code final}，且不提供任何
 * setter，因此记录一旦创建就不能被外部篡改。</p>
 *
 * @author FM
 * @version 1.0
 */
public final class ModificationRecord {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LocalDateTime time;
    private final String action;
    private final String detail;

    /**
     * 构造一条修改记录。
     *
     * @param time   记录时间，由系统自动传入
     * @param action 修改事项，例如“注册”“修改名称”
     * @param detail 修改内容的详细说明
     */
    public ModificationRecord(LocalDateTime time, String action, String detail) {
        this.time = time;
        this.action = action;
        this.detail = detail;
    }

    /**
     * 获取记录时间。
     *
     * @return 记录时间
     */
    public LocalDateTime getTime() {
        return time;
    }

    /**
     * 获取修改事项。
     *
     * @return 修改事项
     */
    public String getAction() {
        return action;
    }

    /**
     * 获取修改内容说明。
     *
     * @return 修改内容说明
     */
    public String getDetail() {
        return detail;
    }

    /**
     * 获取格式化后的记录时间。
     *
     * @return {@code yyyy-MM-dd HH:mm:ss} 格式的时间文本
     */
    public String getTimeText() {
        return TIME_FORMAT.format(time);
    }

    @Override
    public String toString() {
        return "[" + getTimeText() + "] " + action + "：" + detail;
    }
}
