import java.util.Arrays;
import java.util.List;

/**
 * 中国卫星子类。
 *
 * <p>在中国卫星的扩展实现中，额外记录“任务类型”（通信、导航、遥感、科学），
 * 用于体现不同国家可以根据自身需求扩展父类。类本身只负责国家差异，
 * 公共的注册、状态、历史等逻辑全部继承自 {@link Satellite}。</p>
 *
 * @author FM
 * @version 1.0
 */
public class ChinaSatellite extends Satellite {

    private static final List<String> MISSION_TYPES =
            Arrays.asList("通信", "导航", "遥感", "科学");

    private final String missionType;

    /**
     * 构造一颗中国卫星。
     *
     * @param cosparId         必须以 CN 开头的 COSPARID
     * @param name             卫星名称
     * @param orbitRadius      轨道半径
     * @param functionAndNotes 功能与注意事项
     * @param missionType      任务类型：通信、导航、遥感或科学
     * @throws BusinessException 参数不符合规则时抛出
     */
    public ChinaSatellite(String cosparId, String name, double orbitRadius,
                          String functionAndNotes, String missionType) throws BusinessException {
        super(cosparId, name, orbitRadius, functionAndNotes);
        String normalizedId = cosparId == null ? "" : cosparId.trim().toUpperCase();
        if (!normalizedId.startsWith("CN")) {
            throw new BusinessException("中国卫星的 COSPARID 必须以 CN 开头。");
        }
        if (missionType == null || !MISSION_TYPES.contains(missionType.trim())) {
            throw new BusinessException("任务类型只能是：通信、导航、遥感、科学。");
        }
        this.missionType = missionType.trim();
    }

    @Override
    public String getCountryCode() {
        return "CN";
    }

    @Override
    public String getCountryName() {
        return "中国";
    }

    @Override
    public String getAgencyName() {
        return "飞马国家航天局";
    }

    @Override
    public String getSpecificInfo() {
        return "任务类型：" + missionType;
    }
}
