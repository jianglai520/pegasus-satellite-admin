import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 卫星抽象父类。
 *
 * <p>该类抽取了所有国家卫星共有的属性与行为：COSPARID、名称、轨道半径、
 * 状态、功能与注意事项、注册时间、最近修改时间、归档时间以及修改历史。
 * 其中 COSPARID 在构造时初始化并被 {@code final} 修饰，不提供 setter，
 * 保证分配后不可修改；状态的每一次变更都必须经过本类的方法，并由系统
 * 自动写入时间戳和修改记录。</p>
 *
 * <p>不同国家的卫星通过继承本类实现，并实现
 * {@link #getCountryCode()}、{@link #getCountryName()}、
 * {@link #getAgencyName()}、{@link #getSpecificInfo()} 四个抽象方法，
 * 从而在不修改父类和业务层的前提下扩展新的国家卫星类型。</p>
 *
 * @author FM
 * @version 1.0
 */
public abstract class Satellite implements Activatable, Archivable {

    /** 最低卫星轨道半径。 */
    public static final double MIN_ORBIT_RADIUS = 1.2;

    /** 卫星轨道之间的最小间距。 */
    public static final double MIN_ORBIT_SPACING = 0.2;

    /** 卫星名称允许的最大长度。 */
    public static final int MAX_NAME_LENGTH = 30;

    /** 功能与注意事项允许的最大长度。 */
    public static final int MAX_FUNCTION_LENGTH = 300;

    private static final Pattern COSPAR_PATTERN = Pattern.compile("^[A-Z]{2}\\d{4}$");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** COSPARID：构造时初始化，final 修饰，无 setter，分配后不可修改。 */
    private final String cosparId;

    /** 卫星名称：可以通过 {@link #rename(String)} 修改。 */
    private String name;

    /** 轨道半径：注册时确定，之后不再提供修改入口，避免破坏轨道间距规则。 */
    private final double orbitRadius;

    /** 当前状态：只能通过本类的激活、封锁、归档方法变更。 */
    private SatelliteStatus status;

    /** 功能与注意事项：可以通过 {@link #updateFunctionAndNotes(String)} 修改。 */
    private String functionAndNotes;

    /** 注册时间：由系统在构造时自动产生，final 修饰。 */
    private final LocalDateTime registeredAt;

    /** 最近一次修改时间：由系统在每次变更时自动刷新。 */
    private LocalDateTime lastModifiedAt;

    /** 归档时间：报废归档时由系统自动写入。 */
    private LocalDateTime archivedAt;

    /** 修改历史：只允许本类内部追加，对外返回只读视图。 */
    private final List<ModificationRecord> modificationHistory;

    /**
     * 构造一颗卫星并完成公共字段的校验与初始化。
     *
     * @param cosparId        6 位 COSPARID，格式为 2 位大写国家编码 + 4 位数字
     * @param name            卫星名称
     * @param orbitRadius     轨道半径，不得小于 {@link #MIN_ORBIT_RADIUS}
     * @param functionAndNotes 功能与注意事项
     * @throws BusinessException 任一参数不符合规则时抛出
     */
    protected Satellite(String cosparId, String name, double orbitRadius,
                        String functionAndNotes) throws BusinessException {
        String normalizedId = cosparId == null ? "" : cosparId.trim().toUpperCase();
        if (!COSPAR_PATTERN.matcher(normalizedId).matches()) {
            throw new BusinessException(
                    "COSPARID 格式错误：必须是 2 位大写字母国家编码 + 4 位数字，例如 CN0001。");
        }
        if (!Double.isFinite(orbitRadius) || orbitRadius < MIN_ORBIT_RADIUS) {
            throw new BusinessException(String.format(
                    "轨道半径不得小于 %.1f（当前输入为 %.2f）。", MIN_ORBIT_RADIUS, orbitRadius));
        }

        this.cosparId = normalizedId;
        this.name = validateName(name);
        this.orbitRadius = orbitRadius;
        this.functionAndNotes = validateFunctionAndNotes(functionAndNotes);
        this.status = SatelliteStatus.ACTIVATED;
        this.registeredAt = LocalDateTime.now();
        this.lastModifiedAt = this.registeredAt;
        this.archivedAt = null;
        this.modificationHistory = new ArrayList<ModificationRecord>();
        this.modificationHistory.add(new ModificationRecord(
                this.registeredAt, "注册", "卫星注册成功，初始状态为 ACTIVATED。"));
    }

    /**
     * 校验并规范化卫星名称。
     *
     * @param value 原始名称
     * @return 去除首尾空白后的名称
     * @throws BusinessException 名称为空或超长时抛出
     */
    private String validateName(String value) throws BusinessException {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException("卫星名称不能为空。");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new BusinessException("卫星名称不能超过 " + MAX_NAME_LENGTH + " 个字符。");
        }
        return trimmed;
    }

    /**
     * 校验并规范化功能与注意事项。
     *
     * @param value 原始内容
     * @return 去除首尾空白后的内容
     * @throws BusinessException 内容为空或超长时抛出
     */
    private String validateFunctionAndNotes(String value) throws BusinessException {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException("功能与注意事项不能为空。");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_FUNCTION_LENGTH) {
            throw new BusinessException(
                    "功能与注意事项不能超过 " + MAX_FUNCTION_LENGTH + " 个字符。");
        }
        return trimmed;
    }

    /**
     * 校验当前状态是否允许执行修改类操作。
     *
     * <p>封锁状态的卫星不得修改信息和删除；已归档的卫星也不允许再修改。</p>
     *
     * @param operation 操作名称，用于拼接错误提示
     * @throws BusinessException 当前状态不允许修改时抛出
     */
    private void requireEditable(String operation) throws BusinessException {
        if (status == SatelliteStatus.DELETE) {
            throw new BusinessException("卫星已归档，不能" + operation + "。");
        }
        if (status == SatelliteStatus.DEACTIVATED) {
            throw new BusinessException(
                    "卫星处于封锁状态，不能" + operation + "；请先激活该卫星。");
        }
    }

    /**
     * 修改卫星名称。
     *
     * @param newName 新名称
     * @throws BusinessException 名称为空、与原名相同或当前状态不允许修改时抛出
     */
    public void rename(String newName) throws BusinessException {
        requireEditable("修改名称");
        String validated = validateName(newName);
        if (validated.equals(this.name)) {
            throw new BusinessException("新名称与当前名称相同，无需修改。");
        }
        String oldName = this.name;
        this.name = validated;
        recordModification("修改名称", "“" + oldName + "” → “" + validated + "”");
    }

    /**
     * 修改卫星的功能与注意事项。
     *
     * @param newFunctionAndNotes 新内容
     * @throws BusinessException 内容为空、与原文相同或当前状态不允许修改时抛出
     */
    public void updateFunctionAndNotes(String newFunctionAndNotes) throws BusinessException {
        requireEditable("修改功能与注意事项");
        String validated = validateFunctionAndNotes(newFunctionAndNotes);
        if (validated.equals(this.functionAndNotes)) {
            throw new BusinessException("新的功能与注意事项与原文相同，无需修改。");
        }
        this.functionAndNotes = validated;
        recordModification("修改功能与注意事项", "更新为：“" + validated + "”");
    }

    /**
     * 激活卫星，使其恢复为可用状态。
     *
     * @throws BusinessException 卫星已激活或已归档时抛出
     */
    @Override
    public void activate() throws BusinessException {
        if (status == SatelliteStatus.ACTIVATED) {
            throw new BusinessException("卫星已处于激活可用状态，无需重复激活。");
        }
        if (status == SatelliteStatus.DELETE) {
            throw new BusinessException("卫星已归档，不能重新激活。");
        }
        status = SatelliteStatus.ACTIVATED;
        recordModification("激活", "卫星由 DEACTIVATED 恢复为 ACTIVATED，重新投入使用。");
    }

    /**
     * 封锁卫星，使其进入不可用状态，但仍占用原轨道。
     *
     * @throws BusinessException 卫星已封锁或已归档时抛出
     */
    @Override
    public void deactivate() throws BusinessException {
        if (status == SatelliteStatus.DEACTIVATED) {
            throw new BusinessException("卫星已处于封锁状态，无需重复封锁。");
        }
        if (status == SatelliteStatus.DELETE) {
            throw new BusinessException("卫星已归档，不能执行封锁操作。");
        }
        status = SatelliteStatus.DEACTIVATED;
        recordModification("封锁", "卫星由 ACTIVATED 变为 DEACTIVATED，仍占用原轨道。");
    }

    /**
     * 报废并归档卫星。
     *
     * <p>封锁状态的卫星不得直接删除，必须先激活；归档后状态为 DELETE，
     * 归档时间由系统自动记录，COSPARID 由管理器永久保留、不得再次使用。</p>
     *
     * @throws BusinessException 卫星已归档或处于封锁状态时抛出
     */
    @Override
    public void archive() throws BusinessException {
        if (status == SatelliteStatus.DELETE) {
            throw new BusinessException("卫星已经归档，不能重复归档。");
        }
        if (status == SatelliteStatus.DEACTIVATED) {
            throw new BusinessException("封锁状态的卫星不得直接删除，请先激活后再报废。");
        }
        status = SatelliteStatus.DELETE;
        this.archivedAt = LocalDateTime.now();
        recordModification("报废归档",
                "卫星状态标记为 DELETE，轨道已释放，COSPARID 永久保留、不得再次使用。");
    }

    /**
     * 追加一条修改记录，并刷新最近修改时间。
     *
     * <p>时间由系统调用 {@link LocalDateTime#now()} 自动产生，方法为私有，
     * 界面层无法传入自定义时间，从而保证时间信息不可人工篡改。</p>
     *
     * @param action 修改事项
     * @param detail 修改说明
     */
    private void recordModification(String action, String detail) {
        LocalDateTime now = LocalDateTime.now();
        this.lastModifiedAt = now;
        this.modificationHistory.add(new ModificationRecord(now, action, detail));
    }

    /**
     * 获取 COSPARID。
     *
     * @return COSPARID
     */
    public String getCosparId() {
        return cosparId;
    }

    /**
     * 获取卫星名称。
     *
     * @return 卫星名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取轨道半径。
     *
     * @return 轨道半径
     */
    public double getOrbitRadius() {
        return orbitRadius;
    }

    /**
     * 获取当前状态枚举。
     *
     * @return 当前状态
     */
    public SatelliteStatus getStatus() {
        return status;
    }

    /**
     * 获取“枚举名（中文说明）”形式的状态文本。
     *
     * @return 状态文本，例如 {@code ACTIVATED（激活可用）}
     */
    public String getStatusText() {
        return status.name() + "（" + status.getLabel() + "）";
    }

    /**
     * 获取功能与注意事项。
     *
     * @return 功能与注意事项
     */
    public String getFunctionAndNotes() {
        return functionAndNotes;
    }

    /**
     * 获取注册时间。
     *
     * @return 注册时间
     */
    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    /**
     * 获取最近一次修改时间。
     *
     * @return 最近修改时间
     */
    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * 获取归档时间。
     *
     * @return 归档时间；未归档时返回 {@code null}
     */
    @Override
    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    /**
     * 获取修改历史的只读视图。
     *
     * <p>返回 {@link Collections#unmodifiableList(List)} 包装后的列表，
     * 外部只能读取，不能直接增删记录。</p>
     *
     * @return 不可修改的修改记录列表
     */
    public List<ModificationRecord> getModificationHistory() {
        return Collections.unmodifiableList(modificationHistory);
    }

    /**
     * 获取修改记录条数。
     *
     * @return 修改记录条数
     */
    public int getModificationCount() {
        return modificationHistory.size();
    }

    /**
     * 判断卫星是否处于激活可用状态。
     *
     * @return 激活可用返回 {@code true}
     */
    @Override
    public boolean isActivated() {
        return status == SatelliteStatus.ACTIVATED;
    }

    /**
     * 判断卫星是否已归档。
     *
     * @return 已归档返回 {@code true}
     */
    @Override
    public boolean isArchived() {
        return status == SatelliteStatus.DELETE;
    }

    /**
     * 判断卫星当前是否允许修改信息。
     *
     * @return 仅激活状态返回 {@code true}
     */
    public boolean isEditable() {
        return status == SatelliteStatus.ACTIVATED;
    }

    /**
     * 格式化时间，供界面层统一显示。
     *
     * @param time 待格式化的时间，允许为 {@code null}
     * @return 格式化文本；{@code null} 返回 {@code "-"}
     */
    public String formatTime(LocalDateTime time) {
        return time == null ? "-" : TIME_FORMAT.format(time);
    }

    /**
     * 获取两位国家编码。
     *
     * @return 国家编码，例如 {@code CN}
     */
    public abstract String getCountryCode();

    /**
     * 获取国家名称。
     *
     * @return 国家名称，例如 {@code 中国}
     */
    public abstract String getCountryName();

    /**
     * 获取所属航天机构名称。
     *
     * @return 机构名称
     */
    public abstract String getAgencyName();

    /**
     * 获取该国家卫星特有的扩展信息。
     *
     * <p>不同子类返回不同的内容，用于在详细信息中展示差异化字段。</p>
     *
     * @return 扩展信息文本
     */
    public abstract String getSpecificInfo();
}
