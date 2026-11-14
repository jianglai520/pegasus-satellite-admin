import java.util.List;

/**
 * 程序主类。
 *
 * <p>负责创建界面对象与业务管理对象、显示主菜单、读取用户选择并分派
 * 到各功能方法。菜单使用 {@code do-while} 循环反复显示，用户选择 0 时
 * 通过 {@link System#exit(int)} 干净退出。</p>
 *
 * <p>除 {@code main} 方法外，其余方法均为实例方法，避免滥用 static。</p>
 *
 * @author FM
 * @version 1.0
 */
public class Main {

    private final ConsoleUI ui;
    private final SatelliteManager manager;

    /**
     * 构造主程序，完成界面与业务管理器的初始化。
     */
    public Main() {
        this.ui = new ConsoleUI();
        this.manager = new SatelliteManager();
    }

    /**
     * 程序入口。
     *
     * @param args 命令行参数（本程序不使用）
     */
    public static void main(String[] args) {
        new Main().run();
    }

    /**
     * 运行主循环：显示菜单、读取选择、执行操作，直到用户选择退出。
     */
    private void run() {
        boolean running = true;
        do {
            showMenu();
            int choice = ui.readIntInRange("请选择操作 [0-11]：", 0, 11);
            try {
                running = handleChoice(choice);
            } catch (BusinessException e) {
                ui.error(e.getMessage());
            }
            if (running) {
                ui.pause();
            }
        } while (running);

        ui.printBlank();
        ui.info("感谢使用飞马星球卫星微管理系统，系统已安全退出。");
        System.exit(0);
    }

    /**
     * 显示系统标题、规则提示、统计信息和功能菜单。
     */
    private void showMenu() {
        ui.printBlank();
        ui.printLine("============================================================");
        ui.printLine("            飞马星球卫星微管理系统（FM_320240931231_令小刚）");
        ui.printLine("============================================================");
        ui.info("规则：最低轨道半径 1.2；轨道最小间距 0.2。");
        ui.info("COSPARID = 2 位国家编码 + 4 位数字，分配后不可修改。");
        ui.info("当前在用卫星 " + manager.getCurrentCount() + " 颗，已归档 "
                + manager.getArchivedCount() + " 颗，历史已分配编号 "
                + manager.getIssuedCount() + " 个。");
        ui.printDivider();
        ui.printLine(" 1、显示当前卫星列表");
        ui.printLine(" 2、注册添加新卫星");
        ui.printLine(" 3、查询卫星详细信息（按 COSPARID）");
        ui.printLine(" 4、修改卫星名称");
        ui.printLine(" 5、搜索卫星（按 ID 或名称，支持模糊匹配）");
        ui.printLine(" 6、封锁卫星（封锁后不得修改信息和删除）");
        ui.printLine(" 7、激活卫星（重新启用）");
        ui.printLine(" 8、显示失活卫星列表（已封锁 / 已归档）");
        ui.printLine(" 9、查看和修改卫星功能与注意事项");
        ui.printLine("10、查看卫星注册时间与历次修改记录");
        ui.printLine("11、删除卫星并归档");
        ui.printLine(" 0、退出系统");
        ui.printDivider();
    }

    /**
     * 根据用户选择分派功能。
     *
     * @param choice 菜单编号
     * @return 继续运行返回 {@code true}，退出返回 {@code false}
     * @throws BusinessException 业务规则校验失败时抛出
     */
    private boolean handleChoice(int choice) throws BusinessException {
        switch (choice) {
            case 1:
                showSatelliteList();
                return true;
            case 2:
                registerSatellite();
                return true;
            case 3:
                querySatellite();
                return true;
            case 4:
                renameSatellite();
                return true;
            case 5:
                searchSatellites();
                return true;
            case 6:
                deactivateSatellite();
                return true;
            case 7:
                activateSatellite();
                return true;
            case 8:
                showDeactivatedSatellites();
                return true;
            case 9:
                manageFunctionAndNotes();
                return true;
            case 10:
                showHistory();
                return true;
            case 11:
                deleteSatellite();
                return true;
            case 0:
                return false;
            default:
                ui.error("无效的菜单编号。");
                return true;
        }
    }

