package chapter04;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.jr.ob.JSON;
import com.google.common.base.Throwables;

import chapter04.cv.Dataset;
import chapter04.cv.Split;
import joinery.DataFrame;

public class RankedPageData {

	/**
	 * 파일을 읽어 객체 리스트를 생성한다.
	 * 
	 * @return {@link List}
	 * @throws IOException
	 */
    public static List<RankedPage> readRankedPages() throws IOException {
        Path path = Paths.get("./data/ranked-pages.json");
        try (Stream<String> lines = Files.lines(path)) {
            return lines.map(line -> parseJson(line)).collect(Collectors.toList());
        }
    }

    /**
     * JSON을 파싱한다.
     * 
     * @param line 파싱 대상 문자열
     * @return 파싱 객체
     */
    public static RankedPage parseJson(String line) {
        try {
            return JSON.std.beanFrom(RankedPage.class, line);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * 파일로 부터 읽은 데이터를 훈련용, 테스트용 데이터로 분리한다.
     * 
     * @return {@link Split}
     * @throws IOException
     */
    public static Split readRankedPagesMatrix() throws IOException {
        Dataset dataset = readRankedPagesMatrixNoSplit();
        return dataset.trainTestSplit(0.2);
    }
    
    /**
     * 파일로 부터 데이터를 읽는다.
     * 
     * @return {@link Dataset}
     * @throws IOException
     */
    public static Dataset readRankedPagesMatrixNoSplit() throws IOException {
    	List<RankedPage> pages = RankedPageData.readRankedPages();
        DataFrame<Object> dataframe = BeanToJoinery.convert(pages, RankedPage.class);

        List<Object> page = dataframe.col("page");
        double[] target = page.stream().mapToInt(o -> (int) o).mapToDouble(p -> (p == 0) ? 1.0 : 0.0).toArray();

        dataframe = dataframe.drop("page", "url", "position");
        double[][] X = dataframe.toModelMatrix(0.0);

        Dataset dataset = new Dataset(X, target);
        return dataset;
    }

}
