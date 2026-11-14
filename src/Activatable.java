/**
 * “可激活”行为契约。
 *
 * <p>实现该接口的卫星可以在激活可用与封锁非激活两种状态之间切换。
 * 接口只描述“能做什么”，具体的状态校验与时间戳记录由实现类完成。</p>
 *
 * @author FM
 * @version 1.0
 */
public interface Activatable {

    /**
     * 将卫星从封锁状态恢复为激活可用状态。
     *
     * @throws BusinessException 当前状态不允许激活时抛出
     */
    void activate() throws BusinessException;

    /**
     * 封锁卫星，使其进入不可用状态，但仍占用原轨道。
     *
     * @throws BusinessException 当前状态不允许封锁时抛出
     */
    void deactivate() throws BusinessException;

    /**
     * 判断卫星当前是否为激活可用状态。
     *
     * @return 激活可用返回 {@code true}，否则返回 {@code false}
     */
    boolean isActivated();
}
