package chapter04.classification;

import java.io.IOException;

import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.ml.train.MLTrain;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.neural.networks.training.strategy.RegularizationStrategy;

import chapter04.RankedPageData;
import chapter04.cv.Dataset;
import chapter04.cv.Split;
import chapter04.preprocess.StandardizationPreprocessor;

public class EncogTests {
	
	public static void main(String[] args) throws IOException {
		Split split = RankedPageData.readRankedPagesMatrix();

		Dataset train = split.getTrain();
		Dataset test = split.getTest();
		
		int noInputNeurons = train.getX()[0].length;
		
		BasicNetwork network = new BasicNetwork();
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, noInputNeurons));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 30));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 1));
		network.getStructure().finalizeStructure();
		network.reset();
		
		MLTrain trainer = new ResilientPropagation(network, asEncogDataset(train));
		double lambda = 0.01;
		trainer.addStrategy(new RegularizationStrategy(lambda));
		int noEpochs = 101;
		for (int i = 0; i < noEpochs; i++) {
			System.out.println("iteration : " + i  + " started...");
		    trainer.iteration();
		}
		
		// Encog를 사용하여 다양한 테스트를 수행한다.
		useEncog();
		
		shutdown();
	}
	
	/**
	 * Encog를 사용하여 다양한 테스트를 수행한다.
	 * 
	 * @throws IOException
	 */
	public static void useEncog() throws IOException {
		Split split = RankedPageData.readRankedPagesMatrix();

        Dataset fullTrain = split.getTrain();
        Dataset test = split.getTest();

        StandardizationPreprocessor preprocessor = StandardizationPreprocessor.train(fullTrain);
        fullTrain = preprocessor.transform(fullTrain);
        test = preprocessor.transform(test);

        Split validationSplit = fullTrain.trainTestSplit(0.3);
        Dataset train = validationSplit.getTrain();

        int noInputNeurons = fullTrain.getX()[0].length;

        BasicNetwork network = new BasicNetwork();
        network.addLayer(new BasicLayer(new ActivationSigmoid(), true, noInputNeurons));
        network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 30));
        network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 1));
        network.getStructure().finalizeStructure();
        network.reset(1);

        MLDataSet trainSet = EncogTests.asEncogDataset(train);

        System.out.println("training  model with many iterations");
        MLTrain trainer = new ResilientPropagation(network, trainSet);
        double lambda = 0.01;
        trainer.addStrategy(new RegularizationStrategy(lambda));

        int noEpochs = 101;
        EncogTests.learningCurves(validationSplit.getTrain(), validationSplit.getTest(), network, trainer, noEpochs);

        System.out.println();
        System.out.println("retraining full model with 20 iterations");

        network.reset(1);

        MLDataSet fullTrainSet = EncogTests.asEncogDataset(fullTrain);
        trainer = new ResilientPropagation(network, fullTrainSet);
        trainer.addStrategy(new RegularizationStrategy(lambda));

        EncogTests.learningCurves(fullTrain, test, network, trainer, 21);

        EncogTests.shutdown();
	}

    public static void learningCurves(Dataset train, Dataset test, BasicNetwork network, MLTrain trainer, int noEpochs) {
        for (int i = 0; i < noEpochs; i++) {
            trainer.iteration();
            if (i % 10 == 0) {
                double aucTrain = auc(network, train);
                double aucVal = auc(network, test);

                System.out.printf("%3d - train:%.4f, val:%.4f%n", i, aucTrain, aucVal);
            }
        }
    }

    public static double auc(BasicNetwork network, Dataset dataset) {
        double[] predictTrain = predict(network, dataset);
        return Metrics.auc(dataset.getY(), predictTrain);
    }

    public static double[] predict(BasicNetwork model, Dataset dataset) {
        double[][] X = dataset.getX();
        double[] result = new double[X.length];

        for (int i = 0; i < X.length; i++) {
            MLData out = model.compute(new BasicMLData(X[i]));
            result[i] = out.getData(0);
        }

        return result;

    }

    /**
     * 데이터 변환
     * 
     * @param train 훈련 데이터
     * @return
     */
    public static BasicMLDataSet asEncogDataset(Dataset train) {
        return new BasicMLDataSet(train.getX(), to2d(train.getY()));
    }

    /**
     * 일차원 배열 데이터를 2차원 배열 데이터로 변환
     * @param y 일차원 데이터
     * @return 이차원 데이터
     */
    private static double[][] to2d(double[] y) {
        double[][] res = new double[y.length][];

        for (int i = 0; i < y.length; i++) {
            res[i] = new double[] { y[i] };
        }

        return res;
    }

    public static void shutdown() {
        org.encog.Encog.getInstance().shutdown();
    }
}
