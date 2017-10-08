package chapter02;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

public class UrlUtils {

	public static String request(String url) {
		try (InputStream is = new URL(url).openStream()) {
			return IOUtils.toString(is, StandardCharsets.UTF_8);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