    /**
     * 功能 1：显示当前所有在用卫星（含激活与封锁状态）。
     */
    private void showSatelliteList() {
        List<Satellite> satellites = manager.getAllSatellites();
        ui.printLine("\n【当前卫星列表】共 " + satellites.size() + " 颗（含封锁卫星）");
        ui.printSatelliteTable(satellites);
    }

    /**
     * 功能 2：注册新卫星。
     *
     * <p>按“编号—名称—轨道—功能—国家扩展字段”的顺序采集信息，
     * 每一步都提前校验，尽量让用户在输入阶段就得到明确提示。</p>
     *
     * @throws BusinessException 构造或注册卫星失败时抛出
     */
    private void registerSatellite() throws BusinessException {
        ui.printLine("\n---------------- 注册新卫星 ----------------");
        String cosparId;
        while (true) {
            cosparId = ui.readCosparId(
                    "请输入 COSPARID（2 位国家编码 + 4 位数字，例如 CN0006）：");
            if (manager.isCosparIdUsed(cosparId)) {
                ui.error("COSPARID " + cosparId + " 已被使用（含已归档编号），请更换。");
                continue;
            }
            break;
        }

        String countryCode = cosparId.substring(0, 2);
        if (!countryCode.equals("CN") && !countryCode.equals("US")
                && !countryCode.equals("RU")) {
            ui.error("暂不支持国家编码 " + countryCode + "，当前支持 CN / US / RU。");
            return;
        }

        String name;
        while (true) {
            name = ui.readNonEmpty("请输入卫星名称（不超过 30 个字符）：",
                    Satellite.MAX_NAME_LENGTH);
            if (manager.isNameUsed(name)) {
                ui.error("卫星名称“" + name + "”已存在，请更换。");
                continue;
            }
            break;
        }

        double orbitRadius;
        while (true) {
            orbitRadius = ui.readDouble("请输入轨道半径（不小于 1.2）：",
                    Satellite.MIN_ORBIT_RADIUS);
            Satellite conflict = manager.findOrbitConflict(orbitRadius);
            if (conflict == null) {
                break;
            }
            ui.error(String.format(
                    "轨道冲突：与 %s（%s）的间距仅 %.2f，小于最小间距 0.2，请重新输入。",
                    conflict.getCosparId(), conflict.getName(),
                    Math.abs(conflict.getOrbitRadius() - orbitRadius)));
        }

        String functionAndNotes = ui.readNonEmpty(
                "请输入功能与注意事项（不超过 300 字）：", Satellite.MAX_FUNCTION_LENGTH);

        Satellite satellite;
        if (countryCode.equals("CN")) {
            String missionType = readMissionType();
            satellite = new ChinaSatellite(cosparId, name, orbitRadius,
                    functionAndNotes, missionType);
        } else if (countryCode.equals("US")) {
            String operatorType = readOperatorType();
            satellite = new UsSatellite(cosparId, name, orbitRadius,
                    functionAndNotes, operatorType);
        } else {
            int designLife = ui.readIntInRange("请输入设计寿命（1-30 年）：", 1, 30);
            satellite = new RussiaSatellite(cosparId, name, orbitRadius,
                    functionAndNotes, designLife);
        }

        manager.register(satellite);
        ui.success("卫星注册成功，初始状态为 ACTIVATED。");
        ui.printSatelliteDetail(satellite);
    }

