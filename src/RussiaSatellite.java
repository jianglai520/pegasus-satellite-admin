/**
 * 俄罗斯卫星子类。
 *
 * <p>俄罗斯卫星额外记录“设计寿命”（年），用于展示新增一个国家卫星时，
 * 只需继承父类并补充本国特有字段，无需改动父类和业务管理类。</p>
 *
 * @author FM
 * @version 1.0
 */
public class RussiaSatellite extends Satellite {

    private static final int MIN_DESIGN_LIFE = 1;
    private static final int MAX_DESIGN_LIFE = 30;

    private final int designLifeYears;

    /**
     * 构造一颗俄罗斯卫星。
     *
     * @param cosparId         必须以 RU 开头的 COSPARID
     * @param name             卫星名称
     * @param orbitRadius      轨道半径
     * @param functionAndNotes 功能与注意事项
     * @param designLifeYears  设计寿命（年），取值 1 到 30
     * @throws BusinessException 参数不符合规则时抛出
     */
    public RussiaSatellite(String cosparId, String name, double orbitRadius,
                           String functionAndNotes, int designLifeYears) throws BusinessException {
        super(cosparId, name, orbitRadius, functionAndNotes);
        String normalizedId = cosparId == null ? "" : cosparId.trim().toUpperCase();
        if (!normalizedId.startsWith("RU")) {
            throw new BusinessException("俄罗斯卫星的 COSPARID 必须以 RU 开头。");
        }
        if (designLifeYears < MIN_DESIGN_LIFE || designLifeYears > MAX_DESIGN_LIFE) {
            throw new BusinessException(
                    "设计寿命必须在 " + MIN_DESIGN_LIFE + " 到 " + MAX_DESIGN_LIFE + " 年之间。");
        }
        this.designLifeYears = designLifeYears;
    }

    @Override
    public String getCountryCode() {
        return "RU";
    }

    @Override
    public String getCountryName() {
        return "俄罗斯";
    }

    @Override
    public String getAgencyName() {
        return "飞马航天办事处";
    }

    @Override
    public String getSpecificInfo() {
        return "设计寿命：" + designLifeYears + " 年";
    }
}
