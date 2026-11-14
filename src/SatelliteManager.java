import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 卫星业务管理类。
 *
 * <p>负责维护“当前在用卫星”和“已归档卫星”两个 ArrayList，并集中实现
 * 注册校验、轨道冲突检查、COSPARID 唯一性、名称唯一性、查询、激活、
 * 封锁、改名、功能维护、报废归档等业务规则。界面层只调用本类的方法，
 * 不直接操作集合，从而保证业务规则不会被绕过。</p>
 *
 * <p>两个 ArrayList 的初始容量均为 10；构造对象时会预加载 5 颗卫星。</p>
 *
 * @author FM
 * @version 1.0
 */
public final class SatelliteManager {

    private static final double EPSILON = 1e-9;

    /** 当前在用卫星（含激活与封锁两种状态）。 */
    private final List<Satellite> satellites;

    /** 已归档卫星（状态为 DELETE）。 */
    private final List<Satellite> archivedSatellites;

    /** 历史已分配过的 COSPARID，用于防止报废编号被再次使用。 */
    private final Set<String> issuedCosparIds;

    /**
     * 构造管理器，初始化集合容量并预加载示例卫星。
     */
    public SatelliteManager() {
        this.satellites = new ArrayList<Satellite>(10);
        this.archivedSatellites = new ArrayList<Satellite>(10);
        this.issuedCosparIds = new HashSet<String>();
        preloadSatellites();
    }

    /**
     * 预加载 5 颗卫星，满足“程序开始至少 5 颗卫星”的要求。
     *
     * <p>预加载数据覆盖中国、美国、俄罗斯三个国家，轨道半径依次为
     * 1.20、1.40、1.70、2.00、2.30，任意两颗的间距都不小于 0.2。</p>
     */
    private void preloadSatellites() {
        try {
            register(new ChinaSatellite("CN0001", "飞马号-0001", 1.20,
                    "飞马星球首颗通信试验卫星，负责洲际电报中继；太阳耀斑期间应关闭高增益天线。",
                    "通信"));
            register(new ChinaSatellite("CN0002", "飞马号-0002", 1.40,
                    "气象观测卫星，镜头需定期除尘；每季度校准一次辐射计。",
                    "遥感"));
            register(new UsSatellite("US0001", "PEGASUS-0001", 1.70,
                    "导航定位试验星，为飞马洋航线提供定位服务；注意原子钟温度控制。",
                    "政府"));
            register(new UsSatellite("US0002", "PEGASUS-0002", 2.00,
                    "商业遥感卫星，图像数据按次计费；云层覆盖率超过八成时暂停成像。",
                    "商业"));
            register(new RussiaSatellite("RU0001", "SAT-0001", 2.30,
                    "极地通信卫星，冬季需关注蓄电池保温；每半年检查一次太阳能帆板展开机构。",
                    8));
        } catch (BusinessException e) {
            throw new IllegalStateException("预加载卫星数据失败：" + e.getMessage(), e);
        }
    }

    /**
     * 注册一颗新卫星。
     *
     * <p>依次校验：COSPARID 是否已被使用（含历史归档编号）、名称是否
     * 与现有卫星重复、轨道间距是否小于 0.2。全部通过后才加入集合。</p>
     *
     * @param satellite 待注册的卫星对象
     * @throws BusinessException 违反任一业务规则时抛出
     */
    public void register(Satellite satellite) throws BusinessException {
        if (satellite == null) {
            throw new BusinessException("卫星对象不能为空。");
        }
        String cosparId = satellite.getCosparId();
        if (issuedCosparIds.contains(cosparId)) {
            throw new BusinessException("COSPARID " + cosparId
                    + " 已被使用（含已归档编号），不能再次分配。");
        }
        for (Satellite existing : satellites) {
            if (existing.getName().equalsIgnoreCase(satellite.getName())) {
                throw new BusinessException("卫星名称“" + satellite.getName()
                        + "”已存在，请更换名称。");
            }
            double gap = Math.abs(existing.getOrbitRadius() - satellite.getOrbitRadius());
            if (gap < Satellite.MIN_ORBIT_SPACING - EPSILON) {
                throw new BusinessException(String.format(
                        "轨道冲突：与 %s（%s）的轨道间距仅 %.2f，小于最小间距 %.1f。",
                        existing.getCosparId(), existing.getName(), gap,
                        Satellite.MIN_ORBIT_SPACING));
            }
        }
        satellites.add(satellite);
        issuedCosparIds.add(cosparId);
    }

