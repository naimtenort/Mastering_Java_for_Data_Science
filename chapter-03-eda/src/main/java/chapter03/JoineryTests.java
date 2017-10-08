package chapter03;

import java.io.IOException;
import java.util.List;

import joinery.DataFrame;

public class JoineryTests {

	public static void main(String[] args) throws IOException {
		System.out.println("====================== Joinery 사용");
		DataFrame<Object> df = getDataFrame();
		System.out.println("====================== 그룹 별 평균값 구하기");
		getMeanPerPage(df);
	}
	
	public static DataFrame<Object> getDataFrame() throws IOException {
		List<RankedPage> pages = Data.readRankedPages();
		DataFrame<Object> df = BeanToJoinery.convert(pages, RankedPage.class);
		DataFrame<Object> drop = df.retain("bodyContentLength", "titleLength", "numberOfHeaders");
		DataFrame<Object> describe = drop.describe();
		System.out.println(describe.toString());
		
		return df;
	}
	
	/**
	 * 입력받은 데이터 프레임 객체로 부터 페이지별 평균값을 추출한다.
	 * 
	 * @param df 입력 데이터 프레임
	 */
	public static void getMeanPerPage(DataFrame<Object> df) {
		DataFrame<Object> meanPerPage = df.groupBy("page").mean()
		        .drop("position")
		        .sortBy("page")
		        .transpose();
		System.out.println(meanPerPage);
	}

}
