/**
 * 卫星状态枚举。
 *
 * <p>作业规定卫星只有三种状态：激活可用、封锁非激活、报废归档。
 * 使用枚举而不是字符串常量，可以在编译期避免非法状态值。</p>
 *
 * @author FM
 * @version 1.0
 */
public enum SatelliteStatus {

    /** 激活可用状态。 */
    ACTIVATED("激活可用"),

    /** 封锁非激活状态，卫星仍占用原轨道。 */
    DEACTIVATED("封锁非激活"),

    /** 报废归档状态，卫星已从在用列表移除。 */
    DELETE("报废归档");

    private final String label;

    SatelliteStatus(String label) {
        this.label = label;
    }

    /**
     * 获取状态的中文说明。
     *
     * @return 中文说明
     */
    public String getLabel() {
        return label;
    }
}
