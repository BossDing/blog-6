package me.qyh.blog.comment.dao;

import java.io.IOException;
import java.net.MalformedURLException;

import me.qyh.blog.core.util.Jsons;
import me.qyh.blog.core.util.Jsons.ExpressionExecutor;
import me.qyh.blog.core.util.Jsons.ExpressionExecutors;

public class Test {

	public static void main(String[] args) throws MalformedURLException, IOException {
		String url = "https://api.taptapdada.com/app-tag/v1/by-tag?X-UA=V%3D1%26PN%3DTapTap%26VN_CODE%3D305%26LANG%3Dzh_CN%26CH%3Ddefault%26&tag=放置&sort=score&limit=10&from=";
		int from = 0;
		while (true) {
			ExpressionExecutor ee = Jsons.read(url + from);
			if(ee.isNull()){
				break;
			}
			ExpressionExecutors datas = ee.executeForExecutors("data->list");
			for (ExpressionExecutor data : datas) {
				String title = data.execute("title").orElse("");
				try{
					int pls = Integer.parseInt(data.execute("stat->review_count").orElse("0"));
					double score = Double.parseDouble(data.execute("stat->rating->score").orElse("0"));
					
					if(score < 8.5){
						System.out.println("done");
						return;
					}
					
					if(pls > 2500){
						System.out.println(title+"...."+score+"....."+pls);
					}
				}catch (NumberFormatException e) {
					//e.printStackTrace();
				}
			}
			from += 10;
		}

	}

}
