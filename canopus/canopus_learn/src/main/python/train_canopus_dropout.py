import numpy as np 
from numpy.random import shuffle
import tensorflow as tf
import time
import sys
import math
from threading import Thread
from queue import Queue
import os
import os.path
from pathlib import PurePath
import random
import glob
import sys
from collections import Counter

PREFIX = ""
import os

LEARN_RATE = 0.00025

fingerprints = dict()
formulas = dict()
formula_feature_vectors = dict()

fingerprint_indizes = dict()
with open("fingerprint_indizes.txt") as fhandle:
  k = 0
  for line in fhandle:
    fingerprint_indizes[int(line)] = k
    k += 1


NUM_FEATURES = len(fingerprint_indizes)
print("Number of molecular property Features: %d" % NUM_FEATURES)

relativeIndizesToAbsolute = dict()
for (key,value) in fingerprint_indizes.items():
  relativeIndizesToAbsolute[value] = key

NUM_FORMULA_FEATURES = 0

with open("formula_features.csv") as fhandle:
  for line in fhandle:
    tabs = line.split("\t")
    f = tabs.pop(0)
    formulas[f] = np.array([float(val) for val in tabs], dtype=np.float32)
    NUM_FORMULA_FEATURES = len(tabs)

formulaScaleAndCenter = np.loadtxt("formula_normalized.txt")
CENTER_FORMULA = formulaScaleAndCenter[0,:]
SCALE_FORMULA = formulaScaleAndCenter[1,:]

allClasses = Counter()
with open("compounds.csv") as fhandle:
  for line in fhandle:
    tbs=line.rstrip().split("\t")
    for cat in tbs[1:]:
      allClasses[cat.replace("'", "&#39;")] += 1

klasses2id = dict()
_id = -1
for name in sorted(allClasses.keys()):
  anz = allClasses[name]
  if anz >= 500 and anz < 1000000:
    _id += 1
    klasses2id[name] = _id

if not os.path.exists(PREFIX + "klasses_with_indizes.csv"):
  with open(PREFIX + "klasses_with_indizes.csv", "w") as fhandle:
    for klassname in sorted(klasses2id.keys()):
      klassid = klasses2id[klassname]
      fhandle.write(klassname)
      fhandle.write("\t")
      fhandle.write(str(klassid))
      fhandle.write("\n")
else:
  klasses2id = dict()
  with open(PREFIX + "klasses_with_indizes.csv") as fhandle:
    for line in fhandle:
      cols = line.rstrip().split("\t")
      klasses2id[cols[0]] = int(cols[1])

INCLUDE_FP = os.path.exists("trainable_indizes.csv")
trainable_indizes = 0
if INCLUDE_FP:
  with open("trainable_indizes.csv") as fhandle:
    k = 0
    for line in fhandle:
      trainable_indizes += 1
  trainable_indizes -= 1 # remove header

NUM_LABELS = len(klasses2id) + trainable_indizes

print("Number of Labels: %d" % NUM_LABELS)

NUMBER_OF_FINGERPRINTS = len(fingerprint_indizes)
id2klass = []
for i in range(len(klasses2id)):
  id2klass.append("")  
for name in klasses2id:
  id2klass[klasses2id[name]] = name

#################
# java sampling##
#################
import gzip

JAVA_VAR_NAMES = [
"fully_connected/weights",
"fully_connected/biases",
"fully_connected_1/weights",
"fully_connected_1/biases",
"fully_connected_2/weights",
"fully_connected_2/biases",
"fully_connected_3/weights",
"fully_connected_3/biases",
"fully_connected_4/weights",
"fully_connected_4/biases",
"fully_connected_5/weights",
"fully_connected_5/biases"
]

