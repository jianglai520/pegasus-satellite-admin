import java.util.Arrays;
import java.util.List;

/**
 * 美国卫星子类。
 *
 * <p>美国卫星额外记录“运营性质”（政府、军方、商业），用于展示子类
 * 可以在继承公共属性的基础上增加自己的差异化字段。</p>
 *
 * @author FM
 * @version 1.0
 */
public class UsSatellite extends Satellite {

    private static final List<String> OPERATOR_TYPES =
            Arrays.asList("政府", "军方", "商业");

    private final String operatorType;

    /**
     * 构造一颗美国卫星。
     *
     * @param cosparId         必须以 US 开头的 COSPARID
     * @param name             卫星名称
     * @param orbitRadius      轨道半径
     * @param functionAndNotes 功能与注意事项
     * @param operatorType     运营性质：政府、军方或商业
     * @throws BusinessException 参数不符合规则时抛出
     */
    public UsSatellite(String cosparId, String name, double orbitRadius,
                       String functionAndNotes, String operatorType) throws BusinessException {
        super(cosparId, name, orbitRadius, functionAndNotes);
        String normalizedId = cosparId == null ? "" : cosparId.trim().toUpperCase();
        if (!normalizedId.startsWith("US")) {
            throw new BusinessException("美国卫星的 COSPARID 必须以 US 开头。");
        }
        if (operatorType == null || !OPERATOR_TYPES.contains(operatorType.trim())) {
            throw new BusinessException("运营性质只能是：政府、军方、商业。");
        }
        this.operatorType = operatorType.trim();
    }

    @Override
    public String getCountryCode() {
        return "US";
    }

    @Override
    public String getCountryName() {
        return "美国";
    }

    @Override
    public String getAgencyName() {
        return "飞马空军航天司令部";
    }

    @Override
    public String getSpecificInfo() {
        return "运营性质：" + operatorType;
    }
}