    /**
     * 按 COSPARID 精确查找当前在用卫星。
     *
     * @param cosparId COSPARID，大小写不敏感
     * @return 匹配的卫星；未找到返回 {@code null}
     */
    public Satellite findByCosparId(String cosparId) {
        String key = normalizeCosparId(cosparId);
        for (Satellite satellite : satellites) {
            if (satellite.getCosparId().equals(key)) {
                return satellite;
            }
        }
        return null;
    }

    /**
     * 在“当前在用”和“已归档”两个集合中按 COSPARID 查找。
     *
     * @param cosparId COSPARID，大小写不敏感
     * @return 匹配的卫星；未找到返回 {@code null}
     */
    public Satellite findAnyByCosparId(String cosparId) {
        Satellite current = findByCosparId(cosparId);
        if (current != null) {
            return current;
        }
        String key = normalizeCosparId(cosparId);
        for (Satellite satellite : archivedSatellites) {
            if (satellite.getCosparId().equals(key)) {
                return satellite;
            }
        }
        return null;
    }

    /**
     * 按 COSPARID 或名称进行模糊搜索。
     *
     * @param keyword 关键字，大小写不敏感
     * @return 所有匹配的当前卫星；无匹配时返回空列表
     */
    public List<Satellite> search(String keyword) {
        List<Satellite> result = new ArrayList<Satellite>();
        String key = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            return result;
        }
        for (Satellite satellite : satellites) {
            boolean idMatched = satellite.getCosparId()
                    .toLowerCase(Locale.ROOT).contains(key);
            boolean nameMatched = satellite.getName()
                    .toLowerCase(Locale.ROOT).contains(key);
            if (idMatched || nameMatched) {
                result.add(satellite);
            }
        }
        return result;
    }

    /**
     * 修改卫星名称，并检查新名称是否与其它在用卫星重复。
     *
     * @param cosparId 目标卫星的 COSPARID
     * @param newName  新名称
     * @throws BusinessException 卫星不存在、名称为空、重名或状态不允许修改时抛出
     */
    public void rename(String cosparId, String newName) throws BusinessException {
        Satellite target = requireSatellite(cosparId);
        if (newName == null || newName.trim().isEmpty()) {
            throw new BusinessException("卫星名称不能为空。");
        }
        String candidate = newName.trim();
        for (Satellite other : satellites) {
            if (other != target && other.getName().equalsIgnoreCase(candidate)) {
                throw new BusinessException("卫星名称“" + candidate + "”已存在，请更换名称。");
            }
        }
        target.rename(candidate);
    }

    /**
     * 修改卫星的功能与注意事项。
     *
     * @param cosparId          目标卫星的 COSPARID
     * @param functionAndNotes  新内容
     * @throws BusinessException 卫星不存在、内容为空或状态不允许修改时抛出
     */
    public void updateFunctionAndNotes(String cosparId, String functionAndNotes)
            throws BusinessException {
        requireSatellite(cosparId).updateFunctionAndNotes(functionAndNotes);
    }

    /**
     * 封锁卫星。
     *
     * @param cosparId 目标卫星的 COSPARID
     * @throws BusinessException 卫星不存在或状态不允许封锁时抛出
     */
    public void deactivate(String cosparId) throws BusinessException {
        requireSatellite(cosparId).deactivate();
    }

    /**
     * 激活已封锁的卫星。
     *
     * @param cosparId 目标卫星的 COSPARID
     * @throws BusinessException 卫星不存在或状态不允许激活时抛出
     */
    public void activate(String cosparId) throws BusinessException {
        requireSatellite(cosparId).activate();
    }

    /**
     * 报废卫星：先标记归档，再从在用集合移除并放入归档集合。
     *
     * <p>移除后该卫星的轨道立即释放，可被后续注册的卫星使用；
     * 但 COSPARID 仍保留在 {@code issuedCosparIds} 中，永久不可复用。</p>
     *
     * @param cosparId 目标卫星的 COSPARID
     * @throws BusinessException 卫星不存在或状态不允许归档时抛出
     */
    public void archiveAndDelete(String cosparId) throws BusinessException {
        Satellite target = requireSatellite(cosparId);
        target.archive();
        satellites.remove(target);
        archivedSatellites.add(target);
    }

    /**
     * 获取当前在用卫星的只读列表。
     *
     * @return 不可修改的当前卫星列表
     */
    public List<Satellite> getAllSatellites() {
        return Collections.unmodifiableList(new ArrayList<Satellite>(satellites));
    }

    /**
     * 获取当前处于封锁状态的卫星列表。
     *
     * @return 不可修改的失活卫星列表
     */
    public List<Satellite> getDeactivatedSatellites() {
        List<Satellite> result = new ArrayList<Satellite>();
        for (Satellite satellite : satellites) {
            if (satellite.getStatus() == SatelliteStatus.DEACTIVATED) {
                result.add(satellite);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 获取已归档卫星的只读列表。
     *
     * @return 不可修改的归档卫星列表
     */
    public List<Satellite> getArchivedSatellites() {
        return Collections.unmodifiableList(new ArrayList<Satellite>(archivedSatellites));
    }

    /**
     * 获取当前在用卫星数量。
     *
     * @return 在用卫星数量
     */
    public int getCurrentCount() {
        return satellites.size();
    }

    /**
     * 获取已归档卫星数量。
     *
     * @return 归档卫星数量
     */
    public int getArchivedCount() {
        return archivedSatellites.size();
    }

    /**
     * 获取历史已分配 COSPARID 数量。
     *
     * @return 历史已分配编号数量
     */
    public int getIssuedCount() {
        return issuedCosparIds.size();
    }

    /**
     * 判断 COSPARID 是否已被使用（含已归档编号）。
     *
     * <p>供界面层在注册前提前提示，真正的唯一性校验仍在
     * {@link #register(Satellite)} 中执行。</p>
     *
     * @param cosparId 待检查的 COSPARID
     * @return 已使用返回 {@code true}
     */
    public boolean isCosparIdUsed(String cosparId) {
        return issuedCosparIds.contains(normalizeCosparId(cosparId));
    }

    /**
     * 查找与指定轨道半径冲突的卫星。
     *
     * @param orbitRadius 待检查的轨道半径
     * @return 第一条冲突卫星；无冲突返回 {@code null}
     */
    public Satellite findOrbitConflict(double orbitRadius) {
        for (Satellite satellite : satellites) {
            double gap = Math.abs(satellite.getOrbitRadius() - orbitRadius);
            if (gap < Satellite.MIN_ORBIT_SPACING - EPSILON) {
                return satellite;
            }
        }
        return null;
    }

    /**
     * 判断名称是否已被当前在用卫星使用（大小写不敏感）。
     *
     * @param name 待检查的名称
     * @return 已存在返回 {@code true}
     */
    public boolean isNameUsed(String name) {
        if (name == null) {
            return false;
        }
        String candidate = name.trim();
        for (Satellite satellite : satellites) {
            if (satellite.getName().equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按 COSPARID 获取当前在用卫星，不存在时抛出业务异常。
     *
     * @param cosparId COSPARID
     * @return 匹配的卫星
     * @throws BusinessException 未找到时抛出
     */
    private Satellite requireSatellite(String cosparId) throws BusinessException {
        Satellite satellite = findByCosparId(cosparId);
        if (satellite == null) {
            throw new BusinessException("未找到 COSPARID 为 " + normalizeCosparId(cosparId)
                    + " 的在用卫星（可能从未注册或已归档）。");
        }
        return satellite;
    }

    /**
     * 规范化 COSPARID：去除首尾空白并转为大写。
     *
     * @param cosparId 原始输入
     * @return 规范化后的 COSPARID
     */
    private String normalizeCosparId(String cosparId) {
        return cosparId == null ? "" : cosparId.trim().toUpperCase(Locale.ROOT);
    }
}
