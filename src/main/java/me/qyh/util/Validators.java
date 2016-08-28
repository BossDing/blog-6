package me.qyh.util;

import java.util.regex.Pattern;

public final class Validators {

	private static final Pattern IPADDRESS_PATTERN = Pattern
			.compile("((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
					+ "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
					+ "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}" + "|[1-9][0-9]|[0-9]))");

	private Validators() {

	}

	public static boolean validateIp(CharSequence input) {
		return validate(IPADDRESS_PATTERN, input);
	}

	public static boolean validate(Pattern pattern, CharSequence input) {
		return pattern.matcher(input).matches();
	}

	public static boolean isEmptyOrNull(String str, boolean trim) {
		if (str == null) {
			return true;
		}
		return trim ? str.trim().isEmpty() : str.isEmpty();
	}
	

}
