package chapter02;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.jooq.lambda.tuple.Tuple2;

import com.aol.cyclops.control.LazyReact;
import com.aol.cyclops.types.futurestream.LazyFutureStream;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;

public class ExtensionLibraryTests {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		// 커먼즈 IO IOUtils 테스트
		try (InputStream is = new FileInputStream("data/text.txt")) {
			String content = IOUtils.toString(is, StandardCharsets.UTF_8);
			System.out.println(content);
		}
		try (InputStream is = new FileInputStream("data/text.txt")) {
			List<String> lines = IOUtils.readLines(is, StandardCharsets.UTF_8);
			System.out.println(lines);
		}
		
		// 구글 구아바 Files 테스트
		File file = new File("data/words.txt");
		CharSource wordsSource = Files.asCharSource(file, StandardCharsets.UTF_8);
		List<String> lines = wordsSource.readLines();
		System.out.println(lines);
		
		// Lists 클래스의 transform 메소드
		List<Word> words = Lists.transform(lines, line -> {
			String[] split = line.split("\t");
			return new Word(split[0].toLowerCase(), split[1]);
		});
		System.out.println(words);
		
		// Multiset을 사용한 품사별 개수 세기
		Multiset<String> pos = HashMultiset.create();
		for (Word word : words) {
			pos.add(word.getPos());
		}
		System.out.println(pos);
		
		// Multiset 빈도 순 정렬
		Multiset<String> sortedPos = Multisets.copyHighestCountFirst(pos);
		System.out.println(sortedPos);
		
		// 품사별 단어 목록 확인
		ArrayListMultimap<String, String> wordsByPos = ArrayListMultimap.create();
		for (Word word : words) {
			wordsByPos.put(word.getPos(), word.getToken());
		}
		System.out.println(wordsByPos);
		
		// 품사별 단어 목록 확인 - 자바 표준 API 사용
		Map<String, Collection<String>> wordsByPosMap = wordsByPos.asMap();
		wordsByPosMap.entrySet().forEach(System.out::println);
		
		// Table 객체를 사용한 (단어, 품사) 쌍 개수 확인
		Table<String, String, Integer> table = HashBasedTable.create();
		for (Word word : words) {
			Integer cnt = table.get(word.getPos(), word.getToken());
			if (cnt == null) {
				cnt = 0;
			}
			table.put(word.getPos(), word.getToken(), cnt + 1);
		}
		System.out.println(table);
		
		// Table 객체의 행, 열 데이터 개별 접근
		Map<String, Integer> nouns = table.row("NN");
		System.out.println(nouns);
		String word = "eu";
		Map<String, Integer> posTags = table.column(word);
		System.out.println(posTags);
		
		// 원시 데이터 형태의 컬렉션을 원시 데이터 배열로 변환
		Collection<Integer> values = nouns.values();
		int[] nounCounts = Ints.toArray(values);
		int totalNounCount = Arrays.stream(nounCounts).sum();
		System.out.println(totalNounCount);
		
		// Ordering을 사용한 정렬 작업
		Ordering<Word> byTokenLength 
			= Ordering.natural().<Word>onResultOf(w -> w.getToken().length()).reverse();
		List<Word> sortedByLength = byTokenLength.immutableSortedCopy(words);
		System.out.println(sortedByLength);
		
		// Ordering를 사용하지 않는 표준 API 사용 방식, 비교기가 사용되는 예제
		List<Word> sortedCopy = new ArrayList<>(words);
		Collections.sort(sortedCopy, byTokenLength);
		System.out.println(sortedCopy);
		
		// 상위 10건, 하위 10건 반환 기능
		List<Word> first10 = byTokenLength.leastOf(words, 10);
		System.out.println(first10);
		List<Word> last10 = byTokenLength.greatestOf(words, 10);
		System.out.println(last10);
		
		// AOL 싸이클롭스 리액트
		LineIterator it = FileUtils.lineIterator(new File("data/words.txt"), "UTF-8");
		ExecutorService executor = Executors.newCachedThreadPool();
		LazyFutureStream<String> stream = 
				LazyReact.parallelBuilder().withExecutor(executor).from(it);
		
		Map<String, Integer> map = stream
				.map(line -> line.split("\t"))
				.map(arr -> arr[1].toLowerCase())
				.distinct()
				.zipWithIndex()
				.toMap(Tuple2::v1, t -> t.v2.intValue());
		
		System.out.println(map);
		executor.shutdown();
		it.close();
	}

}
