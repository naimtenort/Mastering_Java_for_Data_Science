package chapter02;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamingAPITests {

	public static void main(String[] args) throws IOException {
		
		// Word 클래스를 사용한 문장 표현
		Word[] array = { new Word("My", "RPR"), new Word("dog", "NN"),
				new Word("also", "RB"), new Word("likes", "VB"),
				new Word("eating", "VB"), new Word("sausage", "NN"),
				new Word(".", ".") };
		
		// 배열 데이터의 스트림 변환
		Stream<Word> stream = Arrays.stream(array);
		
		// 컬렉션 stream 메소드 사용 스트림 생성
		List<Word> list = Arrays.asList(array);
		Stream<Word> streamByCollection = list.stream();
		
		// 명사 형태의 단어만 남기기 
		List<String> nouns = list.stream()
				.filter(w -> "NN".equals(w.getPos()))
				.map(Word::getToken)
				.collect(Collectors.toList());
		System.out.println(nouns);
		
		// 얼마나 다양한 POS 태그가 존재하는지 확인 (toSet 수집기 사용)
		Set<String> pos = list.stream()
				.map(Word::getPos)
				.collect(Collectors.toSet());
		System.out.println(pos);
		
		// 문자열 결함
		String rawSentence = list.stream()
				.map(Word::getToken)
				.collect(Collectors.joining(" "));
		System.out.println(rawSentence);
		
		// 품사별 단어 분류
		Map<String, List<Word>> groupByPos = list.stream()
				.collect(Collectors.groupingBy(Word::getPos));
		System.out.println(groupByPos.get("VB"));
		System.out.println(groupByPos.get("NN"));
		
		// 컬렉션 데이터로부터 필드 값 기준 인덱싱
		Map<String, Word> tokenToWord = list.stream()
				.collect(Collectors.toMap(Word::getToken, Function.identity()));
		System.out.println(tokenToWord.get("sausage"));
		
		// 가장 긴 단어 길이 찾기 - 일반 스트림을 프리미티브 스트림으로 변환
		int maxTokenLength = list.stream()
				.mapToInt(w -> w.getToken().length())
				.max().getAsInt();
		System.out.println(maxTokenLength);
		
		// 병렬 작업
		int[] firstLengths = list.parallelStream()
				.filter(w -> w.getToken().length() % 2 == 0)
				.map(Word::getToken)
				.mapToInt(String::length)
				.sequential()
				.sorted()
				.limit(2)
				.toArray();
		System.out.println(Arrays.toString(firstLengths));
		
		// 편의 기능 - 텍스트 파일을 각 줄 스트림으로 표현
		Path path = Paths.get("data/text.txt");
		try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
			double average = lines.flatMap(line -> Arrays.stream(line.split(" ")))
					.map(String::toLowerCase)
					.mapToInt(String::length)
					.average().getAsDouble();
			System.out.println("average token length: " + average);
		}
	}
}