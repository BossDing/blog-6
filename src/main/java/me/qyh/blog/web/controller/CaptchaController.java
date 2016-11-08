/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.web.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.code.kaptcha.Producer;

import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.SystemException;

@Controller
public class CaptchaController {

	@Autowired
	private Producer captchaProducer;

	@ResponseBody
	@RequestMapping(value = "/captcha", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
	public byte[] draw(HttpSession session) {
		String capText = captchaProducer.createText();
		session.setAttribute(Constants.VALIDATE_CODE_SESSION_KEY, capText);
		BufferedImage bi = captchaProducer.createImage(capText);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(bi, "jpg", baos);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		return baos.toByteArray();
	}

}
