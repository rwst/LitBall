#!/usr/bin/env python

import json, os, spacy

os.environ['OMP_NUM_THREADS'] = '3'
#parser_path = "/home/ralf/models/en_core_sci_lg-0.5.1/en_core_sci_lg/en_core_sci_lg-0.5.1"
parser_path = "en_core_sci_sm"
parser_path = "en_core_web_trf"
dataset_path = "/home/ralf/IdeaProjects/LitBall-training/EXP-Title+TLDR/"
text_key = "originalTitle"

# Load the spaCy language model:
nlp = spacy.load(parser_path)

import tensorflow_decision_forests as tfdf

import os
import numpy as np
import pandas as pd
import tensorflow as tf
import math
import json

dataset_path = "/home/ralf/IdeaProjects/LitBall-training/EXP-Title+TLDR/"
text_key = "preprocessedText"
docs = []
larr1 = []
with open(dataset_path + "ROTVRSV") as file:
    lines = file.readlines()
    for line in lines:
        d = json.loads(line)
        docs.append(d[text_key])
        larr1.append(d["label"])

tdocs = []
larr2 = []
with open(dataset_path + "DENV") as file:
    lines = file.readlines()
    for line in lines:
        d = json.loads(line)
        tdocs.append(d[text_key])
        larr2.append(d["label"])

        # Tokenize the documents.

def tok(s):
    return '_'.join(s.split(' '))

# Get entities from documents.
for idx in range(len(docs)):
    docs[idx] = docs[idx].lower()  # Convert to lowercase.
    docs[idx] = ' '.join([tok(t.text) for t in nlp(docs[idx]).ents])
for idx in range(len(tdocs)):
    tdocs[idx] = tdocs[idx].lower()  # Convert to lowercase.
    tdocs[idx] = ' '.join([tok(t.text) for t in nlp(tdocs[idx]).ents])

print(len(docs))
print(docs[0][:500])
print(len(tdocs))
print(tdocs[0][:500])

# Load the dataset
import tensorflow_datasets as tfds
import tensorflow as tf
import pandas as pd

in_memory_file1 = io.StringIO()
for i in range(len(docs)):
    in_memory_file1.write(json.dumps({ text_key: docs[i], "label": larr1[i] }) + '\n')
in_memory_file1.seek(0)
in_memory_file2 = io.StringIO()
for i in range(len(tdocs)):
    in_memory_file2.write(json.dumps({ text_key: tdocs[i], "label": larr2[i] }) + '\n')
in_memory_file2.seek(0)

trds = pd.read_json(in_memory_file1,
                 dtype={text_key: str, "label": str},
                lines=True
                 )
teds = pd.read_json(in_memory_file2,
                 dtype={text_key: str, "label": str},
                lines=True
                 )
tr_ds = tfdf.keras.pd_dataframe_to_tf_dataset(trds, label="label")
te_ds = tfdf.keras.pd_dataframe_to_tf_dataset(teds, label="label")

dataset_path = "/home/ralf/IdeaProjects/LitBall-training/EXP-Title+TLDR/"
with open(dataset_path + "DENV") as file:
    test_lines = file.readlines()
    
print("Made datasets")

def prepare_dataset1(example, label):
    return {"sentence" : tf.strings.split(example[text_key])}, label

def prepare_dataset2(s):
    m = json.loads(s)
    sp = tf.strings.split(m[text_key])
    return m

train_ds = tr_ds.map(prepare_dataset1)
test_ds = te_ds.map(prepare_dataset1)
    
test_cases = list(map(prepare_dataset2, test_lines))

# Specify the model.
model_1 = tfdf.keras.RandomForestModel(num_trees=300, verbose=2, num_threads=3)

# Train the model.
model_1.fit(x=train_ds, batch_size=None)

model_1.compile(metrics=["accuracy"])
evaluation = model_1.evaluate(test_ds, batch_size=None)

print(f"BinaryCrossentropyloss: {evaluation[0]}")
print(f"Accuracy: {evaluation[1]}")

#import matplotlib.pyplot as plt
#
#logs = model_1.make_inspector().training_logs()
#plt.plot([log.num_trees for log in logs], [log.evaluation.accuracy for log in logs])
#plt.xlabel("Number of trees")
#plt.ylabel("Out-of-bag accuracy")

p = model_1.predict(test_ds)

#Fish for best cut
for cut in np.arange(0.25, .8, .05):
    tp = 0
    tn = 0
    fp = 0
    fn = 0
    N = len(p)
    for i in range(N):
        tru = test_cases[i]["label"] == '1'
        prd = p[i] > cut
        if tru and prd == tru:
            tp = tp + 1
        if tru and prd != tru:
            fn = fn + 1
        if tru == False and prd == tru:
            tn = tn + 1
        if tru == False and prd != tru:
            fp = fp + 1
    precision = tp/(tp+fp)
    recall = tp/(tp+fn)
    f1_score = 2 * (precision * recall) / (precision + recall)
    print("cut: {}, acc: {}, prec: {}, rec: {}, f1: {}".format(cut, (tp+tn)/N, precision, recall, f1_score))

cut = 55
N = len(p)
with open("/home/ralf//IdeaProjects/LitBall-training/pred.json", "w") as file:
    for c in np.arange(0., .5, .05):
        s = 0
        t = 0
        for i in range(N):
            tru = test_cases[i]["label"] == '1'
            val = int((p[i]+0.005) * 100)
            pred = False
            if val > cut:
                pred = True
            elif val > 100*c:
                s = s+1
                if tru:
                    t = t+1
        print("c: {}, n = {}, tru: {}".format(c, s, t))
