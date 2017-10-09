package chapter04.regression;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.primitives.Doubles;

import chapter04.cv.Dataset;
import chapter04.cv.Split;
import joinery.DataFrame;
import smile.regression.LASSO;
import smile.regression.OLS;
import smile.regression.Regression;
import smile.validation.MSE;

public class CaseStudyTests {

	public static void main(String[] args) throws IOException {
		// 1. 하드웨어 성능측정 사례연구의 데이터를 수집한다
		prepareData();
	}

    /**
     * 하드웨어 성능측정 사례연구의 데이터를 수집한다.
     * 
     * @throws IOException
     */
    public static void prepareData() throws IOException {
		// 특성 정보를 읽는다.
		DataFrame<Object> dataframe = DataFrame.readCsv("data/performance/x_train.csv");

		// 레이블 값을 읽는다.
		DataFrame<Object> targetDf = DataFrame.readCsv("data/performance/y_train.csv");
		List<Double> targetList = targetDf.cast(Double.class).col("time");
		double[] target = Doubles.toArray(targetList);

		// 누락된 값을 제거한다.
        List<Object> memfreq = noneToNull(dataframe.col("memFreq"));
        List<Object> memtRFC = noneToNull(dataframe.col("memtRFC"));
        dataframe = dataframe.drop("memFreq", "memtRFC");
        dataframe.add("memFreq", memfreq);
        dataframe.add("memtRFC", memtRFC);

        // 원본 프레임을 포함하는 데이터 프레임 생성
        List<Object> types = dataframe.types().stream().map(c -> c.getSimpleName()).collect(Collectors.toList());
        List<Object> columns = new ArrayList<>(dataframe.columns());
        DataFrame<Object> typesDf = new DataFrame<>();
        typesDf.add("column", columns);
        typesDf.add("type", types);

        // String 유형만을 선택한다.
        DataFrame<Object> stringTypes = typesDf.select(p -> p.get(1).equals("String"));
        System.out.println(stringTypes.toString());

        // 범주형 변수를 얻는다.
        DataFrame<Object> categorical = dataframe.retain(stringTypes.col("column").toArray());
        System.out.println(categorical);

        dataframe = dataframe.drop(stringTypes.col("column").toArray());

        // 범주형 데이터의 숫자를 센다.
        for (Object column : categorical.columns()) {
            List<Object> data = categorical.col(column);
            Multiset<Object> counts = HashMultiset.create(data);
            int nunique = counts.entrySet().size();
            Multiset<Object> countsSorted = Multisets.copyHighestCountFirst(counts);

            System.out.print(column + "\t" + nunique + "\t" + countsSorted);
            List<Object> cleaned = data.stream()
                    .map(o -> counts.count(o) >= 50 ? o : "OTHER")
                    .collect(Collectors.toList());

            dataframe.add(column, cleaned);
        }

        System.out.println(dataframe.head());

        // 원 핫 인코딩 스키마를 적용한다.
        double[][] X = dataframe.toModelMatrix(0.0);
        Dataset dataset = new Dataset(X, target);

        Path path = Paths.get("data/performance.bin");
        try (OutputStream os = Files.newOutputStream(path)) {
            SerializationUtils.serialize(dataset, os);
        }
        
        // 교차 검증과 홀드아웃 설정
        Split trainTestSplit = dataset.shuffleSplit(0.3);
        Dataset train = trainTestSplit.getTrain();
        Dataset test = trainTestSplit.getTest();
        List<Split> folds = train.shuffleKFold(3);
        
        // 교차 검증 수행
        DescriptiveStatistics baseline = crossValidate(folds, data -> mean(data));
        System.out.printf("baseline: rmse=%.4f &pm; %.4f%n", baseline.getMean(),
        baseline.getStandardDeviation());
        
        // 최소자승법 회귀 사용
		DescriptiveStatistics ols = crossValidate(folds, data -> {
			return new OLS(data.getX(), data.getY());
			//return new OLS(data.getX(), data.getY(), true);
		});
		System.out.printf("ols: rmse=%.4f &pm; %.4f%n", ols.getMean(), ols.getStandardDeviation());

		// 다양한 람다 값을 사용하여 라소 모델 실행
		double[] lambdas = { 0.1, 1, 10, 100, 1000, 5000, 10000, 20000 };
		for (double lambda : lambdas) {
		    DescriptiveStatistics summary = crossValidate(folds, data -> {
		        return new LASSO(data.getX(), data.getY(), lambda);
		    });
		    double mean = summary.getMean();
		    double std = summary.getStandardDeviation();
		    System.out.printf("lasso λ=%9.1f, rmse=%.4f &pm; %.4f%n", lambda, mean, std);
		}
		
		// 최종 모델 선택
		OLS olsModel = new OLS(train.getX(), train.getY(), true);
		double testRmse = rmse(olsModel, test);
		System.out.printf("final rmse=%.4f%n", testRmse);

    }
    
    /**
     * 교차 검증을 수행한다.
     * 
     * @param folds 분할 데이터
     * @param trainer 검증 함수
     * @return 교차 검증 결과
     */
	public static DescriptiveStatistics crossValidate(List<Split> folds,
			Function<Dataset, Regression<double[]>> trainer) {
		double[] aucs = folds.parallelStream().mapToDouble(fold -> {
			Dataset train = fold.getTrain();
			Dataset validation = fold.getTest();
			Regression<double[]> model = trainer.apply(train);
			return rmse(model, validation);
		}).toArray();
		return new DescriptiveStatistics(aucs);
	}
	
	/**
	 * 평균 제곱근 편차를 구한다.
	 * 
	 * @param model 회귀 모델
	 * @param dataset 데이터 셋
	 * @return 평균 제곱근 편차
	 */
	private static double rmse(Regression<double[]> model, Dataset dataset) {
        double[] prediction = predict(model, dataset);
        double[] truth = dataset.getY();

        double mse = new MSE().measure(truth, prediction);
        return Math.sqrt(mse);
    }
	
	/**
	 * 평균을 예측한다.
	 * @param data 데이터 셋
	 * @return 회귀 모델
	 */
	private static Regression<double[]> mean(Dataset data) {
	    double meanTarget = Arrays.stream(data.getY()).average().getAsDouble();
	    return x -> meanTarget;
	}

	/**
	 * 값을 예측한다.
	 * 
	 * @param model 회귀 모델
	 * @param dataset 데이터 셋
	 * @return 예측 값
	 */
	public static double[] predict(Regression<double[]> model, Dataset dataset) {
        double[][] X = dataset.getX();
        double[] result = new double[X.length];

        for (int i = 0; i < X.length; i++) {
            result[i] = model.predict(X[i]);
        }

        return result;
    }

    /**
     * 문자열 "None"을 null로 변환한다.
     * 
     * @param memfreq 변환 대상
     * @return 변환 된 데이터
     */
    private static List<Object> noneToNull(List<Object> memfreq) {
        return memfreq.stream()
                .map(s -> isNone(s) ? null : Double.parseDouble(s.toString()))
                .collect(Collectors.toList());
    }

    /**
     * 문자열이 "None"인지 확인
     * 
     * @param s 대상 문자열
     * @return "None" 여부
     */
    private static boolean isNone(Object s) {
        return "None".equals(s);
    }
}
