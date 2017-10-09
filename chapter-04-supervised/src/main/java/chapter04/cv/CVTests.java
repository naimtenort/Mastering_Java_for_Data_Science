package chapter04.cv;

import java.io.IOException;
import java.util.List;

import chapter04.RankedPageData;

public class CVTests {

	public static void main(String[] args) throws IOException {
		// 원본 데이터를 사용하여 테스트 데이터와 훈련 데이터로 분리한다.
		Dataset dataset = RankedPageData.readRankedPagesMatrixNoSplit();
		
		// RankedPageData.readRankedPagesMatrix() 참조
		Split split = dataset.trainTestSplit(0.2);
		Dataset train = split.getTrain();
		Dataset test = split.getTest();
		
		// K-fold 교차 검증을 위한 데이터 생성
		List<Split> folds = train.kfold(3);
		
	}

}
