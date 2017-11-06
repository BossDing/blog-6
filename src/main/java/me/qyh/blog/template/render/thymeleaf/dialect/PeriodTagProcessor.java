package me.qyh.blog.template.render.thymeleaf.dialect;

import java.time.LocalDateTime;
import java.util.Map;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import me.qyh.blog.core.exception.RuntimeLogicException;
import me.qyh.blog.core.message.Message;
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
		super(TemplateMode.HTML, dialectPrefix, // Prefix to be applied to name
				TAG_NAME, // Tag name: match specifically this tag
				false, // Apply dialect prefix to tag name
				null, // No attribute name: will match by tag name
				false, // No prefix to be applied to attribute name
				PRECEDENCE); // Precedence (inside dialect's own precedence)
	}

	@Override
	protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {
		structureHandler.removeElement();
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

		if (beginTime != null || endTime != null) {
			LocalDateTime now = Times.now();
			
			boolean invalid = beginTime != null && endTime != null && endTime.isBefore(beginTime);
			
			if(invalid){
				return ;
			}
			
			if (include) {
				// >begin and <end
				if ((beginTime != null && now.isBefore(beginTime)) || (endTime != null && now.isAfter(endTime))) {
					handleNotInclude(beginTime, endTime);
				}
			} else {
				// <begin or >end
				if((beginTime != null && now.isAfter(beginTime)) || (endTime != null && now.isBefore(endTime)) ){
					handleNotExclude(beginTime,endTime);
				}
			}
		}
	}
	
	
	protected void handleNotInclude(LocalDateTime begin,LocalDateTime end){
		String fmtBegin = begin == null ? "/" : Times.format(begin, "yyyy-MM-dd HH:mm");
		String fmtEnd =  end == null ? "/" : Times.format(end, "yyyy-MM-dd HH:mm");
		throw new RuntimeLogicException(new Message("template.render.period.include",
				"只能在["+fmtBegin+","+fmtEnd+"]这个时间段內访问",fmtBegin,fmtEnd));
	}
	
	protected void handleNotExclude(LocalDateTime begin,LocalDateTime end){
		String fmtBegin = begin == null ? "/" : Times.format(begin, "yyyy-MM-dd HH:mm");
		String fmtEnd =  end == null ? "/" : Times.format(end, "yyyy-MM-dd HH:mm");
		throw new RuntimeLogicException(new Message("template.render.period.exclude",
				"只能在["+fmtBegin+","+fmtEnd+"]这个时间段外访问",fmtBegin,fmtEnd));
	}

}
