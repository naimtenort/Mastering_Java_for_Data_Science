package chapter02;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jooq.lambda.tuple.Tuple2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.aol.cyclops.control.LazyReact;
import com.fasterxml.jackson.jr.ob.JSON;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import joinery.DataFrame;

public class AccessDataTests {

	public static void main(String[] args) throws IOException, SQLException {
		
		// CSV 에서 내용 읽어 오기
		System.out.println("=========================== CSV 에서 내용 읽어 오기");
		List<Person> result = new ArrayList<>();
		Path csvFile = Paths.get("data/csv-example-generatedata_com.csv");
		try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
			CSVFormat csv = CSVFormat.RFC4180.withHeader();
			try (CSVParser parser = csv.parse(reader)) {
				Iterator<CSVRecord> it = parser.iterator();
				it.forEachRemaining(rec -> {
					String name = rec.get("name");
					String email = rec.get("email");
					String country = rec.get("country");
					int salary = Integer.parseInt(rec.get("salary").substring(1));
					int experience = Integer.parseInt(rec.get("experience"));
					Person person = new Person(name, email, country, salary, experience);
					result.add(person);
				});
			}
		}
		System.out.println(result);
		
		// CSV 에서 내용 읽어 오기 - CSV 크기가 작을 경우 반복자 없이 한번에 읽기
		System.out.println("=========================== CSV 에서 내용 읽어 오기 - CSV 크기가 작을 경우 반복자 없이 한번에 읽기");
		List<Person> result2 = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
			CSVFormat csv = CSVFormat.RFC4180.withHeader();
			try (CSVParser parser = csv.parse(reader)) {
				List<CSVRecord> records = parser.getRecords();
				for (CSVRecord rec : records) {
					String name = rec.get("name");
					String email = rec.get("email");
					String country = rec.get("country");
					int salary = Integer.parseInt(rec.get("salary").substring(1));
					int experience = Integer.parseInt(rec.get("experience"));
					Person person = new Person(name, email, country, salary, experience);
					result2.add(person);
				}
			}
		}
		System.out.println(result2);
		
		// CSV 에서 내용 읽어 오기 - 탭으로 구분된 CSV
		System.out.println("=========================== CSV 에서 내용 읽어 오기 - 탭으로 구분된 CSV");
		List<Person> result3 = new ArrayList<>();
		Path csvFile2 = Paths.get("data/csv-example-generatedata_com_tab.csv");
		try (BufferedReader reader = Files.newBufferedReader(csvFile2, StandardCharsets.UTF_8)) {
			CSVFormat csv = CSVFormat.TDF.withHeader();
			try (CSVParser parser = csv.parse(reader)) {
				Iterator<CSVRecord> it = parser.iterator();
				it.forEachRemaining(rec -> {
					String name = rec.get("name");
					String email = rec.get("email");
					String country = rec.get("country");
					int salary = Integer.parseInt(rec.get("salary").substring(1));
					int experience = Integer.parseInt(rec.get("experience"));
					Person person = new Person(name, email, country, salary, experience);
					result3.add(person);
				});
			}
		}
		System.out.println(result3);
		
		// Jsoup 사용 웹 크롤링
		System.out.println("=========================== Jsoup 사용 웹 크롤링");
		Map<String, Double> result4 = new HashMap<>();
		String rawHtml = 
				UrlUtils.request("https://www.kaggle.com/c/5174/leaderboard.json?includeBeforeUser=false&includeAfterUser=false");
		Document document = Jsoup.parse(rawHtml);
		Elements tableRows = document.select("#competition-leaderboard__table tr");
		for (Element tr : tableRows) {
			Elements columns = tr.select("td");
			if (columns.isEmpty()) {
				continue;
			}
			String team = columns.get(2).select("span").text();
			double score = Double.parseDouble(columns.get(3).text());
			result4.put(team, score);
		}
		Comparator<Map.Entry<String, Double>> byValue = Map.Entry.comparingByValue();
		result4.entrySet().stream()
			.sorted(byValue.reversed())
			.forEach(System.out::println);
		
		// JSON 메시지 요청 - 단순 유형
		System.out.println("=========================== JSON 메시지 요청 - 단순 유형");
		String text = "mastering java for data science";
		String json = UrlUtils.request("http://md5.jsontest.com/?text=" + text.replace(' ', '+'));
		Map<String, Object> map = JSON.std.mapFrom(json);
		System.out.println(map.get("original"));
		System.out.println(map.get("md5"));
		
		// JSON 메시지 요청 - 복합 유형
		System.out.println("=========================== JSON 메시지 요청 - 복합 유형");
		String username = "alexeygrigorev";
		String json2 = UrlUtils.request("https://api.github.com/users/" + username + "/repos");
		List<Map<String, ?>> list = (List<Map<String, ?>>) JSON.std.anyFrom(json2);
		String name = (String) list.get(0).get("name");
		System.out.println(name);
		
		// JSON 메시지 요청 - 질의 언어 사용
		System.out.println("=========================== JSON 메시지 요청 - 질의 언어 사용");
		ReadContext ctx = JsonPath.parse(json2);
		String query = "$..[?(@.language=='Java' && @.stargazers_count > 0)]full_name";
		List<String> javaProjects = ctx.read(query);
		System.out.println(javaProjects);
		
		// 데이터베이스 접속
		System.out.println("=========================== 데이터베이스 접속");
		MysqlDataSource datasource = new MysqlDataSource();
		datasource.setServerName("localhost");
		datasource.setDatabaseName("people");
		datasource.setUser("root");
		datasource.setPassword("abc123");
		
		// 데이터베이스 데이터 적재 - 다음 테스트를 위한 테이블 초기화
		System.out.println("=========================== 데이터베이스 데이터 적재 - 다음 테스트를 위한 테이블 초기화");
		try (Connection connection = datasource.getConnection()) {
			String sql = "DELETE from people";
			try (Statement statement = connection.createStatement()) {
				statement.execute(sql);
			}
		}
				
		// 데이터베이스 데이터 적재 - 일반 방식
		System.out.println("=========================== 데이터베이스 데이터 적재 - 일반 방식");
		try (Connection connection = datasource.getConnection()) {
			String sql = "INSERT INTO people (name, email, country, salary, experience)"
					+ " VALUES (?, ?, ?, ?, ?);";
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				for (Person person : result) {
					statement.setString(1, person.getName());
					statement.setString(2, person.getEmail());
					statement.setString(3, person.getCountry());
					statement.setInt(4, person.getSalary());
					statement.setInt(5, person.getExperience());
					statement.execute();
				}
			}
		}
		
		// 데이터베이스 데이터 적재 - 다음 테스트를 위한 테이블 초기화
		System.out.println("=========================== 데이터베이스 데이터 적재 - 다음 테스트를 위한 테이블 초기화");
		try (Connection connection = datasource.getConnection()) {
			String sql = "DELETE from people";
			try (Statement statement = connection.createStatement()) {
				statement.execute(sql);
			}
		}
		
		// 데이터베이스 데이터 적재 - 배치 방식
		System.out.println("=========================== 데이터베이스 데이터 적재 - 배치 방식");
		List<List<Person>> chunks = Lists.partition(result, 50);
		try (Connection connection = datasource.getConnection()) {
			String sql = "INSERT INTO people (name, email, country, salary, experience) "
					+ "VALUES (?, ?, ?, ?, ?);";
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				for (List<Person> chunk : chunks) {
					for (Person person : chunk) {
						statement.setString(1, person.getName());
						statement.setString(2, person.getEmail());
						statement.setString(3, person.getCountry());
						statement.setInt(4, person.getSalary());
						statement.setInt(5, person.getExperience());
						statement.addBatch();
					}
					statement.executeBatch();
				}
			}
		}
		
		// 데이터베이스 질의문 실행
		System.out.println("=========================== 데이터베이스 질의문 실행");
		String country = "Greenland";
		try (Connection connection = datasource.getConnection()) {
			String sql = "SELECT name, email, salary, experience "
					+ "FROM people WHERE country = ?;";
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				List<Person> selectResult = new ArrayList<>();
				statement.setString(1, country);
				try (ResultSet rs = statement.executeQuery()) {
					while (rs.next()) {
						String personName = rs.getString(1);
						String email = rs.getString(2);
						int salary = rs.getInt(3);
						int experience = rs.getInt(4);
						Person person 
							= new Person(personName, email, country, salary, experience);
						selectResult.add(person);
					}
				}
				System.out.println(selectResult);
			}
		}
		
		// 데이터프레임 - joinery 사용 읽기
		System.out.println("=========================== 데이터프레임 - joinery 사용 읽기");
		DataFrame<Object> df = DataFrame
				.readCsv("data/csv-example-generatedata_com.csv");
		System.out.println(df);
		
		// 데이터프레임 - 국가별 인덱스 생성
		System.out.println("=========================== 데이터프레임 - 국가별 인덱스 생성");
		List<Object> contries = df.col("country");
		Map<String, Long> contriesmap = LazyReact
				.sequentialBuilder()
				.from(contries)
				.cast(String.class)
				.distinct()
				.zipWithIndex()
				.toMap(Tuple2::v1, Tuple2::v2);
		List<Object> indexes = contries.stream()
				.map(contriesmap::get)
				.collect(Collectors.toList());
		System.out.println(indexes);
		
		// 데이터프레임 - 기존 열 삭제 후 새로운 열로 대체
		System.out.println("=========================== 데이터프레임 - 기존 열 삭제 후 새로운 열로 대체");
		df = df.drop("country");
		df.add("country_index", indexes);
		System.out.println(df);
	}
}
