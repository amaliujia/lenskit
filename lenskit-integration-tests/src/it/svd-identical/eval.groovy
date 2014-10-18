/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2014 Regents of the University of Minnesota and contributors
 * Work on LensKit has been funded by the National Science Foundation under
 * grants IIS 05-34939, 08-08692, 08-12148, and 10-17697.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

import org.grouplens.lenskit.ItemScorer
import org.grouplens.lenskit.RatingPredictor
import org.grouplens.lenskit.baseline.BaselineScorer
import org.grouplens.lenskit.baseline.ItemMeanRatingItemScorer
import org.grouplens.lenskit.baseline.MeanDamping
import org.grouplens.lenskit.baseline.UserMeanBaseline
import org.grouplens.lenskit.baseline.UserMeanItemScorer
import org.grouplens.lenskit.eval.metrics.predict.*
import org.grouplens.lenskit.iterative.IterationCount
import org.grouplens.lenskit.knn.NeighborhoodSize
import org.grouplens.lenskit.knn.item.ItemItemScorer
import org.grouplens.lenskit.knn.item.ModelSize
import org.grouplens.lenskit.knn.item.model.*
import org.grouplens.lenskit.knn.user.NeighborFinder
import org.grouplens.lenskit.knn.user.SnapshotNeighborFinder
import org.grouplens.lenskit.knn.user.UserUserItemScorer
import org.grouplens.lenskit.mf.funksvd.FeatureCount
import org.grouplens.lenskit.mf.funksvd.FunkSVDItemScorer
import org.grouplens.lenskit.mf.funksvd.FunkSVDModelBuilder
import org.grouplens.lenskit.mf.funksvd.FunkSVDUpdateRule
import org.grouplens.lenskit.mf.svd.BiasedMFItemScorer
import org.grouplens.lenskit.mf.svd.BiasedMFKernel
import org.grouplens.lenskit.mf.svd.DotProductKernel
import org.grouplens.lenskit.mf.svd.MFModel
import org.grouplens.lenskit.transform.normalize.MeanCenteringVectorNormalizer
import org.grouplens.lenskit.transform.normalize.UserVectorNormalizer
import org.grouplens.lenskit.transform.normalize.VectorNormalizer
import org.grouplens.lenskit.transform.truncate.VectorTruncator

def dataDir = config['lenskit.movielens.100k']

trainTest {
    dataset crossfold("ML100K") {
        source csvfile("$dataDir/u.data") {
            delimiter "\t"
        }
        partitions 5
        holdout 5
        train 'train.%d.csv'
        test 'test.%d.csv'
    }

    def common = {
        set FeatureCount to 40
        set IterationCount to 200
        bind (BaselineScorer, ItemScorer) to UserMeanItemScorer
        bind (UserMeanBaseline, ItemScorer) to ItemMeanRatingItemScorer
        set MeanDamping to 5
    }

    algorithm("FunkSVD") {
        include common
        bind ItemScorer to FunkSVDItemScorer
        at (ItemScorer) {
            bind FunkSVDUpdateRule to null
        }
    }
    algorithm("BiasedMF") {
        include common
        bind ItemScorer to BiasedMFItemScorer
        bind MFModel toProvider FunkSVDModelBuilder
    }

    metric CoveragePredictMetric
    metric RMSEPredictMetric
    metric MAEPredictMetric
    metric NDCGPredictMetric
    metric HLUtilityPredictMetric

    output 'results.csv'
    predictOutput 'predictions.csv'
}
