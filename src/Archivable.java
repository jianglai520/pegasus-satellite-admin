import java.time.LocalDateTime;

/**
 * “可归档”行为契约。
 *
 * <p>卫星报废后必须先从在用集合中删除、释放轨道，再把对象归档保存，
 * 并把状态标记为 {@code DELETE}。实现该接口的类负责完成状态变更和
 * 归档时间记录。</p>
 *
 * @author FM
 * @version 1.0
 */
public interface Archivable {

    /**
     * 将卫星标记为报废归档状态。
     *
     * @throws BusinessException 当前状态不允许归档时抛出
     */
    void archive() throws BusinessException;

    /**
     * 判断卫星是否已经归档。
     *
     * @return 已归档返回 {@code true}，否则返回 {@code false}
     */
    boolean isArchived();

    /**
     * 获取归档时间。
     *
     * @return 归档时间；未归档时返回 {@code null}
     */
    LocalDateTime getArchivedAt();
}
