package me.qyh.blog.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.qyh.blog.ui.UIExposeHelper;

/**
 * 被标记为这个annotation的类将会被UIExposeHelper所用
 * <p>
 * <b>但是只能调用其中的静态方法！！！</b>
 * </p>
 * 
 * @see UIExposeHelper
 * @author Administrator
 *
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface UIUtils {

	String name() default "";

}
