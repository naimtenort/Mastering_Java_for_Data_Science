package chapter04.regression;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.google.common.base.Stopwatch;

import chapter04.cv.Dataset;
import chapter04.cv.Split;
import smile.regression.LASSO;
import smile.regression.OLS;
import smile.regression.RandomForest;
import smile.regression.Regression;
import smile.regression.RidgeRegression;
import smile.validation.MSE;

public class SmileTests {

	public static void main(String[] args) throws IOException {
		Path path = Paths.get("data/performance.bin");
		Dataset data = read(path);

		// 간단한 OLS 생성
		System.out.println("==================== 간단한 OLS 생성");
		OLS ols = new OLS(data.getX(), data.getY());
		
		// 라소, 능형회귀 
		System.out.println("==================== 라소, 능형회귀");
		double lambda = 0.01;
		RidgeRegression ridge = new RidgeRegression(data.getX(), data.getY(), lambda);
		LASSO lasso = new LASSO(data.getX(), data.getY(), lambda);

		// 랜덤 포레스트
		System.out.println("==================== 랜덤 포레스트");
		int nbtrees = 100;
		RandomForest rf = new RandomForest.Trainer(nbtrees)
		        .setNumRandomFeatures(15)
		        .setMaxNodes(128)
		        .setNodeSize(10)
		        .setSamplingRates(0.6)
		        .train(data.getX(), data.getY());
		
		for (int i = 0; i < data.getX().length; i++) {
			double[] row = data.getX()[i];
            double result = rf.predict(row);
            System.out.println(result);
        }
		
		// 스마일을 사용한 회귀 모델 테스트
		useSmile();
	}
    
	/**
	 * 스마일을 사용한 회귀 모델 테스트
	 * 
	 * @throws IOException
	 */
    public static void useSmile() throws IOException {
    	Path path = Paths.get("data/performance.bin");
        Dataset dataset = read(path);

        Split trainTestSplit = dataset.shuffleSplit(0.3);
        Dataset train = trainTestSplit.getTrain();
        Dataset test = trainTestSplit.getTest();

        List<Split> folds = train.shuffleKFold(3);

        DescriptiveStatistics baseline = crossValidate(folds, data -> mean(data));
        System.out.printf("baseline: rmse=%.4f ± %.4f%n", baseline.getMean(), baseline.getStandardDeviation());

        DescriptiveStatistics ols = crossValidate(folds, data -> {
            return new OLS(data.getX(), data.getY());
        });

        System.out.printf("ols:      rmse=%.4f ± %.4f%n", ols.getMean(), ols.getStandardDeviation());

        double[] lambdas = { 0.1, 1, 10, 100, 1000, 5000, 10000, 20000 };
        for (double lambda : lambdas) {
            Stopwatch stopwatch = Stopwatch.createStarted();

            DescriptiveStatistics summary = crossValidate(folds, data -> {
                return new LASSO(data.getX(), data.getY(), lambda);
            });

            double mean = summary.getMean();
            double std = summary.getStandardDeviation();

            System.out.printf("lasso λ=%9.1f, rmse=%.4f ± %.4f (took %s)%n", 
                    lambda, mean, std, stopwatch.stop());
        }

        DescriptiveStatistics rf = crossValidate(folds, data -> {
            int nbtrees = 100;
            return new RandomForest.Trainer(nbtrees)
                    .setNumRandomFeatures(15)
                    .setMaxNodes(128)
                    .setNodeSize(10)
                    .setSamplingRates(0.6)
                    .train(data.getX(), data.getY());
        });
        System.out.printf("rf:       rmse=%.4f ± %.4f%n", rf.getMean(), rf.getStandardDeviation());

        OLS finalOls = new OLS(train.getX(), train.getY(), true);
        double testRmse = rmse(finalOls, test);
        System.out.printf("final rmse=%.4f%n", testRmse);
    }

    private static Regression<double[]> mean(Dataset data) {
        double meanTarget = Arrays.stream(data.getY()).average().getAsDouble();
        return x -> meanTarget;
    }

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

    private static double rmse(Regression<double[]> model, Dataset dataset) {
        double[] prediction = predict(model, dataset);
        double[] truth = dataset.getY();

        double mse = new MSE().measure(truth, prediction);
        return Math.sqrt(mse);
    }

    public static double[] predict(Regression<double[]> model, Dataset dataset) {
        double[][] X = dataset.getX();
        double[] result = new double[X.length];

        for (int i = 0; i < X.length; i++) {
            result[i] = model.predict(X[i]);
        }

        return result;
    }

    private static Dataset read(Path path) throws IOException {
        if (!path.toFile().exists()) {
            CaseStudyTests.prepareData();
        }

        try (InputStream is = Files.newInputStream(path)) {
            return SerializationUtils.deserialize(is);
        }
    }

}
