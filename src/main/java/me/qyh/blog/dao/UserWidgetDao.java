package me.qyh.blog.dao;

import java.util.List;

import me.qyh.blog.pageparam.UserWidgetQueryParam;
import me.qyh.blog.ui.widget.UserWidget;

public interface UserWidgetDao {

	void insert(UserWidget userWidget);

	void deleteById(Integer id);

	List<UserWidget> selectPage(UserWidgetQueryParam param);

	int selectCount(UserWidgetQueryParam param);

	void update(UserWidget userWidget);
	
	UserWidget selectByName(String name);

	UserWidget selectById(Integer id);

}
