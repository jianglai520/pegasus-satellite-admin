/**
 * 业务异常类。
 *
 * <p>用于表达卫星管理业务规则被违反时产生的可预期错误，例如轨道冲突、
 * COSPARID 重复、对封锁卫星执行修改等。界面层捕获该异常并把提示信息
 * 展示给用户，而不是让程序崩溃。</p>
 *
 * @author FM
 * @version 1.0
 */
public class BusinessException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * 使用指定的提示信息构造业务异常。
     *
     * @param message 面向用户的错误说明
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * 使用提示信息和原始异常构造业务异常。
     *
     * @param message 面向用户的错误说明
     * @param cause   引发该异常的底层异常
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
