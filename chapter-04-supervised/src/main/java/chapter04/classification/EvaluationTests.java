package chapter04.classification;

import java.io.IOException;

import chapter04.RankedPageData;
import chapter04.cv.Dataset;
import chapter04.cv.Split;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.SolverType;

public class EvaluationTests {

	public static void main(String[] args) throws IOException {
		Split split = RankedPageData.readRankedPagesMatrix();

		Dataset train = split.getTrain();
		Dataset test = split.getTest();
		
		SolverType solverType = SolverType.L1R_LR;
		double C = 0.001;
		double eps = 0.0001;
		Parameter param = new Parameter(solverType, C, eps);
		
		double[] actual = train.getY();
		double[] proba = LibLinearTests.predictProba(LibLinearTests.train(train, param), train);
		double threshold = 0.5;
		
		// 1. 정확도 계산
		System.out.println("========================== 정확도 계산");
		System.out.println("Accuracy = " + Metrics.accuracy(actual, proba, threshold));
		
		// 2. 정밀도, 재현율, F1 계산
		System.out.println("========================== 정밀도, 재현율, F1 계산");
		System.out.println("Precision = " + Metrics.precision(actual, proba, threshold));
		System.out.println("Recall = " + Metrics.recall(actual, proba, threshold));
		System.out.println("F1 = " + Metrics.f1(actual, proba, threshold));
		
		// 3. ROC 곡선을 그린다.
		System.out.println("========================== ROC 곡선");
		RocCurve.plot(actual, proba);
		
		// 4. AUC를 구한다.
		System.out.println("========================== AUC를 구한다");
		System.out.println("AUC = " + Metrics.auc(actual, proba));
	}
	
	
}
