package chapter04.classification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import chapter04.RankedPageData;
import chapter04.cv.Dataset;
import chapter04.cv.Split;
import chapter04.preprocess.StandardizationPreprocessor;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.tree.RandomForest;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;

public class JavaMLTests {
	
	public static void main(String[] args) throws IOException {
		Split split = RankedPageData.readRankedPagesMatrix();

        Dataset train = split.getTrain();
        Dataset test = split.getTest();

        StandardizationPreprocessor preprocessor = StandardizationPreprocessor.train(train);
        train = preprocessor.transform(train);
        test = preprocessor.transform(test);

        List<Split> folds = train.kfold(3);

        DescriptiveStatistics rf = JavaMLTests.crossValidate(folds, fold -> randomForest(fold));
        System.out.printf("random forest    auc=%.4f ± %.4f%n", rf.getMean(), rf.getStandardDeviation());
	}
	
	public static RandomForest randomForest(Dataset train) {
        net.sf.javaml.core.Dataset data = JavaMLTests.wrapDataset(train);

        RandomForest randomForest = new RandomForest(150);
        randomForest.setNumAttributes(3);
        randomForest.buildClassifier(data);

        return randomForest;
    }

    public static DescriptiveStatistics crossValidate(List<Split> folds, Function<Dataset, Classifier> trainer) {
        double[] aucs = folds.parallelStream().mapToDouble(fold -> {
            Dataset foldTrain = fold.getTrain();
            Dataset foldValidation = fold.getTest();
            Classifier model = trainer.apply(foldTrain);
            return auc(model, foldValidation);
        }).toArray();

        return new DescriptiveStatistics(aucs);
    }

    public static double auc(Classifier model, Dataset dataset) {
        double[] probability = predict(model, dataset);
        return Metrics.auc(dataset.getY(), probability);
    }

    public static double[] predict(Classifier model, Dataset dataset) {
        double[][] X = dataset.getX();
        double[] result = new double[X.length];

        for (int i = 0; i < X.length; i++) {
            DenseInstance point = new DenseInstance(X[i]);
            Map<Object, Double> distribution = model.classDistribution(point);
            result[i] = distribution.get(1);
        }

        return result;
    }

    public static net.sf.javaml.core.Dataset wrapDataset(Dataset train) {
        double[][] X = train.getX();
        int[] y = train.getYAsInt();

        List<Instance> rows = new ArrayList<>(X.length);
        for (int i = 0; i < X.length; i++) {
            rows.add(new DenseInstance(X[i], y[i]));
        }

        return new DefaultDataset(rows);
    }
}
