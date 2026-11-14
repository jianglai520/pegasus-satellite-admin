import java.io.Console;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * 字符界面输入输出工具类。
 *
 * <p>集中封装控制台输入校验、表格对齐、详细信息与历史记录排版，
 * 避免在主类中重复编写输入输出代码。所有输入方法都会在非法输入时
 * 给出提示并允许用户重新输入。</p>
 *
 * @author FM
 * @version 1.0
 */
public class ConsoleUI {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String DIVIDER =
            "--------------------------------------------------------------------------------";

    private final Scanner scanner;

    /**
     * 构造界面工具，自动识别控制台编码，保证 Windows GBK 与 UTF-8 终端下中文输入都正常。
     */
    public ConsoleUI() {
        this.scanner = new Scanner(
                new InputStreamReader(System.in, detectInputCharset()));
    }

    /**
     * 推断标准输入应使用的字符集。
     *
     * <p>优先使用 {@code -Dstdin.encoding} 显式指定的编码；否则尝试通过反射
     * 调用 Java 17+ 的 {@code Console.charset()}；再退回 JDK 的 native 编码、
     * JVM 文件系统编码或系统默认编码。Java 8 没有 {@code Console.charset()}，
     * 会自然退回到 {@code sun.jnu.encoding}（中文 Windows 下通常为 GBK）。</p>
     *
     * @return 标准输入应使用的字符集
     */
    private Charset detectInputCharset() {
        Charset explicit = charsetOrNull(System.getProperty("stdin.encoding"));
        if (explicit != null) {
            return explicit;
        }
        Console console = System.console();
        if (console != null) {
            try {
                Method charsetMethod = Console.class.getMethod("charset");
                Object result = charsetMethod.invoke(console);
                if (result instanceof Charset) {
                    return (Charset) result;
                }
            } catch (Exception ignored) {
                // Java 8 没有 Console.charset()，继续按属性推断
            }
        }
        Charset nativeCharset = charsetOrNull(System.getProperty("native.encoding"));
        if (nativeCharset != null) {
            return nativeCharset;
        }
        Charset jnuCharset = charsetOrNull(System.getProperty("sun.jnu.encoding"));
        if (jnuCharset != null) {
            return jnuCharset;
        }
        return Charset.defaultCharset();
    }

