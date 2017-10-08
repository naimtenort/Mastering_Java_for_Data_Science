package chapter02;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IOTests {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		// BufferedReader 테스트
		List<String> lines = new ArrayList<>();
		try (InputStream is = new FileInputStream("data/text.txt")) {
			try (InputStreamReader isReader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				try (BufferedReader reader = new BufferedReader(isReader)) {
					while (true) {
						String line = reader.readLine();
						if (line == null) {
							break;
						}
						lines.add(line);
					}
					isReader.close();
				}
			}
		}
		System.out.println(lines);
		
		// BufferedReader 테스트 2
		List<String> lines2 = new ArrayList<>();
		Path path = Paths.get("data/text.txt");
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				lines2.add(line);
			}
		}
		System.out.println(lines2);
		
		// 자바 NIO 방식의 파일 읽기
		Path path2 = Paths.get("data/text.txt");
		List<String> lines3 = Files.readAllLines(path, StandardCharsets.UTF_8);
		System.out.println(lines3);
		
		// 결과 데이터 쓰기
		try (PrintWriter writer = new PrintWriter("output.txt", "UTF-8")) {
			for (String line : lines) {
				String upperCase = line.toUpperCase(Locale.US);
				writer.println(upperCase);
			}
		}
		
		// 결과 데이터 쓰기 - NIO 사용
		Path output = Paths.get("output2.txt");
		try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
			for (String line : lines) {
				String upperCase = line.toUpperCase(Locale.US);
				writer.write(upperCase);
				writer.newLine();
			}
		}
	}

}
