/*
 * Copyright (c) 2013 LDBC
 * Linked Data Benchmark Council (http://ldbc.eu)
 *
 * This file is part of ldbc_socialnet_dbgen.
 *
 * ldbc_socialnet_dbgen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ldbc_socialnet_dbgen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ldbc_socialnet_dbgen.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2011 OpenLink Software <bdsmt@openlinksw.com>
 * All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation;  only Version 2 of the License dated
 * June 1991.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package ldbc.snb.datagen.dictionary;

import ldbc.snb.datagen.generator.DatagenParams;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

public class TagMatrix {

    private static final String SEPARATOR = " ";

    private TreeMap<Integer, ArrayList<Integer>> relatedTags;
    /**
     * < @brief An array of related tags per tag.
     */
    private TreeMap<Integer, ArrayList<Double>> cumulative;

    private ArrayList<Integer> nonZeroTags;

    private int cacheSize = 20000;
    private int expandSize = 10000;
    private ArrayList<Integer> [] unrolledCumulativeCache = null;
    private int [] unrolledCumulativeCacheTag = null;

    /**
     * < @brief The list of tags.
     */

    public TagMatrix() {
        cumulative = new TreeMap<Integer, ArrayList<Double>>();
        relatedTags = new TreeMap<Integer, ArrayList<Integer>>();
        nonZeroTags = new ArrayList<Integer>();
        unrolledCumulativeCache = new ArrayList[cacheSize];
        unrolledCumulativeCacheTag = new int[cacheSize];
        for(int i = 0; i < cacheSize; ++i) {
            unrolledCumulativeCache[i] = new ArrayList<Integer>();
            unrolledCumulativeCacheTag[i] = -1;
        }
        load(DatagenParams.tagMatrixFile);

    }

    /**
     * @param tagMatrixFileName The tag matrix file name.
     * @brief Loads the tag matrix from a file.
     */
    private void load(String tagMatrixFileName) {
        try {
            BufferedReader dictionary = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(tagMatrixFileName), "UTF-8"));
            String line;
            while ((line = dictionary.readLine()) != null) {
                String data[] = line.split(SEPARATOR);
                int celebrityId = Integer.parseInt(data[0]);
                int topicId = Integer.parseInt(data[1]);
                double cumuluative = Double.parseDouble(data[2]);
                ArrayList<Double> cum = cumulative.get(celebrityId);
                if(cum == null ) cumulative.put(celebrityId,new ArrayList<Double>());
                cumulative.get(celebrityId).add(cumuluative);
                ArrayList<Integer> related = relatedTags.get(celebrityId);
                if(related == null) relatedTags.put(celebrityId,new ArrayList<Integer>());
                relatedTags.get(celebrityId).add(topicId);
            }
            for(Integer tag : relatedTags.keySet()) {
                nonZeroTags.add(tag);
                fillCache(tag);
            }
            dictionary.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fillCache(int tagId) {
        int cacheIndex = tagId % cacheSize;
        unrolledCumulativeCacheTag[cacheIndex] = tagId;
        int tagIndex = 0;
        ArrayList<Integer> related = relatedTags.get(tagId);
        for(Double prob : cumulative.get(tagId) ) {
            double previousProb = 0;
            if(tagIndex > 0) {
                previousProb = cumulative.get(tagId).get(tagIndex-1);
            }
            int numElements = (int)((prob-previousProb)*expandSize)+1;
            int relatedTag = related.get(tagIndex);
            for(int i = 0; i < numElements; ++i) {
                unrolledCumulativeCache[cacheIndex].add(relatedTag);
            }
            ++tagIndex;
        }
    }

    /**
     * @param randomTag The random tag number generator.
     * @param tag       The tag identifier.
     * @return The related tag identifier.
     * @brief Gets a random related tag.
     */
    public Integer getRandomRelated(Random randomTag, int tag) {
        int tagId = tag;
        if(relatedTags.get(tagId) == null) {
            tagId = nonZeroTags.get(randomTag.nextInt(nonZeroTags.size()));
        }
        return relatedTags.get(tagId).get(randomTag.nextInt(relatedTags.get(tagId).size()));
    }

    /**
     * @param randomTopic  The random number generator used to select aditional popular tags
     * @param randomTag    The random number generator used to select related tags.
     * @param popularTagId The popular tag identifier.
     * @param numTags      The number of related tags to retrieve.
     * @return The set of related tags.
     * @brief Get a set of related tags.
     */
    public TreeSet<Integer> getSetofTags(Random randomTopic, Random randomTag, int popularTagId, int numTags) {
        TreeSet<Integer> resultTags = new TreeSet<Integer>();
        resultTags.add(popularTagId);
        while (resultTags.size() < numTags) {
            int tagId;
            tagId = popularTagId;

            while (relatedTags.get(tagId).size() == 0) {
                tagId = randomTopic.nextInt(relatedTags.size());
            }

            // Doing binary search for finding the tag
            double randomDis = randomTag.nextDouble();
            int lowerBound = 0;
            int upperBound = relatedTags.get(tagId).size();
            int midPoint = (upperBound + lowerBound) / 2;

            while (upperBound > (lowerBound + 1)) {
                if (cumulative.get(tagId).get(midPoint) > randomDis) {
                    upperBound = midPoint;
                } else {
                    lowerBound = midPoint;
                }
                midPoint = (upperBound + lowerBound) / 2;
            }
            resultTags.add(relatedTags.get(tagId).get(midPoint));
        }
        return resultTags;

    }

    public TreeSet<Integer> getSetofTagsCached(Random randomTopic, Random randomTag, int popularTagId, int numTags) {
        TreeSet<Integer> resultTags = new TreeSet<Integer>();
        resultTags.add(popularTagId);
        while (resultTags.size() < numTags) {
            int tagId;
            tagId = popularTagId;

            while (relatedTags.get(tagId) == null) {
                tagId = nonZeroTags.get(randomTopic.nextInt(nonZeroTags.size()));
            }
            int cacheIndex = tagId%cacheSize;
            if(unrolledCumulativeCacheTag[cacheIndex] != tagId) {
                fillCache(tagId);
            }
            int randomIndex = randomTag.nextInt(unrolledCumulativeCache[cacheIndex].size());
            resultTags.add(unrolledCumulativeCache[cacheIndex].get(randomIndex));
        }
        return resultTags;

        /*TreeSet<Integer> resultTags = new TreeSet<Integer>();
        resultTags.add(popularTagId);
        ArrayList<Integer> related = relatedTags.get(popularTagId);
        if( related.size() <= (numTags-1)) {
            resultTags.addAll(relatedTags.get(popularTagId));
        } else if(related.size() > 0) {
            int [] indices = new int[related.size()];
            for(int i = 0; i < related.size();++i) {
                indices[i] = i;
            }
        }

        while (resultTags.size() < numTags) {
            int tagId;
            tagId = nonZeroTags.get(randomTopic.nextInt(nonZeroTags.size()));

            // Doing binary search for finding the tag
            double randomDis = randomTag.nextDouble();
            int lowerBound = 0;
            int upperBound = relatedTags.get(tagId).size();
            int midPoint = (upperBound + lowerBound) / 2;

            while (upperBound > (lowerBound + 1)) {
                if (cumulative.get(tagId).get(midPoint) > randomDis) {
                    upperBound = midPoint;
                } else {
                    lowerBound = midPoint;
                }
                midPoint = (upperBound + lowerBound) / 2;
            }
            resultTags.add(relatedTags.get(tagId).get(midPoint));
        }
        return resultTags;
        */
    }
}