    /**
     * 读取中国卫星的任务类型，非法值要求重新输入。
     *
     * @return 合法任务类型
     */
    private String readMissionType() {
        while (true) {
            String value = ui.readLine("请输入任务类型（通信/导航/遥感/科学）：");
            if (value.equals("通信") || value.equals("导航")
                    || value.equals("遥感") || value.equals("科学")) {
                return value;
            }
            ui.error("任务类型只能是：通信、导航、遥感、科学。");
        }
    }

    /**
     * 读取美国卫星的运营性质，非法值要求重新输入。
     *
     * @return 合法运营性质
     */
    private String readOperatorType() {
        while (true) {
            String value = ui.readLine("请输入运营性质（政府/军方/商业）：");
            if (value.equals("政府") || value.equals("军方") || value.equals("商业")) {
                return value;
            }
            ui.error("运营性质只能是：政府、军方、商业。");
        }
    }

    /**
     * 功能 3：按 COSPARID 精确查询详细信息。
     */
    private void querySatellite() {
        String cosparId = ui.readCosparId("请输入要查询的 COSPARID：");
        Satellite satellite = manager.findAnyByCosparId(cosparId);
        if (satellite == null) {
            ui.error("未找到 COSPARID 为 " + cosparId + " 的卫星（可能从未注册）。");
            return;
        }
        if (satellite.isArchived()) {
            ui.info("该卫星已归档，以下为归档信息。");
        }
        ui.printSatelliteDetail(satellite);
    }

    /**
     * 功能 4：修改卫星名称。
     */
    private void renameSatellite() {
        String cosparId = ui.readCosparId("请输入要修改名称的 COSPARID：");
        Satellite satellite = manager.findByCosparId(cosparId);
        if (satellite == null) {
            ui.error("未找到 COSPARID 为 " + cosparId + " 的在用卫星。");
            return;
        }
        if (!satellite.isEditable()) {
            ui.error("卫星当前为 " + satellite.getStatusText()
                    + "，封锁状态下不得修改信息，请先激活。");
            return;
        }
        ui.printLine("当前名称：" + satellite.getName());
        while (true) {
            String newName = ui.readNonEmpty("请输入新名称：", Satellite.MAX_NAME_LENGTH);
            try {
                manager.rename(cosparId, newName);
                ui.success("名称修改成功，新名称为：" + satellite.getName());
                return;
            } catch (BusinessException e) {
                ui.error(e.getMessage());
            }
        }
    }

    /**
     * 功能 5：按 ID 或名称模糊搜索，并显示所有匹配卫星的详细信息。
     */
    private void searchSatellites() {
        String keyword = ui.readNonEmpty(
                "请输入 ID 或名称关键字（支持模糊匹配）：", 30);
        List<Satellite> matches = manager.search(keyword);
        ui.printLine("\n【搜索结果】关键字“" + keyword + "”，共 "
                + matches.size() + " 颗");
        if (matches.isEmpty()) {
            ui.info("没有匹配的卫星。");
            return;
        }
        ui.printSatelliteTable(matches);
        for (Satellite satellite : matches) {
            ui.printBlank();
            ui.printSatelliteDetail(satellite);
        }
    }

    /**
     * 功能 6：封锁卫星。
     *
     * @throws BusinessException 状态不允许封锁时抛出
     */
    private void deactivateSatellite() throws BusinessException {
        String cosparId = ui.readCosparId("请输入要封锁的 COSPARID：");
        Satellite satellite = manager.findByCosparId(cosparId);
        if (satellite == null) {
            ui.error("未找到 COSPARID 为 " + cosparId + " 的在用卫星。");
            return;
        }
        manager.deactivate(cosparId);
        ui.success("卫星 " + cosparId + " 已封锁（DEACTIVATED），仍占用原轨道，"
                + "封锁期间不得修改信息和删除。");
    }

