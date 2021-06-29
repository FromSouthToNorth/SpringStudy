package org.springframework.beans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * JavaBeans 的静态便利方法：用于实例化 bean，
 * 检查 bean 属性类型、复制 bean 属性等
 *
 * <p>主要供框架内部使用，但在某种程度上也
 * 对应用程序类很有用。考虑
 * <a href="https://commons.apache.org/proper/commons-beanutils/">Apache Commons BeanUtils</a>,
 * <a href="https://hotelsdotcom.github.io/bull/">BULL - Bean Utils Light Library</a>,
 * 或类似的第三方框架，以获得更全面的 bean 实用程序
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Sebastien Deleuze
 */
public abstract class BeanUtils {

    private static final Log logger = LogFactory.getLog(BeanUtils.class);

    private static final ParameterNameDiscoverer parameterNameDiscoverer =
            new DefaultParameterNameDiscoverer();
}
