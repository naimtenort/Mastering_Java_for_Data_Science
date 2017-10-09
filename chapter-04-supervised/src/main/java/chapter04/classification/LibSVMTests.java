package chapter04.classification;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import chapter04.RankedPageData;
import chapter04.cv.Dataset;
import chapter04.cv.Split;
import chapter04.preprocess.StandardizationPreprocessor;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

public class LibSVMTests {
	
	public static void main(String[] args) throws IOException {
		Dataset rankedPage = RankedPageData.readRankedPagesMatrixNoSplit();
		
		predictProba(train(rankedPage, polynomialSVC(2, 0.001)), rankedPage);
		
		// LibSVM을 사용하여 다양한 테스트를 수행한다.
		useLibSvm();
	}
	
	/**
	 * LibSVM을 사용하여 다양한 테스트를 수행한다.
	 * 
	 * @throws IOException
	 */
	public static void useLibSvm() throws IOException {
		Split split = RankedPageData.readRankedPagesMatrix();

        Dataset train = split.getTrain();
        Dataset test = split.getTest();

        StandardizationPreprocessor preprocessor = StandardizationPreprocessor.train(train);
        train = preprocessor.transform(train);
        test = preprocessor.transform(test);

//        LibSVMTests.mute();

        List<Split> folds = train.kfold(3);

        System.out.println("========================== 선형 커널");
        double[] Cs = { 0.001, 0.01, 0.1, 0.5, 1.0, 10.0, 20.0 };
        for (double C : Cs) {
            DescriptiveStatistics summary = LibSVMTests.crossValidate(folds, fold -> {
                svm_parameter param = LibSVMTests.linearSVC(C);
                svm_model model = LibSVMTests.train(fold, param);
                return model;
            });

            double mean = summary.getMean();
            double std = summary.getStandardDeviation();
            System.out.printf("linear  C=%8.3f, auc=%.4f ± %.4f%n", C, mean, std);
        }

        System.out.println("========================== 다항식 커널");
        Cs = new double[] { 0.001, 0.01, 0.1, 0.5, 1.0};
        for (double C : Cs) {
            DescriptiveStatistics summary = LibSVMTests.crossValidate(folds, fold -> {
                svm_parameter param = LibSVMTests.polynomialSVC(2, C);
                svm_model model = LibSVMTests.train(fold, param);
                return model;
            });

            double mean = summary.getMean();
            double std = summary.getStandardDeviation();
            System.out.printf("poly(2) C=%8.3f, auc=%.4f ± %.4f%n", C, mean, std);
        }

        Cs = new double[] { 0.001, 0.01, 0.1 };
        for (double C : Cs) {
            DescriptiveStatistics summary = LibSVMTests.crossValidate(folds, fold -> {
                svm_parameter param = LibSVMTests.polynomialSVC(3, C);
                return LibSVMTests.train(fold, param);
            });

            double mean = summary.getMean();
            double std = summary.getStandardDeviation();
            System.out.printf("poly(3) C=%8.3f, auc=%.4f ± %.4f%n", C, mean, std);
        }

        System.out.println("========================== 가우시안 커널");
        Cs = new double[] { 0.001, 0.01, 0.1};
        for (double C : Cs) {
            DescriptiveStatistics summary = LibSVMTests.crossValidate(folds, fold -> {
                svm_parameter param = LibSVMTests.gaussianSVC(C, 1.0);
                svm_model model = LibSVMTests.train(fold, param);
                return model;
            });

            double mean = summary.getMean();
            double std = summary.getStandardDeviation();
            System.out.printf("rbf     C=%8.3f, auc=%.4f ± %.4f%n", C, mean, std);
        }
	}

	/**
	 * 로그 출력 비활성화
	 */
    public static void mute() {
        svm.svm_set_print_string_function(s -> {});
    }

    /**
     * SVM 모델 훈련
     * 
     * @param dataset 대상 데이터 셋
     * @param param 매개변수
     * @return {@link svm_model}
     */
    public static svm_model train(Dataset dataset, svm_parameter param) {
        svm_problem prob = wrapDataset(dataset);
        return svm.svm_train(prob, param);
    }

    public static DescriptiveStatistics crossValidate(List<Split> folds, Function<Dataset, svm_model> trainer) {
        double[] aucs = folds.parallelStream().mapToDouble(fold -> {
            Dataset foldTrain = fold.getTrain();
            Dataset foldValidation = fold.getTest();
            svm_model model = trainer.apply(foldTrain);
            return auc(model, foldValidation);
        }).toArray();

        return new DescriptiveStatistics(aucs);
    }