    /**
     * 功能 7：激活已封锁的卫星。
     *
     * @throws BusinessException 状态不允许激活时抛出
     */
    private void activateSatellite() throws BusinessException {
        String cosparId = ui.readCosparId("请输入要激活的 COSPARID：");
        Satellite satellite = manager.findByCosparId(cosparId);
        if (satellite == null) {
            ui.error("未找到 COSPARID 为 " + cosparId + " 的在用卫星。");
            return;
        }
        manager.activate(cosparId);
        ui.success("卫星 " + cosparId + " 已重新激活（ACTIVATED），恢复可用。");
    }

    /**
     * 功能 8：显示失活（封锁）卫星列表和已归档卫星列表。
     */
    private void showDeactivatedSatellites() {
        List<Satellite> deactivated = manager.getDeactivatedSatellites();
        ui.printLine("\n【失活卫星列表（DEACTIVATED）】共 "
                + deactivated.size() + " 颗");
        ui.printSatelliteTable(deactivated);

        List<Satellite> archived = manager.getArchivedSatellites();
        ui.printLine("\n【已归档卫星（DELETE）】共 " + archived.size()
                + " 颗（编号已永久锁定，原轨道已释放）");
        ui.printSatelliteTable(archived);
    }

    /**
     * 功能 9：查看并修改卫星的功能与注意事项。
     *
     * @throws BusinessException 修改失败时抛出
     */
    private void manageFunctionAndNotes() throws BusinessException {
        String cosparId = ui.readCosparId("请输入要查看的 COSPARID：");
        Satellite satellite = manager.findAnyByCosparId(cosparId);
        if (satellite == null) {
            ui.error("未找到 COSPARID 为 " + cosparId + " 的卫星。");
            return;
        }
        ui.printLine("\n---------------- 功能与注意事项 ----------------");
        ui.printLine("COSPARID : " + satellite.getCosparId());
        ui.printLine("名称     : " + satellite.getName());
        ui.printLine("当前状态 : " + satellite.getStatusText());
        ui.printLine("内容     : " + satellite.getFunctionAndNotes());
        int action = ui.readIntInRange("请选择：1 仅查看 / 2 修改 / 0 返回：", 0, 2);
        if (action != 2) {
            return;
        }
        if (!satellite.isEditable()) {
            ui.error("卫星当前为 " + satellite.getStatusText()
                    + "，不允许修改功能与注意事项。");
            return;
        }
        String newText = ui.readNonEmpty(
                "请输入新的功能与注意事项（不超过 300 字）：",
                Satellite.MAX_FUNCTION_LENGTH);
        manager.updateFunctionAndNotes(cosparId, newText);
        ui.success("功能与注意事项修改成功。");
    }

    /**
     * 功能 10：查看注册时间、最近修改时间和历次修改记录。
     */
    private void showHistory() {
        String cosparId = ui.readCosparId("请输入要查看历史的 COSPARID：");
        Satellite satellite = manager.findAnyByCosparId(cosparId);
        if (satellite == null) {
            ui.error("未找到 COSPARID 为 " + cosparId + " 的卫星。");
            return;
        }
        ui.printHistory(satellite);
    }

    /**
     * 功能 11：报废卫星，从在用列表删除并归档。
     *
     * @throws BusinessException 状态不允许删除时抛出
     */
    private void deleteSatellite() throws BusinessException {
        String cosparId = ui.readCosparId("请输入要删除的 COSPARID：");
        Satellite satellite = manager.findByCosparId(cosparId);
        if (satellite == null) {
            ui.error("未找到 COSPARID 为 " + cosparId + " 的在用卫星"
                    + "（可能已归档）。");
            return;
        }
        ui.printSatelliteDetail(satellite);
        boolean confirmed = ui.readYesNo("确认报废并归档该卫星？(y/n)：");
        if (!confirmed) {
            ui.info("已取消删除，卫星仍在原轨道运行。");
            return;
        }
        manager.archiveAndDelete(cosparId);
        ui.success("卫星 " + cosparId + " 已报废归档（DELETE），轨道已释放，"
                + "编号已永久锁定。");
    }
}
