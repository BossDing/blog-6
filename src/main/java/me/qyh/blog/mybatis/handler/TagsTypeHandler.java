package me.qyh.blog.mybatis.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.SystemException;
import me.qyh.util.Validators;

public class TagsTypeHandler extends BaseTypeHandler<Set<Tag>> {
	@Override
	public Set<Tag> getNullableResult(ResultSet rs, String str) throws SQLException {
		return toTags(rs.getString(str));
	}

	@Override
	public Set<Tag> getNullableResult(ResultSet rs, int pos) throws SQLException {
		return toTags(rs.getString(pos));
	}

	@Override
	public void setNonNullParameter(PreparedStatement arg0, int arg1, Set<Tag> arg2, JdbcType arg3)
			throws SQLException {
		throw new SystemException("不支持这个方法");
	}

	protected Set<Tag> toTags(String str) {
		if (Validators.isEmptyOrNull(str, true)) {
			return Collections.emptySet();
		}
		String[] tagArray = str.split(",");
		Set<Tag> tags = new LinkedHashSet<Tag>();
		for (String tag : tagArray) {
			Tag _tag = new Tag();
			_tag.setName(tag);
			tags.add(_tag);
		}
		return tags;
	}

	@Override
	public Set<Tag> getNullableResult(CallableStatement arg0, int arg1) throws SQLException {
		throw new SystemException("不支持这个方法");
	}

}
