package chapter03;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class ApacheCommonsMathTest {

	public static void main(String[] args) throws IOException {
		List<RankedPage> data = Data.readRankedPages();
		
		System.out.println("====================== SummaryStatistics");
		useSummaryStatistics(data);
		System.out.println("====================== DescriptiveStatistics");
		useDescriptiveStatistics(data);
		System.out.println("====================== 바디 내용 길이가 0인 페이지의 비율 계산");
		getProportionZeroBodyContentsLength(data);
		
		// 검색 결과 페이지 별 RankedPage 객체를 그룹화
		System.out.println("====================== 검색 결과 페이지 별 RankedPage 객체를 그룹화");
		Map<Integer, List<RankedPage>> byPage = data.stream()
    	        .filter(p -> p.getBodyContentLength() != 0)
    	        .collect(Collectors.groupingBy(RankedPage::getPage));
		
		//System.out.println(byPage);
		
		// 그룹 데이터별 평균 계산
		System.out.println("====================== 그룹 데이터별 평균 계산");
		List<DescriptiveStatistics> stats = byPage.entrySet().stream()
		        .sorted(Map.Entry.comparingByKey())
		        .map(e -> calculate(e.getValue(), RankedPage::getBodyContentLength))
		        .collect(Collectors.toList());
		
		//System.out.println(stats);
		
		System.out.println("====================== 그룹별 통계치 출력");
		displayResult(byPage, stats);
	}
	
	/**
	 * {@link SummaryStatistics} 활용, 요약 통계값 추출
	 * 
	 * @param data 입력 데이터
	 */
	private static void useSummaryStatistics(List<RankedPage> data) {
		SummaryStatistics statistics = new SummaryStatistics();
		data.stream().mapToDouble(RankedPage::getBodyContentLength)
		    .forEach(statistics::addValue);
		System.out.println(statistics.getSummary());
	}
	
	/**
	 * {@link DescriptiveStatistics} 활용, 요약 통계값 추출
	 * 
	 * @param data 입력 데이터
	 */
	private static void useDescriptiveStatistics(List<RankedPage> data) {
		double[] dataArray = data.stream()
		        .mapToDouble(RankedPage::getBodyContentLength)
		        .toArray();
		DescriptiveStatistics desc = new DescriptiveStatistics(dataArray);
		System.out.printf("min: %9.1f%n", desc.getMin());
		System.out.printf("p05: %9.1f%n", desc.getPercentile(5));
		System.out.printf("p25: %9.1f%n", desc.getPercentile(25));
		System.out.printf("p50: %9.1f%n", desc.getPercentile(50));
		System.out.printf("p75: %9.1f%n", desc.getPercentile(75));
		System.out.printf("p95: %9.1f%n", desc.getPercentile(95));
		System.out.printf("max: %9.1f%n", desc.getMax());
	}
	
	/**
	 * 바디 내용 길이가 0인 페이지의 비율 계산
	 * 
	 * @param data 입력 데이터
	 */
	private static void getProportionZeroBodyContentsLength(List<RankedPage> data) {
		double proportion = data.stream()
		        .mapToInt(p -> p.getBodyContentLength() == 0 ? 1 : 0)
		        .average().getAsDouble();
		System.out.printf("내용 길이가 0인 비율 : %.5f%n", proportion);
	}
	
	/**
	 * 컬렉션 데이터를 입력받아 특정 메소드를 실행시켜 {@link DescriptiveStatistics} 객체로 생성
	 * 
	 * @param data 컬렉션 데이터
	 * @param getter 실행 메소드
	 * @return {@link DescriptiveStatistics}
	 */
	private static DescriptiveStatistics calculate(List<RankedPage> data
	        , ToDoubleFunction<RankedPage> getter) {
	    double[] dataArray = data.stream().mapToDouble(getter).toArray();
	    return new DescriptiveStatistics(dataArray);
	}
	
	/**
	 * 검색 페이지 별 그룹화 데이터와 길이가 0인 데이터를 제거한 통계 데이터를 활용하여 결과를 출력한다.
	 * 
	 * @param byPage 검색 페이지 별 그룹화 데이터
	 * @param stats 길이가 0인 데이터를 제거하여 평균을 구한 데이터
	 */
	private static void displayResult(Map<Integer, List<RankedPage>> byPage, List<DescriptiveStatistics> stats) {
	    Map<String, Function<DescriptiveStatistics, Double>> functions 
	            = new LinkedHashMap<>();
	    functions.put("min", d -> d.getMin());
	    functions.put("p05", d -> d.getPercentile(5));
	    functions.put("p25", d -> d.getPercentile(25));
	    functions.put("p50", d -> d.getPercentile(50));
	    functions.put("p75", d -> d.getPercentile(75));
	    functions.put("p95", d -> d.getPercentile(95));
	    functions.put("max", d -> d.getMax());
	    System.out.print("page");
	    for (Integer page : byPage.keySet()) {
	        System.out.printf("%9d ", page);
	    }
	    System.out.println();
	    for (Entry<String, Function<DescriptiveStatistics, Double>> pair : functions.entrySet()) {
	        System.out.print(pair.getKey());
	        Function<DescriptiveStatistics, Double> function = pair.getValue();
	        System.out.print(" ");
	        for (DescriptiveStatistics ds : stats) {
	            System.out.printf("%9.1f ", function.apply(ds));
	        }
	        System.out.println();
	    }
	}
}
