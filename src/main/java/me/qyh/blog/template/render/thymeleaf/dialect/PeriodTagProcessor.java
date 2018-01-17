package me.qyh.blog.template.render.thymeleaf.dialect;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import me.qyh.blog.core.util.Times;
import me.qyh.blog.core.util.Validators;

/**
 * 用来限制页面只能在一个时间段内被访问
 */
public class PeriodTagProcessor extends DefaultAttributesTagProcessor {

	private static final String TAG_NAME = "period";
	private static final int PRECEDENCE = 1000;

	private static final String BEGIN = "begin";
	private static final String END = "end";
	private static final String INCLUDE = "include";

	public PeriodTagProcessor(String dialectPrefix) {
		super(TemplateMode.HTML, dialectPrefix, TAG_NAME, false, null, false, PRECEDENCE);
	}

	@Override
	protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {
		Map<String, String> attMap = processAttribute(context, tag);

		LocalDateTime beginTime = null;
		LocalDateTime endTime = null;

		String begin = attMap.get(BEGIN);
		if (!Validators.isEmptyOrNull(begin, true)) {
			beginTime = Times.parseAndGet(begin);
		}

		String end = attMap.get(END);
		if (!Validators.isEmptyOrNull(end, true)) {
			endTime = Times.parseAndGet(end);
		}

		boolean include = Boolean.parseBoolean(attMap.getOrDefault(INCLUDE, "true"));

		boolean removed = false;

		try {
			PeriodStatus status = parse(beginTime, endTime, include);
			Objects.requireNonNull(status);

			if (PeriodStatus.IN.equals(status)) {
				structureHandler.removeTags();
				removed = true;
			}

		} finally {
			if (!removed) {
				structureHandler.removeElement();
			}
		}
	}

	/**
	 * @return NOT NULL !!
	 */
	protected PeriodStatus parse(LocalDateTime beginTime, LocalDateTime endTime, boolean include) {
		if (beginTime != null || endTime != null) {
			LocalDateTime now = Times.now();

			boolean invalid = beginTime != null && endTime != null && endTime.isBefore(beginTime)
					&& endTime.equals(beginTime);

			if (invalid) {
				return PeriodStatus.INVALID;
			}

			if (include) {
				// > begin and < end
				if ((beginTime != null && now.isBefore(beginTime)) || (endTime != null && now.isAfter(endTime))) {
					return PeriodStatus.OUT;
				} else {
					return PeriodStatus.IN;
				}
			} else {
				// < begin or > end
				if ((beginTime != null && now.isAfter(beginTime)) || (endTime != null && now.isBefore(endTime))) {
					return PeriodStatus.OUT;
				} else {
					return PeriodStatus.IN;
				}
			}
		}
		return PeriodStatus.INVALID;
	}

	protected enum PeriodStatus {
		IN, OUT, INVALID;
	}
}