with tf.Session() as sess:

  # config
  activation = tf.nn.relu
  loss_function = tf.losses.sigmoid_cross_entropy
  l1inner = 1e-6
  l2inner = 1e-4


  C=None#tf.keras.constraints.MaxNorm(5)

  # queue
  platts_x         = tf.placeholder(tf.float32, shape=[None, NUM_FEATURES], name="input_platts")
  formulas_x         = tf.placeholder(tf.float32, shape=[None, NUM_FORMULA_FEATURES], name="input_formulas")
  target_y  = tf.placeholder(tf.float32, shape=[None, NUM_LABELS], name="input_labels")

  in_training = tf.placeholder(tf.bool, shape=[], name="in_training")
  zeroone_y = (target_y+tf.ones_like(target_y))/2
  reluconst = 0

  # Formula Layer 1
  flayer_1_w = tf.get_variable("fully_connected/weights", shape=[NUM_FORMULA_FEATURES, 32], dtype=tf.float32, initializer=tf.contrib.layers.variance_scaling_initializer(1), trainable=True, constraint=C)
  flayer_1_b = tf.get_variable("fully_connected/biases", shape=[32], dtype=tf.float32, initializer=tf.constant_initializer(reluconst,tf.float32), trainable=True)
  flayer = activation(tf.matmul(formulas_x,flayer_1_w) + flayer_1_b, name="fully_connected/Relu")
  dropout_flayer = tf.contrib.layers.dropout(inputs=flayer, keep_prob=0.5, is_training=in_training)
  #dropout_flayer1 = tf.contrib.layers.dropout(inputs=flayer, keep_prob=dropout_rate, is_training=in_training)

  flayer_2_w = tf.get_variable("fully_connected_1/weights", shape=[32, 16], dtype=tf.float32, initializer=tf.contrib.layers.variance_scaling_initializer(1), trainable=True, constraint=C)
  flayer_2_b = tf.get_variable("fully_connected_1/biases", shape=[16], dtype=tf.float32, initializer=tf.constant_initializer(0,tf.float32), trainable=True)
  flayer2 = activation(tf.matmul(dropout_flayer,flayer_2_w) + flayer_2_b)
  dropout_flayer2 = tf.contrib.layers.dropout(inputs=flayer2, keep_prob=0.5, is_training=in_training)
  #dropout_flayer2 = tf.contrib.layers.dropout(inputs=flayer2, keep_prob=dropout_rate, is_training=in_training)

  # layer 1
  #layer1 = tf.contrib.layers.fully_connected(inputs=platts_x, num_outputs=3000, activation_fn=activation, trainable=True, biases_initializer=tf.constant_initializer(0,tf.float32))
  layer_1_w = tf.get_variable("fully_connected_2/weights", shape=[NUM_FEATURES, 3000], dtype=tf.float32, initializer=tf.contrib.layers.variance_scaling_initializer(1), trainable=True, constraint=C)
  layer_1_b = tf.get_variable("fully_connected_2/biases", shape=[3000], dtype=tf.float32, initializer=tf.constant_initializer(reluconst,tf.float32), trainable=True)
  layer1ausgabe = tf.matmul(platts_x,layer_1_w) + layer_1_b
  layer1 = activation(layer1ausgabe)
  dropout_layer1 = tf.contrib.layers.dropout(inputs=layer1, keep_prob=0.5, is_training=in_training)
  #dropout_layer1 = tf.contrib.layers.dropout(inputs=layer1, keep_prob=dropout_rate, is_training=in_training)
  
  # connection layer
  connection = tf.concat([dropout_flayer2, dropout_layer1], 1)
  #
  #layer2 = tf.contrib.layers.fully_connected(inputs=connection, num_outputs=3000, activation_fn=activation, trainable=True, biases_initializer=tf.constant_initializer(0,tf.float32))
  layer_2_w = tf.get_variable("fully_connected_3/weights", shape=[3000+16, 3000], dtype=tf.float32, initializer=tf.contrib.layers.variance_scaling_initializer(1), trainable=True, constraint=C)
  layer_2_b = tf.get_variable("fully_connected_3/biases", shape=[3000], dtype=tf.float32, initializer=tf.constant_initializer(reluconst,tf.float32), trainable=True)
  layer2 = activation(tf.matmul(connection,layer_2_w) + layer_2_b)
  dropout_layer2 = tf.contrib.layers.dropout(inputs=layer2, keep_prob=0.5, is_training=in_training)
  #dropout_layer2 = tf.contrib.layers.dropout(inputs=layer2, keep_prob=dropout_rate, is_training=in_training)

  #layer3 = tf.contrib.layers.fully_connected(inputs=dropout_layer2, num_outputs=3000, activation_fn=activation, trainable=True, biases_initializer=tf.constant_initializer(0,tf.float32))
  layer_3_w = tf.get_variable("fully_connected_4/weights", shape=[3000, 5000], dtype=tf.float32, initializer=tf.contrib.layers.variance_scaling_initializer(1), trainable=True, constraint=C)
  layer_3_b = tf.get_variable("fully_connected_4/biases", shape=[5000], dtype=tf.float32, initializer=tf.constant_initializer(reluconst,tf.float32), trainable=True)
  layer3ausgabe = tf.matmul(dropout_layer2,layer_3_w) + layer_3_b
  layer3 = activation(layer3ausgabe)
  dropout_layer3 = tf.contrib.layers.dropout(inputs=layer3, keep_prob=0.5, is_training=in_training)
  
  output_w = tf.get_variable("fully_connected_5/weights", shape=[5000, NUM_LABELS], dtype=tf.float32, initializer=tf.contrib.layers.variance_scaling_initializer(1), trainable=True, constraint=C)
  output_b = tf.get_variable("fully_connected_5/biases", shape=[NUM_LABELS], dtype=tf.float32, initializer=tf.constant_initializer(0,tf.float32), trainable=True) 
  output_layer = tf.add(tf.matmul(dropout_layer3,output_w), output_b, name="final_output")

  weight_layers = [flayer_1_w,flayer_2_w,layer_1_w,layer_2_w,layer_3_w,output_w]

  matrix_norms_l2 = tf.stack([ tf.nn.l2_loss(l) for l in weight_layers], name="matrix_norms")
  matrix_norms_l1 = tf.stack([ tf.reduce_sum(tf.abs(l)) for l in weight_layers], name="matrix_norms_l1")

  l2_regularizer = tf.reduce_sum(matrix_norms_l2)*l2inner
  l1_regularizer = tf.reduce_sum(matrix_norms_l1)*l1inner
  regularization = tf.add(l2_regularizer, l1_regularizer, name="regularization")
  #
  # loss function
  loss = loss_function(zeroone_y, output_layer) #tf.losses.hinge_loss(zeroone_y, output_layer)
  #
  # regularized_loss = tf.constant(0,dtype=tf.float32)
  out_loss = loss + regularization
  # monitor accuracy
  acc = tf.contrib.metrics.accuracy(output_layer>=0, target_y>=0)
  # monitor recall, precision and f1
  prediction = tf.greater_equal(output_layer, 0)
  target = tf.greater_equal(target_y, 0)
  tp = tf.reduce_sum(tf.cast(tf.logical_and(prediction, target), tf.float32), 0) + 0.1
  fp = tf.reduce_sum(tf.cast(tf.logical_and(prediction, tf.logical_not(target)), tf.float32), 0) + 0.1
  tn = tf.reduce_sum(tf.cast(tf.logical_and(tf.logical_not(prediction), tf.logical_not(target)), tf.float32), 0) + 0.1
  fn = tf.reduce_sum(tf.cast(tf.logical_and(tf.logical_not(prediction), target), tf.float32), 0) + 0.1
  recall = tp/(tp+fn)
  precision = tp/(tp+fp)
  f1 = (2*recall*precision)/(recall+precision)
  acc_tot = (tp+tn)/(tp+tn+fn+fp)
  
  atLeastFive = tf.greater_equal(tf.reduce_sum(tf.cast(target, dtype=tf.int8), 0), 10)
  numberOfFive = tf.reduce_sum(tf.cast(atLeastFive, dtype=tf.float32))
  onlyZero = tf.zeros(NUM_LABELS,  dtype=tf.float32)

  mean_recall = tf.reduce_mean(tf.where(atLeastFive, recall, onlyZero), name="eval_recall")
  mean_precision = tf.reduce_mean(tf.where(atLeastFive, precision, onlyZero), name="eval_precision")
  mean_f1 = tf.reduce_mean(tf.where(atLeastFive, f1, onlyZero), name="eval_f1")
  top90 = tf.reduce_sum(tf.cast(tf.greater_equal(f1, 0.9), tf.float32))
  top75 = tf.reduce_sum(tf.cast(tf.greater_equal(f1, 0.75), tf.float32))
  top50 = tf.reduce_sum(tf.cast(tf.greater_equal(f1, 0.5), tf.float32))
  #
  # training
  #tf.summary.scalar(loss.op.name, loss)
  #initial_optim = tf.train.GradientDescentOptimizer(learning_rate=0.2)
  optimizer =tf.train.AdamOptimizer(learning_rate=LEARN_RATE)
  #
  global_step = tf.Variable(0, name='global_step', trainable=False)
  train_op = optimizer.minimize(loss, global_step=global_step)

  # for export to java
  tf.add_to_collection('train_op', train_op)
  tf.add_to_collection('loss', loss)
  tf.add_to_collection('output_layer', output_layer)
  tf.add_to_collection('eval_top50', top50)
  tf.add_to_collection('eval_top75', top75)
  tf.add_to_collection('eval_top90', top90)
  tf.add_to_collection('eval_mean_f1', mean_f1)
  tf.add_to_collection('eval_mean_precision', mean_precision)
  tf.add_to_collection('eval_mean_recall', mean_recall)
  tf.add_to_collection('input_platts', platts_x)
  tf.add_to_collection('input_formulas', formulas_x)
  tf.add_to_collection('input_labels', target_y)

  # we need dummy variable, because tensorflow java does not support string variables
  dummy = tf.constant("saved_model", name="dummy_saved_model_name")

  save_model_id = tf.placeholder(tf.int32,  shape=[], name="model_id")
  better_dummy = tf.string_join([dummy, tf.string_join([tf.as_string(save_model_id), tf.constant("nn")], separator="/")], separator="_", name="save_model_with_id")
  
  saver = tf.train.Saver(tf.trainable_variables(), keep_checkpoint_every_n_hours=8, write_version=2)
  
  #saver = tf.train.Saver(tf.trainable_variables(), keep_checkpoint_every_n_hours=8, write_version=2)

  #
  #################################################
  sess.run(tf.global_variables_initializer())
  sess.run(tf.local_variables_initializer())

  builder = tf.saved_model.builder.SavedModelBuilder("dropout_model")
  builder.add_meta_graph_and_variables(sess,[])
  builder.save()