    /**
     * 按名称获取字符集，名称非法或不受支持时返回 {@code null}。
     *
     * @param name 字符集名称
     * @return 字符集对象或 {@code null}
     */
    private Charset charsetOrNull(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        try {
            return Charset.forName(name.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 输出一行文本。
     *
     * @param text 文本内容
     */
    public void printLine(String text) {
        System.out.println(text);
    }

    /**
     * 输出空行。
     */
    public void printBlank() {
        System.out.println();
    }

    /**
     * 输出普通提示。
     *
     * @param message 提示内容
     */
    public void info(String message) {
        System.out.println("[提示] " + message);
    }

    /**
     * 输出成功提示。
     *
     * @param message 提示内容
     */
    public void success(String message) {
        System.out.println("[成功] " + message);
    }

    /**
     * 输出错误提示。
     *
     * @param message 提示内容
     */
    public void error(String message) {
        System.out.println("[失败] " + message);
    }

    /**
     * 输出分隔线。
     */
    public void printDivider() {
        System.out.println(DIVIDER);
    }

    /**
     * 读取一行输入并去除首尾空白。
     *
     * @param prompt 输入提示
     * @return 用户输入；输入流结束时返回空字符串
     */
    public String readLine(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        if (!scanner.hasNextLine()) {
            System.out.println();
            return "";
        }
        return scanner.nextLine().trim();
    }

    /**
     * 读取非空字符串，超长或为空时要求重新输入。
     *
     * @param prompt   输入提示
     * @param maxLength 最大长度
     * @return 合法输入
     */
    public String readNonEmpty(String prompt, int maxLength) {
        while (true) {
            String value = readLine(prompt);
            if (value.isEmpty()) {
                error("输入不能为空，请重新输入。");
                continue;
            }
            if (value.length() > maxLength) {
                error("输入长度不能超过 " + maxLength + " 个字符，请重新输入。");
                continue;
            }
            return value;
        }
    }

    /**
     * 读取指定范围内的整数，非法输入时要求重新输入。
     *
     * @param prompt 输入提示
     * @param min    最小值
     * @param max    最大值
     * @return 合法整数
     */
    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            String value = readLine(prompt);
            try {
                int number = Integer.parseInt(value);
                if (number < min || number > max) {
                    error("请输入 " + min + " 到 " + max + " 之间的整数。");
                    continue;
                }
                return number;
            } catch (NumberFormatException e) {
                error("输入不是合法整数，请输入 " + min + " 到 " + max + " 之间的数字。");
            }
        }
    }

    /**
     * 读取不小于指定值的小数，非法输入时要求重新输入。
     *
     * @param prompt 输入提示
     * @param min    允许的最小值
     * @return 合法小数
     */
    public double readDouble(String prompt, double min) {
        while (true) {
            String value = readLine(prompt);
            try {
                double number = Double.parseDouble(value);
                if (!Double.isFinite(number) || number < min) {
                    error(String.format("请输入不小于 %.1f 的数字。", min));
                    continue;
                }
                return number;
            } catch (NumberFormatException e) {
                error("输入不是合法数字，请重新输入。");
            }
        }
    }

    /**
     * 读取符合格式的 COSPARID（2 位国家编码 + 4 位数字）。
     *
     * @param prompt 输入提示
     * @return 统一转为大写后的 COSPARID
     */
    public String readCosparId(String prompt) {
        while (true) {
            String value = readLine(prompt).toUpperCase(Locale.ROOT);
            if (value.matches("[A-Z]{2}\\d{4}")) {
                return value;
            }
            error("格式错误：COSPARID 应为 2 位国家编码 + 4 位数字，例如 CN0001。");
        }
    }

    /**
     * 读取 y/n 确认。
     *
     * @param prompt 输入提示
     * @return 确认返回 {@code true}，取消返回 {@code false}
     */
    public boolean readYesNo(String prompt) {
        while (true) {
            String value = readLine(prompt).toLowerCase(Locale.ROOT);
            if (value.equals("y") || value.equals("yes") || value.equals("是")) {
                return true;
            }
            if (value.equals("n") || value.equals("no") || value.equals("否")) {
                return false;
            }
            error("请输入 y 或 n。");
        }
    }

    /**
     * 暂停，等待用户按回车后返回主菜单。
     */
    public void pause() {
        readLine("\n按回车键返回主菜单...");
    }

    /**
     * 格式化时间。
     *
     * @param time 时间，允许为 {@code null}
     * @return 格式化文本；{@code null} 返回 {@code "-"}
     */
    public String formatTime(LocalDateTime time) {
        return time == null ? "-" : TIME_FORMAT.format(time);
    }

    /**
     * 打印卫星列表表格。
     *
     * @param satellites 待显示的卫星列表
     */
    public void printSatelliteTable(List<Satellite> satellites) {
        if (satellites == null || satellites.isEmpty()) {
            info("列表为空，没有可显示的卫星。");
            return;
        }
        System.out.println(padRight("序号", 5) + padRight("COSPARID", 10)
                + padRight("名称", 22) + padRight("状态", 12)
                + padRight("轨道半径", 10) + "所属机构");
        System.out.println(DIVIDER);
        for (int i = 0; i < satellites.size(); i++) {
            Satellite satellite = satellites.get(i);
            System.out.println(padRight(String.valueOf(i + 1), 5)
                    + padRight(satellite.getCosparId(), 10)
                    + padRight(truncate(satellite.getName(), 20), 22)
                    + padRight(satellite.getStatus().name(), 12)
                    + padRight(String.format("%.2f", satellite.getOrbitRadius()), 10)
                    + satellite.getCountryName() + " · " + satellite.getAgencyName());
        }
    }

    /**
     * 打印一颗卫星的详细信息。
     *
     * @param satellite 待显示的卫星
     */
    public void printSatelliteDetail(Satellite satellite) {
        System.out.println("==================== 卫星详细信息 ====================");
        System.out.println(padRight("COSPARID", 15) + ": "
                + satellite.getCosparId() + "（分配后不可修改）");
        System.out.println(padRight("名称", 15) + ": " + satellite.getName());
        System.out.println(padRight("国家 / 机构", 15) + ": "
                + satellite.getCountryName() + "（" + satellite.getCountryCode()
                + "）· " + satellite.getAgencyName());
        System.out.println(padRight("扩展信息", 15) + ": " + satellite.getSpecificInfo());
        System.out.println(padRight("轨道半径", 15) + ": "
                + String.format("%.2f", satellite.getOrbitRadius()));
        System.out.println(padRight("状态", 15) + ": " + satellite.getStatusText());
        System.out.println(padRight("是否可修改", 15) + ": "
                + (satellite.isEditable() ? "是" : "否（封锁或已归档）"));
        System.out.println(padRight("功能与注意事项", 15) + ": "
                + satellite.getFunctionAndNotes());
        System.out.println(padRight("注册时间", 15) + ": "
                + formatTime(satellite.getRegisteredAt()));
        System.out.println(padRight("最近修改时间", 15) + ": "
                + formatTime(satellite.getLastModifiedAt()));
        System.out.println(padRight("归档时间", 15) + ": "
                + formatTime(satellite.getArchivedAt()));
        System.out.println(padRight("修改记录条数", 15) + ": "
                + satellite.getModificationCount() + "（含注册记录）");
    }

    /**
     * 打印一颗卫星的注册时间、历次修改时间与修改事项。
     *
     * @param satellite 待显示的卫星
     */
    public void printHistory(Satellite satellite) {
        System.out.println("==================== 注册与修改历史 ====================");
        System.out.println(padRight("COSPARID", 16) + ": " + satellite.getCosparId());
        System.out.println(padRight("名称", 16) + ": " + satellite.getName());
        System.out.println(padRight("当前状态", 16) + ": " + satellite.getStatusText());
        System.out.println(padRight("注册时间", 16) + ": "
                + formatTime(satellite.getRegisteredAt()));
        System.out.println(padRight("最近修改时间", 16) + ": "
                + formatTime(satellite.getLastModifiedAt()));
        System.out.println(padRight("归档时间", 16) + ": "
                + formatTime(satellite.getArchivedAt()));
        List<ModificationRecord> history = satellite.getModificationHistory();
        System.out.println("修改记录（" + history.size() + " 条）：");
        for (int i = 0; i < history.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + history.get(i));
        }
    }