    public static double auc(svm_model model, Dataset dataset) {
        double[] probs = predictProba(model, dataset);
        return Metrics.auc(dataset.getY(), probs);
    }

    /**
     * 훈련한 SVM 모델을 사용하여 데이터 분류, 확률값을 얻는다.
     * @param model 훈련한 SVM 모델
     * @param dataset 분류할 데이터 셋
     * @return double 배열, 확률값 배열
     */
    public static double[] predictProba(svm_model model, Dataset dataset) {
        int n = dataset.length();

        double[][] X = dataset.getX();
        double[] results = new double[n];
        double[] probs = new double[2];

        for (int i = 0; i < n; i++) {
            svm_node[] row = wrapAsSvmNode(X[i]);
            svm.svm_predict_probability(model, row, probs);
            results[i] = probs[1];
        }

        return results;
    }

    /**
     * 선형 커널 SVM 설정
     * @param C 정규화 매개변수
     * @return {@link svm_parameter}
     */
    public static svm_parameter linearSVC(double C) {
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.LINEAR;
        param.probability = 1;
        param.C = C;

        // 기본 매개변수
        param.cache_size = 100;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        return param;
    }

    /**
     * 다항식 커널 SVM 설정
     * @param degree 다항식의 차수
     * @param C 정규화 매개변수
     * @return {@link svm_parameter}
     */
	public static svm_parameter polynomialSVC(int degree, double C) {
		svm_parameter param = new svm_parameter();
		param.svm_type = svm_parameter.C_SVC;
		param.kernel_type = svm_parameter.POLY;
		param.C = C;
		param.degree = degree;
		param.gamma = 1;
		param.coef0 = 1;
		param.probability = 1;

		// 기본 매개변수
		param.cache_size = 100;
		param.eps = 1e-3;
		param.p = 0.1;
		param.shrinking = 1;
		return param;
	}

	/**
	 * RBF 커널 SVM 설정
	 * @param C 정규화 매개변수
	 * @param gamma RBF 폭 제어 변수
	 * @return {@link svm_parameter}
	 */
    public static svm_parameter gaussianSVC(double C, double gamma) {
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.C = C;
        param.gamma = gamma;
        param.probability = 1;

        // defaults
        param.cache_size = 100;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        return param;
    }

    /**
     * 데이터와 레이블을 {@link svm_problem} 객체로 생성
     * @param dataset 데이터 셋
     * @return {@link svm_problem}
     */
    public static svm_problem wrapDataset(Dataset dataset) {
        svm_problem prob = new svm_problem();
        prob.l = dataset.length();
        prob.x = wrapAsSvmNodes(dataset.getX());
        prob.y = dataset.getY();
        return prob;
    }

    /**
     * 일반적인 행렬 형태의 변환
     * 
     * @param X 행렬 데이터
     * @return {@link svm_node} 이중 배열
     */
    private static svm_node[][] wrapAsSvmNodes(double[][] X) {
        int n = X.length;
        svm_node[][] nodes = new svm_node[n][];

        for (int i = 0; i < n; i++) {
            nodes[i] = wrapAsSvmNode(X[i]);
        }

        return nodes;
    }

    /**
     * 단일 행 데이터의 희소형태 변환
     * 
     * @param dataRow 단일 행 데이터
     * @return {@link svm_node} 배열
     */
    private static svm_node[] wrapAsSvmNode(double[] dataRow) {
        svm_node[] svmRow = new svm_node[dataRow.length];

        for (int j = 0; j < dataRow.length; j++) {
            svm_node node = new svm_node();
            node.index = j;
            node.value = dataRow[j];
            svmRow[j] = node;
        }

        return svmRow;
    }
    
    // 기타 svm_parameter 생성
    
	public static double[] predict(svm_model model, Dataset dataset) {
		int n = dataset.length();

		double[][] X = dataset.getX();
		double[] results = new double[n];

		for (int i = 0; i < n; i++) {
			svm_node[] row = wrapAsSvmNode(X[i]);
			results[i] = svm.svm_predict(model, row);
		}

		return results;
	}
    
    public static svm_parameter epsilonSVR(double C, double eps) {
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.EPSILON_SVR;
        param.kernel_type = svm_parameter.LINEAR;
        param.C = C;
        param.eps = eps;

        // defaults
        param.cache_size = 100;
        param.p = 0.1;
        param.shrinking = 1;
        return param;
    }

    public static svm_parameter nuSVR(double C, double nu) {
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.NU_SVR;
        param.kernel_type = svm_parameter.LINEAR;
        param.C = C;
        param.nu = nu;

        // defaults
        param.cache_size = 100;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        return param;
    }
}
