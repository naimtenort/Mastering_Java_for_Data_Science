package chapter02;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Crawler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Crawler.class);
	
	public static void main(String[] args) throws IOException {
		// MapDB 생성
		DB db = DBMaker.fileDB("urls.db").closeOnJvmShutdown().make();
		HTreeMap<?, ?> htreeMap = db.hashMap("urls").createOrOpen();
		Map<String, String> urls = (Map<String, String>) htreeMap;
		
		// 연관 URL을 읽어 HTML를 MapDB에 저장
		Path path = Paths.get("data/search-results.txt");
		List<String> lines = FileUtils.readLines(path.toFile(), StandardCharsets.UTF_8);
		lines.parallelStream()
			.map(line -> line.split("\t"))
			.map(split -> "http://" + split[2])
			.distinct()
				.forEach(url -> {
					try {
						Optional<String> html = crawl(url);
						if (html.isPresent()) {
							LOGGER.debug("successfully crawled {}", url);
							urls.put(url, html.get());
						}
					} catch (Exception e) {
						LOGGER.error("got exception when processing url {}", url, e);
					}
				});
		
		// 페이지 크롤링 및 RankedPage 객체 생성
		Stream<RankedPage> pages = lines.parallelStream().flatMap(line -> {
			String[] split = line.split("\t");
			String query = split[0];
			int position = Integer.parseInt(split[1]);
			int searchPageNumber = 1 + (position - 1) / 10; // converts position to a page number
			String url = "http://" + split[2];
			if (!urls.containsKey(url)) { // no crawl available
				return Stream.empty();
			}
			RankedPage page = new RankedPage(url, position, searchPageNumber);
			String html = urls.get(url);
			Document document = Jsoup.parse(html);
			String title = document.title();
			int titleLength = title.length();
			page.setTitleLength(titleLength);
			boolean queryInTitle = title.toLowerCase().contains(query.toLowerCase());
			page.setQueryInTitle(queryInTitle);
			if (document.body() == null) { // no body for the document
				return Stream.empty();
			}
			int bodyContentLength = document.body().text().length();
			page.setBodyContentLength(bodyContentLength);
			int numberOfLinks = document.body().select("a").size();
			page.setNumberOfLinks(numberOfLinks);
			int numberOfHeaders = document.body().select("h1,h2,h3,h4,h5,h6").size();
			page.setNumberOfHeaders(numberOfHeaders);
			return Stream.of(page);
		});
	}
	
	public static Optional crawl(String url) {
		// executor 선언
		int numProc = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(numProc);

		// executor 사용
		int timeout = 30;
		try {
			Future<String> future = executor.submit(() -> UrlUtils.request(url));
			String result = future.get(timeout, TimeUnit.SECONDS);
			return Optional.of(result);
		} catch (TimeoutException e) {
			LOGGER.warn("timeout exception: could not crawl {} in {} sec", url, timeout);
			return Optional.empty();
		} catch (InterruptedException e) {
			LOGGER.warn("interrupted exception:");
			return Optional.empty();
		} catch (ExecutionException e) {
			LOGGER.warn("execution exception:");
			return Optional.empty();
		}
		
		
	}

}
