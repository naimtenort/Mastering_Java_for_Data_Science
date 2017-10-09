package chapter04.cv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.apache.commons.lang3.Validate;

public class CV {

	/**
	 * K-fold 교차 검증을 위한 데이터를 준비한다. Split 객체들로 만든다.
	 * 
	 * @param dataset 원본 데이터
	 * @param k 나눌 폴더 수
	 * @param shuffle 색인 재구성 여부
	 * @param seed 재현성을 위한 시드
	 * @return {@link Split} 데이터들
	 */
    public static List<Split> kfold(Dataset dataset, int k, boolean shuffle, long seed) {
        int length = dataset.length();
        Validate.isTrue(k < length);

        int[] indexes = IntStream.range(0, length).toArray();
        if (shuffle) {
            shuffle(indexes, seed);
        }

        int[][] folds = prepareFolds(indexes, k);
        List<Split> result = new ArrayList<>();

        for (int i = 0; i < k; i++) {
            int[] testIdx = folds[i];
            int[] trainIdx = combineTrainFolds(folds, indexes.length, i);
            result.add(Split.fromIndexes(dataset, trainIdx, testIdx));
        }

        return result;
    }

    /**
     * 데이터셋을 testRatio에 따라 훈련 데이터와 테스트 데이터로 분리한다.
     * 
     * @param dataset 원본 데이터 셋
     * @param testRatio 테스트 데이터 비율
     * @param shuffle 색인 재구성 여부
     * @param seed 재현성을 위한 시드
     * @return 분리된 데이터 셋
     */
    public static Split trainTestSplit(Dataset dataset, double testRatio, boolean shuffle, long seed) {
        Validate.isTrue(testRatio > 0.0 && testRatio < 1.0, "testRatio must be in (0, 1) interval");

        int[] indexes = IntStream.range(0, dataset.length()).toArray();
        if (shuffle) {
            shuffle(indexes, seed);
        }

        int trainSize = (int) (indexes.length * (1 - testRatio));

        int[] trainIndex = Arrays.copyOfRange(indexes, 0, trainSize);
        int[] testIndex = Arrays.copyOfRange(indexes, trainSize, indexes.length);

        return Split.fromIndexes(dataset, trainIndex, testIndex);
    }

    /**
     * 색인을 재구성한다.
     * @param indexes 색인
     * @param seed 재현성을 위한 시드
     */
    public static void shuffle(int[] indexes, long seed) {
        Random rnd = new Random(seed);

        for (int i = indexes.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);

            int tmp = indexes[index];
            indexes[index] = indexes[i];
            indexes[i] = tmp;
        }
    }

    /**
     * K-fold 교차 검증을 위한 데이터를 준비한다. 
     * @param indexes 색인 정보
     * @param k 분리한 데이터 개수
     * @return 각 폴더 별 인덱스 정보
     */
    private static int[][] prepareFolds(int[] indexes, int k) {
        int[][] foldIndexes = new int[k][];

        int step = indexes.length / k;
        int beginIndex = 0;

        for (int i = 0; i < k - 1; i++) {
            foldIndexes[i] = Arrays.copyOfRange(indexes, beginIndex, beginIndex + step);
            beginIndex = beginIndex + step;
        }

        foldIndexes[k - 1] = Arrays.copyOfRange(indexes, beginIndex, indexes.length);
        return foldIndexes;
    }

    /**
     * K-1 부분에 해당하는 배열 데이터들을 하나로 결합
     * 
     * @param folds 분리된 색인 정보
     * @param totalSize 전체 크기
     * @param excludeIndex 제외할 색인 정보
     * @return 분리 데이터 색인
     */
    private static int[] combineTrainFolds(int[][] folds, int totalSize, int excludeIndex) {
        int size = totalSize - folds[excludeIndex].length;
        int result[] = new int[size];

        int start = 0;
        for (int i = 0; i < folds.length; i++) {
            if (i == excludeIndex) {
                continue;
            }
            int[] fold = folds[i];
            System.arraycopy(fold, 0, result, start, fold.length);
            start = start + fold.length;
        }

        return result;
    }
}
