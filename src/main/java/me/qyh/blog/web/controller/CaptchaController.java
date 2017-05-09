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
import java.util.Random;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.github.cage.Cage;
import com.github.cage.GCage;
import com.github.cage.token.RandomTokenGenerator;

import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.exception.SystemException;

@Controller
public class CaptchaController implements InitializingBean {

	private static final Random random = new Random(System.nanoTime());
	
	@Value("${captcha.num:4}")
	private int num;
	@Value("${captcha.delta:0}")
	private int delta;

	private Cage cage;

	@ResponseBody
	@GetMapping(value = "captcha", produces = MediaType.IMAGE_JPEG_VALUE)
	public byte[] draw(HttpSession session) {
		String capText = cage.getTokenGenerator().next();
		session.setAttribute(Constants.VALIDATE_CODE_SESSION_KEY, capText);
		BufferedImage bi = cage.drawImage(capText);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(bi, "jpg", baos);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		return baos.toByteArray();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (num > 0 && delta >= 0) {
			cage = new Cage(null, null, null, null, Cage.DEFAULT_COMPRESS_RATIO,
					new RandomTokenGenerator(random, num, delta), random);
		} else {
			cage = new GCage();
		}
	}
}
