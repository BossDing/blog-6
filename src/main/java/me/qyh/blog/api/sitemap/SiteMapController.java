package me.qyh.blog.api.sitemap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SiteMapController {

	@Autowired
	private XmlSiteMap xmlSiteMap;

	@RequestMapping(value = "sitemap.xml", method = RequestMethod.GET, produces = { MediaType.APPLICATION_XML_VALUE })
	@ResponseBody
	public String siteMap() {
		return xmlSiteMap.getSiteMap();
	}

}
