package me.qyh.blog.tool;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.h2.tools.RunScript;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.SystemException;

/**
 * 将mysql中的数据转化为h2中的数据
 * 
 * @author Administrator
 *
 */
public class DBConvert {

	private DataSource fds;
	private DataSource tds;
	private final DBType from;
	private final DBType to;

	private enum DBType {
		MYSQL, H2;
	}

	/**
	 * 删除表(如果存在)，然后新建表
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	private void initDB() throws SQLException, IOException {
		// init sql
		try (InputStream is = new ClassPathResource("me/qyh/blog/tool/blog.init.sql").getInputStream();
				InputStreamReader reader = new InputStreamReader(is, Constants.CHARSET);
				Connection conn = tds.getConnection()) {
			RunScript.execute(conn, reader);
			System.out.println("初始化mysql完毕");
		}
	}

	private void initDataSource() throws IOException {
		try (InputStream is = new ClassPathResource("me/qyh/blog/tool/db.properties").getInputStream()) {
			Properties pros = new Properties();
			pros.load(is);
			switch (from) {
			case MYSQL:
				fds = new DriverManagerDataSource(pros.getProperty("mysql.jdbcUrl"), pros.getProperty("mysql.user"),
						pros.getProperty("mysql.password"));
				break;
			case H2:
				fds = new DriverManagerDataSource(pros.getProperty("h2.jdbcUrl"), pros.getProperty("h2.user"),
						pros.getProperty("h2.password"));
				break;
			default:
				throw new SystemException("不支持的数据库类型：" + from);
			}
			switch (to) {
			case MYSQL:
				tds = new DriverManagerDataSource(pros.getProperty("mysql.jdbcUrl"), pros.getProperty("mysql.user"),
						pros.getProperty("mysql.password"));
				break;
			case H2:
				tds = new DriverManagerDataSource(pros.getProperty("h2.jdbcUrl"), pros.getProperty("h2.user"),
						pros.getProperty("h2.password"));
				break;
			default:
				throw new SystemException("不支持的数据库类型：" + from);
			}
		}
	}

	public DBConvert(DBType from, DBType to) throws IOException, SQLException {
		if (from == null) {
			throw new IllegalArgumentException("源数据库不能为空");
		}
		if (to == null) {
			throw new IllegalArgumentException("目标数据库不能为空");
		}
		this.from = from;
		this.to = to;
		initDataSource();
	}

	public void convert() throws SQLException, IOException {
		initDB();
		copyTable("blog_lock");
		copyTable("blog_space");
		copyTable("blog_tag");
		copyTable("blog_article");
		copyTable("blog_article_tag");
		copyTable("blog_comment");
		copyTable("blog_common_file");
		copyTable("blog_file");
		copyTable("blog_file_delete");
		copyTable("blog_fragment_user");
		copyTable("blog_page_user");
		copyTable("blog_page_lock");
		copyTable("blog_page_error");
		copyTable("blog_page_expanded");
		copyTable("blog_page_sys");
	}

	private void copyTable(String table) throws SQLException {
		try (Connection conn = fds.getConnection()) {
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + table);
			try (ResultSet rs = ps.executeQuery()) {
				ResultSetMetaData rsmd = rs.getMetaData();
				String insertSql = buildInsertSql(rsmd);
				try (Connection h2Conn = tds.getConnection()) {
					PreparedStatement h2Ps = h2Conn.prepareStatement(insertSql);
					while (rs.next()) {
						for (int i = 1; i <= rsmd.getColumnCount(); i++) {
							h2Ps.setObject(i, rs.getObject(i), rsmd.getColumnType(i));
						}
						h2Ps.addBatch();
					}
					h2Ps.executeBatch();
				}
			}
		}
		System.out.println("拷贝表" + table + "完毕");
	}

	private String buildInsertSql(ResultSetMetaData rsmd) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO ");
		sb.append(rsmd.getTableName(1).toLowerCase()).append("(");
		for (int i = 1; i <= rsmd.getColumnCount(); i++) {
			String columnName = rsmd.getColumnName(i);
			sb.append(columnName).append(",");
		}
		sb.deleteCharAt(sb.length() - 1).append(") ");
		sb.append("VALUES(");
		for (int i = 1; i <= rsmd.getColumnCount(); i++) {
			sb.append("?").append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		return sb.toString();
	}

	public static void main(String[] args) throws IOException, SQLException {
		DBConvert tool = new DBConvert(DBType.MYSQL, DBType.H2);
		tool.convert();
	}

}
