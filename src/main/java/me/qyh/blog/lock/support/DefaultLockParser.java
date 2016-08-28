package me.qyh.blog.lock.support;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.springframework.validation.Validator;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;

import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockParser;
import me.qyh.util.Jsons;

public class DefaultLockParser implements LockParser<Lock> {

	private Validator[] validators;

	@Override
	public Lock getLockFromRequest(HttpServletRequest request) throws Exception {
		InputStream is = request.getInputStream();
		ObjectReader reader = Jsons.reader();
		JsonParser jp = reader.getFactory().createParser(is);
		JsonNode node = Jsons.reader().readTree(jp);
		JsonNode typeNode = node.get("type");
		if (typeNode != null && typeNode.textValue() != null) {
			String type = typeNode.textValue();
			if (type != null) {
				switch (type) {
				case "PASSWORD":
					return reader.treeToValue(node, PasswordLock.class);
				case "QA":
					return reader.treeToValue(node, QALock.class);
				}
			}
		}
		return null;
	}

	@Override
	public Validator[] getValidators() {
		return validators;
	}

	public void setValidators(Validator[] validators) {
		this.validators = validators;
	}

}