    /**
     * 计算字符串在终端中的显示宽度，中文等全角字符按 2 列计算。
     *
     * @param text 文本
     * @return 显示宽度
     */
    public int displayWidth(String text) {
        if (text == null) {
            return 0;
        }
        int width = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            width += isWide(codePoint) ? 2 : 1;
            i += Character.charCount(codePoint);
        }
        return width;
    }

    /**
     * 按显示宽度在右侧补空格。
     *
     * @param text  文本
     * @param width 目标显示宽度
     * @return 补齐后的文本
     */
    public String padRight(String text, int width) {
        String value = text == null ? "" : text;
        StringBuilder builder = new StringBuilder(value);
        int padding = width - displayWidth(value);
        for (int i = 0; i < padding; i++) {
            builder.append(' ');
        }
        return builder.toString();
    }

    /**
     * 按显示宽度截断文本，超长时以省略号结尾。
     *
     * @param text  文本
     * @param width 最大显示宽度
     * @return 截断后的文本
     */
    public String truncate(String text, int width) {
        if (text == null || displayWidth(text) <= width) {
            return text == null ? "" : text;
        }
        StringBuilder builder = new StringBuilder();
        int current = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            int charWidth = isWide(codePoint) ? 2 : 1;
            if (current + charWidth > width - 1) {
                break;
            }
            builder.appendCodePoint(codePoint);
            current += charWidth;
            i += Character.charCount(codePoint);
        }
        return builder.append('…').toString();
    }

    /**
     * 判断一个 Unicode 码点是否为全角宽字符。
     *
     * @param codePoint Unicode 码点
     * @return 宽字符返回 {@code true}
     */
    private boolean isWide(int codePoint) {
        return (codePoint >= 0x1100 && codePoint <= 0x115F)
                || (codePoint >= 0x2E80 && codePoint <= 0xA4CF && codePoint != 0x303F)
                || (codePoint >= 0xAC00 && codePoint <= 0xD7A3)
                || (codePoint >= 0xF900 && codePoint <= 0xFAFF)
                || (codePoint >= 0xFE30 && codePoint <= 0xFE4F)
                || (codePoint >= 0xFF00 && codePoint <= 0xFF60)
                || (codePoint >= 0xFFE0 && codePoint <= 0xFFE6)
                || (codePoint >= 0x20000 && codePoint <= 0x3FFFD);
    }
}
