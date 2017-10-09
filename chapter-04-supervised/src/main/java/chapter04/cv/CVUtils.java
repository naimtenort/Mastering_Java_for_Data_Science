package chapter04.cv;

public class CVUtils {

	/**
	 * 평균 제곱 오차를 구한다.
	 * 
	 * @param actual 실제 값
	 * @param predicted 예측 값
	 * @return 평균 제곱 오차
	 */
	public static double mse(double[] actual, double[] predicted) {
		int n = actual.length;
		double sum = 0.0;
		
		for (int i = 0; i < n; i++) {
		    double diff = actual[i] - predicted[i];
		    sum = sum + diff * diff;
		}
		
		double mse = sum / n;

		return mse;
	}
	
	/**
	 * 평균 제곱근 편차를 구한다.
	 * 
	 * @param actual 실제 값
	 * @param predicted 예측 값
	 * @return 평균 제곱근 편차
	 */
	public static double rmse(double[] actual, double[] predicted) {
		double mse = mse(actual, predicted);
		double rmse = Math.sqrt(mse);
		return rmse;
	}
	
	/**
	 * 평균 절대 오차를 구한다.
	 * 
	 * @param actual 실제 값
	 * @param predicted 예측 값
	 * @return 평균 절대 오차
	 */
	public static double mae(double[] actual, double[] predicted) {
		double sum = 0.0;
		int n = actual.length;
		for (int i = 0; i < n; i++) {
		    sum = sum + Math.abs(actual[i] - predicted[i]);
		}
		double mae = sum / n;

		return mae;
	}
}
