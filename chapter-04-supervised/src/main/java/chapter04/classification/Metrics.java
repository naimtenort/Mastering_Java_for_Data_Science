package chapter04.classification;

import java.util.Arrays;

import org.apache.commons.lang3.Validate;

import smile.validation.AUC;

public class Metrics {

	/**
	 * AUC를 구한다. 
	 * 
	 * @param actual 실제 값
	 * @param predicted 예측 값
	 * @return AUC
	 */
    public static double auc(double[] actual, double[] predicted) {
        Validate.isTrue(actual.length == predicted.length, "the lengths don't match");

        int[] truth = Arrays.stream(actual).mapToInt(i -> (int) i).toArray();
        double auc = AUC.measure(truth, predicted);
        if (auc > 0.5) {
            return auc;
        } else {
            return 1 - auc;
        }
    }

    public static double logLoss(double[] actual, double[] predicted) {
        return logLoss(actual, predicted, 1e-15);
    }

    public static double logLoss(double[] actual, double[] predicted, double eps) {
        Validate.isTrue(actual.length == predicted.length, "the lengths don't match");
        int n = actual.length;
        double total = 0.0;

        for (int i = 0; i < n; i++) {
            double yi = actual[i];
            double pi = predicted[i];

            if (yi == 0.0) {
                total = total + Math.log(Math.min(1 - pi, 1 - eps));
            } else if (yi == 1.0) {
                total = total + Math.log(Math.max(pi, eps));
            } else {
                throw new IllegalArgumentException("unrecognized class " + yi);
            }
        }

        return -total / n;
    }

//    /**
//     * 정확도를 계산한다.
//     * 
//     * @param actual 실제 값
//     * @param proba 예측 값
//     * @param threshold 임계치
//     * @return 정확도
//     */
//    public static double accuracy(double[] actual, double[] proba, double threshold) {
//        ConfusionMatrix matrix = confusion(actual, proba, threshold);
//        return matrix.accuracy();
//    }
    
    /**
	 * 정확도를 측정한다.
	 * @param actual 실제 값
	 * @param proba 예측 값
	 * @param threshold 임계치
	 * @return 정확도
	 */
	public static double accuracy(double[] actual, double[] proba, double threshold) {
		int n = actual.length;
		double[] prediction = Arrays.stream(proba).map(p -> p > threshold ? 1.0 : 0.0).toArray();
		int correct = 0;
		for (int i = 0; i < n; i++) {
		    if (actual[i] == prediction[i]) {
		        correct++;
		    }
		}
		double accuracy = 1.0 * correct / n;

		return accuracy;
	}

    /**
     * 정밀도를 계산한다.
     * 
     * @param actual 실제 값
     * @param proba 예측 값
     * @param threshold 임계치
     * @return 정밀도
     */
    public static double precision(double[] actual, double[] proba, double threshold) {
        ConfusionMatrix matrix = confusion(actual, proba, threshold);
        return matrix.precision();
    }

    /**
     * 재현율를 계산한다.
     * 
     * @param actual 실제 값
     * @param proba 예측 값
     * @param threshold 임계치
     * @return 재현율
     */
    public static double recall(double[] actual, double[] proba, double threshold) {
        ConfusionMatrix matrix = confusion(actual, proba, threshold);
        return matrix.recall();
    }

    /**
     * F1을 계산한다.
     * 
     * @param actual 실제 값
     * @param proba 예측 값
     * @param threshold 임계치
     * @return F1
     */
    public static double f1(double[] actual, double[] proba, double threshold) {
        ConfusionMatrix matrix = confusion(actual, proba, threshold);
        return matrix.f1();
    }

    /**
     * {@link ConfusionMatrix}를 만든다.
     * @param actual 실제 값
     * @param proba 예측 값
     * @param threshold 임계치
     * @return {@link ConfusionMatrix}
     */
    public static ConfusionMatrix confusion(double[] actual, double[] proba, double threshold) {
        Validate.isTrue(actual.length == proba.length, "the lengths don't match");

        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;

        for (int i = 0; i < actual.length; i++) {
            if (actual[i] == 1.0 && proba[i] > threshold) {
                tp++;
            } else if (actual[i] == 0.0 && proba[i] <= threshold) {
                tn++;
            } else if (actual[i] == 0.0 && proba[i] > threshold) {
                fp++;
            } else if (actual[i] == 1.0 && proba[i] <= threshold) {
                fn++;
            } else {
                throw new IllegalArgumentException("unexpected label " + actual[i] + " at index " + i);
            }
        }

        return new ConfusionMatrix(tp, tn, fp, fn);
    }

    public static class ConfusionMatrix {
        private final int tp;
        private final int tn;
        private final int fp;
        private final int fn;

        public ConfusionMatrix(int tp, int tn, int fp, int fn) {
            this.tp = tp;
            this.tn = tn;
            this.fp = fp;
            this.fn = fn;
        }

        public int getTP() {
            return tp;
        }

        public int getTN() {
            return tn;
        }

        public int getFP() {
            return fp;
        }

        public int getFN() {
            return fn;
        }

        public double accuracy() {
            int n = tp + tn + fp + fn;
            return 1.0 * (tp + fp) / n;
        }

        public double precision() {
            return 1.0 * tp / (tp + fp);
        }

        public double recall() {
            return 1.0 * tp / (tp + fn);
        }

        public double f1() {
            double precision = precision();
            double recall = recall();
            return 2 * precision * recall / (precision + recall);
        }

    }
}
