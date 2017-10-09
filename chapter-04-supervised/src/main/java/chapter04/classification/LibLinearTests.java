package chapter04.classification;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import chapter04.RankedPageData;
import chapter04.cv.Dataset;
import chapter04.cv.Split;
import chapter04.preprocess.StandardizationPreprocessor;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

public class LibLinearTests {
	
	public static void main(String[] args) throws IOException {
		SolverType solverType = SolverType.L1R_LR;
		double C = 0.001;
		double eps = 0.0001;
		Parameter param = new Parameter(solverType, C, eps);
		
		Dataset rankedPage = RankedPageData.readRankedPagesMatrixNoSplit();
		
		// 1. 로지스틱 회귀 모델 생성 테스트.
//		predictValues(train(train, param), train);
		double[] proba = predictProba(train(rankedPage, param), rankedPage);
		
		// 2. LibLinear 사용 다양한 테스트 수행
		useLibLinear();
	}
	
	/**
	 * LibLinear를 사용하여 다양한 테스트를 수행한다.
	 * 
	 * @throws IOException
	 */
	public static void useLibLinear() throws IOException {
        Split split = RankedPageData.readRankedPagesMatrix();

        Dataset train = split.getTrain();
        Dataset test = split.getTest();

        StandardizationPreprocessor preprocessor = StandardizationPreprocessor.train(train);
        train = preprocessor.transform(train);
        test = preprocessor.transform(test);

        LibLinearTests.mute();

        List<Split> folds = train.kfold(3);

        SolverType[] solvers = { SolverType.L1R_LR, SolverType.L2R_LR, SolverType.L1R_L2LOSS_SVC,
                SolverType.L2R_L2LOSS_SVC };
        double[] Cs = { 0.01, 0.02, 0.03, 0.05, 0.1, 0.5, 1.0, 10.0, 100.0 };

        for (SolverType solver : solvers) {
            for (double C : Cs) {
                DescriptiveStatistics summary = LibLinearTests.crossValidate(folds, fold -> {
                    Parameter param = new Parameter(solver, C, 0.0001);
                    return LibLinearTests.train(fold, param);
                });

                double mean = summary.getMean();
                double std = summary.getStandardDeviation();
                System.out.printf("%15s  C=%7.3f, auc=%.4f ± %.4f%n", solver, C, mean, std);
            }
        }

        Parameter param = new Parameter(SolverType.L1R_LR, 0.05, 0.0001);
        Model finalModel = LibLinearTests.train(train, param);
        double finalAuc = LibLinearTests.auc(finalModel, test);
        System.out.printf("final logreg        auc=%.4f%n", finalAuc);
    }

	/**
	 * 로그를 끈다.
	 */
    public static void mute() {
        PrintStream devNull = new PrintStream(new NullOutputStream());
        Linear.setDebugOutput(devNull);
    }

    /**
     * 교차 검증을 수행한다.
     * 
     * @param folds K개로 쪼개진 데이터
     * @param trainer 검증 함수
     * @return 교차 검증 결과
     */
    public static DescriptiveStatistics crossValidate(List<Split> folds, Function<Dataset, Model> trainer) {
        double[] aucs = folds.parallelStream().mapToDouble(fold -> {
            Dataset foldTrain = fold.getTrain();
            Dataset foldValidation = fold.getTest();
            Model model = trainer.apply(foldTrain);
            return auc(model, foldValidation);
        }).toArray();

        return new DescriptiveStatistics(aucs);
    }

    /**
     * 모델을 훈련시킨다.
     * @param dataset 데이터 셋
     * @param param 트레이닝 매개변수
     * @return {@link Model}
     */
    public static Model train(Dataset dataset, Parameter param) {
        Problem problem = wrapDataset(dataset);
        return Linear.train(problem, param);
    }

    /**
     * 레이블과 데이터를 사용하여 {@link Problem} 객체 생성
     * @param dataset 데이터 셋
     * @return {@link Problem}
     */
    private static Problem wrapDataset(Dataset dataset) {
        double[][] X = dataset.getX();
        double[] y = dataset.getY();

        Problem problem = new Problem();
        problem.x = wrapMatrix(X);
        problem.y = y;
        problem.n = X[0].length + 1;
        problem.l = X.length;

        return problem;
    }

    /**
     * AUC를 구한다.
     * 
     * @param model 선형 모델
     * @param dataset 데이터 셋
     * @return AUC
     */
    public static double auc(Model model, Dataset dataset) {
        double[] scores;
        if (model.isProbabilityModel()) {
            scores = predictProba(model, dataset);
        } else {
            scores = predictValues(model, dataset);
            scores = sigmoid(scores);
        }

        return Metrics.auc(dataset.getY(), scores);
    }

    /**
     * 훈련한 모델을 사용하여 데이터 분류
     * @param model 훈련한 모델
     * @param dataset 새로운 데이터 셋
     * @return 확률 값 배열 
     */
    public static double[] predictProba(Model model, Dataset dataset) {
        int n = dataset.length();

        double[][] X = dataset.getX();
        double[] results = new double[n];
        double[] probs = new double[2];

        for (int i = 0; i < n; i++) {
            Feature[] row = wrapRow(X[i]);
            Linear.predictProbability(model, row, probs);
            results[i] = probs[1];
        }

        return results;
    }

    /**
     * SVM 모델의 경우 확률값을 얻을 수 없기 때문에 원시 데이터를 출력물로 가져옴
     * @param model 훈련한 모델
     * @param dataset 새로운 데이터 셋
     * @return 원시 데이터 값 배열
     */
    public static double[] predictValues(Model model, Dataset dataset) {
        int n = dataset.length();

        double[][] X = dataset.getX();
        double[] results = new double[n];
        double[] values = new double[1];

        for (int i = 0; i < n; i++) {
            Feature[] row = wrapRow(X[i]);
            Linear.predictValues(model, row, values);
            results[i] = values[0];
        }

        return results;
    }

    /**
     * 결과 값을 0~1 사이로 매핑 한다.
     * @param scores 실제 결과 값
     * @return 0~1 사이의 결과 값
     */
    public static double[] sigmoid(double[] scores) {
        double[] result = new double[scores.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = 1 / (1 + Math.exp(-scores[i]));
        }

        return result;
    }

    /**
     * 다중 행 행렬 데이터 변환
     * @param X 다중 행 행렬
     * @return {@link Feature} 이중배열
     */
    private static Feature[][] wrapMatrix(double[][] X) {
        int n = X.length;
        Feature[][] matrix = new Feature[n][];
        for (int i = 0; i < n; i++) {
            matrix[i] = wrapRow(X[i]);
        }
        return matrix;
    }

    /**
     * 단일 행 데이터 변환
     * @param row 단일 행 데이터
     * @return {@link Feature} 배열
     */
    private static Feature[] wrapRow(double[] row) {
        int m = row.length;
        Feature[] result = new Feature[m];

        for (int i = 0; i < m; i++) {
            result[i] = new FeatureNode(i + 1, row[i]);
        }

        return result;
    }
}
