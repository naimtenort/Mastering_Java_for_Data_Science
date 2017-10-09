package chapter04.classification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import chapter04.RankedPageData;
import chapter04.cv.Dataset;
import chapter04.cv.Split;
import chapter04.preprocess.StandardizationPreprocessor;
import jsat.classifiers.CategoricalData;
import jsat.classifiers.CategoricalResults;
import jsat.classifiers.ClassificationDataSet;
import jsat.classifiers.DataPoint;
import jsat.classifiers.DataPointPair;
import jsat.classifiers.linear.LogisticRegressionDCD;
import jsat.classifiers.svm.SBP;
import jsat.classifiers.svm.SupportVectorLearner.CacheMode;
import jsat.classifiers.trees.RandomForest;
import jsat.distributions.kernels.KernelTrick;
import jsat.distributions.kernels.PolynomialKernel;
import jsat.linear.DenseVector;
import jsat.regression.LogisticRegression;

public class JSATTests {

	public static void main(String[] args) throws IOException {
		Dataset rankedPage = RankedPageData.readRankedPagesMatrixNoSplit();
		
		double[][] X = rankedPage.getX();
		int[] y = rankedPage.getYAsInt();
		
		// 다중 클래스 분류를 위해 더 많은 클래스로 변환
		System.out.println("========================== 다중 클래스 분류를 위해 더 많은 클래스로 변환");
		CategoricalData binary = new CategoricalData(2);
		List<DataPointPair<Integer>> data = new ArrayList<>(X.length);
		for (int i = 0; i < X.length; i++) {
			int target = y[i];
			DataPoint row = new DataPoint(new DenseVector(X[i]));
			data.add(new DataPointPair<Integer>(row, target));
		}
		
		ClassificationDataSet dataset = new ClassificationDataSet(data, binary);
		
		// 1. 랜덤 포레스트를 훈련시킨다.
		System.out.println("========================== 랜덤 포레스트를 트레이닝");
		RandomForest model = runRandomForest(dataset);
		
		for (int i = 0; i < X.length; i++) {
            DenseVector vector = new DenseVector(X[i]);
    		DataPoint point = new DataPoint(vector);
    		CategoricalResults out = model.classify(point);
    		double probability = out.getProb(1);
    		System.out.println("prob = " + probability);
        }
		
		// 2. 로지스틱 회귀 모델을 훈련시킨다.
		System.out.println("========================== 로지스틱 회귀 모델 트레이닝");
		runLogisticRegression(dataset);
		
		// 3. 정규화 로지스틱 회귀 모델을 훈련시킨다.
		System.out.println("========================== 정규화 로지스틱 회귀 모델 트레이닝");
		runLogisticsRegressionDCD(dataset);
		
		// 4. JSAT을 사용하여 다양한 테스트를 수행한다.
		System.out.println("========================== JSAT을 사용하여 다양한 테스트를 수행");
		useJSAT();
	}
	
	/**
	 * JSAT을 사용하여 다양한 테스트를 수행한다.
	 * 
	 * @throws IOException
	 */
	public static void useJSAT() throws IOException {
		Split split = RankedPageData.readRankedPagesMatrix();

        Dataset train = split.getTrain();
        Dataset test = split.getTest();

        StandardizationPreprocessor preprocessor = StandardizationPreprocessor.train(train);
        train = preprocessor.transform(train);
        test = preprocessor.transform(test);

        List<Split> folds = train.kfold(3);

        DescriptiveStatistics logreg = JSAT.crossValidate(folds, fold -> {
            LogisticRegression model = new LogisticRegression();
            model.trainC(fold.toJsatClassificationDataset());
            return model;
        });

        System.out.printf("plain logreg     auc=%.4f ± %.4f%n", logreg.getMean(),
                logreg.getStandardDeviation());

        double[] cs = { 0.0001, 0.01, 0.5, 1.0, 5.0, 10.0, 50.0, 70, 100 };
        for (double c : cs) {
            int maxIterations = 100;
            DescriptiveStatistics summary = JSAT.crossValidate(folds, fold -> {
                LogisticRegressionDCD model = new LogisticRegressionDCD();
                model.setMaxIterations(maxIterations);
                model.setC(c);
                model.trainC(fold.toJsatClassificationDataset());
                return model; 
            });

            double mean = summary.getMean();
            double std = summary.getStandardDeviation();
            System.out.printf("logreg, C=%5.1f, auc=%.4f ± %.4f%n", c, mean, std);
        }


        KernelTrick kernel = new PolynomialKernel(2);
        CacheMode cacheMode = CacheMode.FULL;

        double[] nus = { 0.3, 0.5, 0.7 };
        for (double nu : nus) {
            int maxIterations = 30;
            DescriptiveStatistics summary = JSAT.crossValidate(folds, fold -> {
                SBP sbp = new SBP(kernel, cacheMode, maxIterations, nu);
                sbp.trainC(fold.toJsatClassificationDataset());
                return sbp;
            });

            double mean = summary.getMean();
            double std = summary.getStandardDeviation();
            System.out.printf("sbp    nu=%5.1f, auc=%.4f ± %.4f%n", nu, mean, std);
        }

        DescriptiveStatistics rf = JSAT.crossValidate(folds, fold -> {
            RandomForest model = new RandomForest();
            model.setFeatureSamples(4);
            model.setMaxForestSize(150);
            model.trainC(fold.toJsatClassificationDataset());
            return model;
        });

        System.out.printf("random forest    auc=%.4f ± %.4f%n", rf.getMean(), rf.getStandardDeviation());

        LogisticRegression finalModel = new LogisticRegression();
        finalModel.trainC(train.toJsatClassificationDataset());

        double auc = JSAT.auc(finalModel, test);
        System.out.printf("final log reg    auc=%.4f%n", auc);
	}
	
	/**
	 * 랜덤 포레스트 모델을 훈련시킨다.
	 * 
	 * @param dataset 훈련 데이터
	 */
	public static RandomForest runRandomForest(ClassificationDataSet dataset) {
		RandomForest model = new RandomForest();
		model.setFeatureSamples(4);
		model.setMaxForestSize(150);
		model.trainC(dataset);
		
		return model;
	}

	/**
	 * 로지스틱 회귀 모델을 훈련시킨다.
	 * 
	 * @param dataset 훈련 데이터
	 */
	public static void runLogisticRegression(ClassificationDataSet dataset) {
		LogisticRegression model = new LogisticRegression();
		model.trainC(dataset);
	}
	
	/**
	 * 정규화 로지스틱 회귀 모델을 훈련시킨다.
	 * 
	 * @param dataset 훈련 데이터
	 */
	public static void runLogisticsRegressionDCD(ClassificationDataSet dataset) {
		int maxIterations = 100;
		double C = 0.0001;
		
		LogisticRegressionDCD model = new LogisticRegressionDCD();
		model.setMaxIterations(maxIterations);
		model.setC(C);
		model.trainC(dataset);
	}
}
