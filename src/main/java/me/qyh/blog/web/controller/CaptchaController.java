package me.qyh.blog.web.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.code.kaptcha.Producer;

import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.SystemException;

@Controller
public class CaptchaController {

	@Autowired
	private Producer captchaProducer;

	@RequestMapping(value = "captcha", method = RequestMethod.GET)
	public ResponseEntity<byte[]> draw(HttpSession session) {
		String capText = captchaProducer.createText();
		session.setAttribute(Constants.VALIDATE_CODE_SESSION_KEY, capText);
		BufferedImage bi = captchaProducer.createImage(capText);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(bi, "jpg", baos);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.valueOf("image/jpeg"));
		return new ResponseEntity<byte[]>(baos.toByteArray(), headers, HttpStatus.OK);
	}

}
