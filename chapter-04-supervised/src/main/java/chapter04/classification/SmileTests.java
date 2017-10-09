package chapter04.classification;

import java.io.IOException;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import chapter04.RankedPageData;
import chapter04.cv.Dataset;
import chapter04.cv.Split;
import chapter04.preprocess.StandardizationPreprocessor;
import smile.classification.DecisionTree.SplitRule;
import smile.classification.GradientTreeBoost;
import smile.classification.LogisticRegression;
import smile.classification.RandomForest;
import smile.classification.SVM;
import smile.math.kernel.MercerKernel;
import smile.math.kernel.PolynomialKernel;

public class SmileTests {

	public static void main(String[] args) throws IOException {
		Dataset rankedPage = RankedPageData.readRankedPagesMatrixNoSplit();

		// 1. 랜덤 포레스트를 실행시킨다.
		System.out.println("========================== 랜덤 포레스트를 트레이닝");
		RandomForest model = runRandomForest(rankedPage);
		
		System.out.println("========================== 레이블 별 확률값 계산");
		double[][] X = rankedPage.getX();
		
		double[] result = new double[X.length];
		double[] probs = new double[2];
        for (int i = 0; i < X.length; i++) {
            int prediction = model.predict(X[i], probs);
            System.out.println(prediction + " : " + probs[0] + "," + probs[1]);
            result[i] = probs[1];
        }

		// 2. 로지스틱 회귀 모델을 실행 테스트
        System.out.println("========================== 로지스틱 회귀 모델 트레이닝");
		runLogisticRegression(rankedPage);

		// 3. 스마일을 사용하여 로지스틱 회귀 모델의 일반적인 절차를 실행한다.
		System.out.println("========================== 스마일을 사용하여 로지스틱 회귀 모델의 일반적인 절차를 실행");
		useSmile();
	}
	
	/**
	 * 스마일 사용 비지도 학습
	 * 
	 * @throws IOException
	 */
	public static void useSmile() throws IOException {
		Split split = RankedPageData.readRankedPagesMatrix();

        Dataset train = split.getTrain();
        Dataset test = split.getTest();

        StandardizationPreprocessor preprocessor = StandardizationPreprocessor.train(train);
        train = preprocessor.transform(train);
        test = preprocessor.transform(test);

        List<Split> folds = train.kfold(3);

        double[] lambdas = { 0, 0.5, 1.0, 5.0, 10.0, 100.0, 1000.0 };
        for (double lambda : lambdas) {
            DescriptiveStatistics summary = Smile.crossValidate(folds, fold -> {
                return new LogisticRegression.Trainer()
                        .setRegularizationFactor(lambda)
                        .train(fold.getX(), fold.getYAsInt());
            });

            double mean = summary.getMean();
            double std = summary.getStandardDeviation();
            System.out.printf("logreg, λ=%8.3f, auc=%.4f ± %.4f%n", lambda, mean, std);
        }

        MercerKernel<double[]> kernel = new PolynomialKernel(2);

        double[] Cs = { 0.001, 0.01, 0.1 };
        for (double C : Cs) {
            DescriptiveStatistics summary = Smile.crossValidate(folds, fold -> {
                double[][] X = fold.getX();
                int[] y = fold.getYAsInt();
                SVM<double[]> svm = new SVM.Trainer<double[]>(kernel, C).train(X, y);
                svm.trainPlattScaling(X, y);
                return svm;
            });

            double mean = summary.getMean();
            double std = summary.getStandardDeviation();
            System.out.printf("svm     C=%8.3f, auc=%.4f ± %.4f%n", C, mean, std);
        }

        DescriptiveStatistics rf = Smile.crossValidate(folds, fold -> {
            return new RandomForest.Trainer()
                    .setNumTrees(100)
                    .setNodeSize(4)
                    .setSamplingRates(0.7)
                    .setSplitRule(SplitRule.ENTROPY)
                    .setNumRandomFeatures(3)
                    .train(fold.getX(), fold.getYAsInt());
        });

        System.out.printf("random forest       auc=%.4f ± %.4f%n", rf.getMean(), rf.getStandardDeviation());

        DescriptiveStatistics gbt = Smile.crossValidate(folds, fold -> {
            return new GradientTreeBoost.Trainer()
                    .setMaxNodes(100)
                    .setSamplingRates(0.7)
                    .setShrinkage(0.01)
                    .setNumTrees(100)
                    .train(fold.getX(), fold.getYAsInt());
        });

        System.out.printf("gbt                 auc=%.4f ± %.4f%n", gbt.getMean(), gbt.getStandardDeviation());

        RandomForest rfFinal = new RandomForest.Trainer()
                .setNumTrees(100)
                .setNodeSize(4)
                .setSamplingRates(0.7)
                .setSplitRule(SplitRule.ENTROPY)
                .setNumRandomFeatures(3)
                .train(train.getX(), train.getYAsInt());

        double finalAuc = Smile.auc(rfFinal, test);
        System.out.printf("final rf            auc=%.4f%n", finalAuc);

        LogisticRegression logregFinal = new LogisticRegression.Trainer()
                .setRegularizationFactor(100.0)
                .train(train.getX(), train.getYAsInt());

        finalAuc = Smile.auc(logregFinal, test);
        System.out.printf("final logreg        auc=%.4f%n", finalAuc);
	}
	
	/**
	 * 랜덤 포레스트 모델을 훈련시킨다.
	 * 
	 * @param train 훈련 데이터
	 */
	public static RandomForest runRandomForest(Dataset train) {
		double[][] X = train.getX();
		int[] y = train.getYAsInt();
		
		RandomForest model = new RandomForest.Trainer()
				.setNumTrees(100)
				.setNodeSize(4)
				.setSamplingRates(0.7)
				.setSplitRule(SplitRule.ENTROPY)
				.setNumRandomFeatures(3)
				.train(X, y);
		
		return model;		
	}
	
	/**
	 * 로지스틱 회귀 모델을 훈련시킨다.
	 * 
	 * @param train 훈련 데이터
	 */
	public static void runLogisticRegression(Dataset train) {
		double[][] X = train.getX();
		int[] y = train.getYAsInt();
		double lambda = 0.5;
		
		LogisticRegression lr = new LogisticRegression.Trainer()
		        .setRegularizationFactor(lambda)
		        .train(X, y);
	}
}
